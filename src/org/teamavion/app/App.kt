@file:Suppress("USELESS_ELVIS")

package org.teamavion.app

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ProgressIndicator
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * File where credentials are stored
 */
val FILE_REMEMBER = File(getDocumentsFolder(), "TADA-login.txt")

/**
 * Whether or not print output. Note that passing the parameter "-v" or "-verbose" to the program will override this
 */
val FLAG_VERBOSE = true


// Debug flags

val FLAG_DEBUG_SIMULATE_LOGIN = true
val FLAG_DEBUG_SIMULATE_LOGIN_OUTCOME = true

val VALUE_MAIN_CONTENT: String? = "#Login-main-pane"

var VERSION_STRING = VersionTypes.Alpha
var VERSION_MAJOR = "1"
var VERSION_MINOR = "0"
var VERSION_PATCH = "0"


/**
 * A registry listing available languages/language files
 */
val registry = LanguageRegistry(AssetType.Language.path(), "lang")

/**
 * Internationalization handler. This is what translates all the stuff
 */
var language: I18n = if(registry.default!=null) I18n(AssetType.Language.path(), registry.default, "login") else I18n.getStub()


@Suppress("UNCHECKED_CAST")
fun main(args: Array<String>){
    Thread.currentThread().setUncaughtExceptionHandler(::crashReport)
    bootstrapStreams(FLAG_VERBOSE || args.contains("-v") || args.contains("-verbose")) // Deutschland über alles: https://www.youtube.com/watch?v=yLHGTJjHXLo

    javafx.application.Application.launch(Application::class.java, *args)
}

class Application: javafx.application.Application() {
    override fun start(primaryStage: Stage?) {
        if(primaryStage==null) return
        val parent = FXMLLoader.load<Parent>(AssetType.Layout.loadURLAsset("login"))
        primaryStage.initStyle(StageStyle.DECORATED)
        primaryStage.title = language.getString("login.window.title")
        primaryStage.scene = Scene(parent)
        primaryStage.isResizable = false
        primaryStage.show()

        parent.applyLanguage(language)

        parent.setText("#Version", "Version: $VERSION_STRING-$VERSION_MAJOR.$VERSION_MINOR.$VERSION_PATCH")

        parent.lookup("#Login-Button").onMouseClicked = EventHandler {
            // Check for invalid inputs
            if(areAnyEmpty(*parent.lookupAllText("#Login-Username", "#Login-Password"))){
                val alert = Alert(AlertType.ERROR)
                alert.title = language.getString("login.error.title")
                alert.headerText = language.getString("login.error.header")
                alert.contentText = language.getString("login.error.content")
                alert.showAndWait()
            }else {
                // Hide stuff
                val all = parent.lookup("#Login-pane") as Parent hideAllExcept "Login-PI"
                parent.lookup("#Login-logo").isVisible = true

                // Attempt login
                login(parent lookupText "#Login-Username", parent lookupText "#Login-Password", parent lookupBoolean "#Login-RM", {
                    b, u ->
                    println("[Login] Authentication with username \"$u\"" + (if (b) " succeeded" else " failed"))

                    // Show elements
                    all.forEach { it.isVisible = true }
                    parent.lookup("#Login-PI").isVisible = false

                    if(b) loadLayoutInto(primaryStage, VALUE_MAIN_CONTENT, "root", language)
                }, { progress -> (parent.lookup("#Login-PI") as ProgressIndicator).progress = progress })
            }
        }
        parent.lookup("#Login-CWL-Button").onMouseClicked = EventHandler {
            println("[Action] Login CWL clicked")
            loadLayoutInto(primaryStage, VALUE_MAIN_CONTENT, "root", language)
        }


        val remember = getRemembered()
        if(remember!=null){
            println("[Credentials] Found remembered credentials: "+remember.first+" : "+remember.second)
            parent.setText("#Login-Username", remember.first)
            parent.setText("#Login-Password", remember.second)
            parent.setBoolean("#Login-RM", true)

            // TODO: Auto-login here
        }
    }
}

/**
 * Log in to Minecraft account
 *
 * @param username Username to log in with
 * @param password Password to log in with
 * @return Whether or not login was successful
 */
fun login(username: String, password: String, remember: Boolean, onLoginAttemptFinish: ((Boolean, String) -> Unit)?, progress: ((Double) -> Unit)?) {
    val t = Thread({
        if(FLAG_DEBUG_SIMULATE_LOGIN) {
            var i: Double = 0.0
            while (i < 1) {
                i += 0.1
                Thread.sleep(100) // Simulate interaction with internet
                Platform.runLater { progress?.invoke(i) }
            }
            Platform.runLater {
                onLoginAttemptFinish?.invoke(FLAG_DEBUG_SIMULATE_LOGIN_OUTCOME, username)
                if(FLAG_DEBUG_SIMULATE_LOGIN_OUTCOME && remember) writeUserScheme(null, username, password)
                else if(!remember) FILE_REMEMBER.delete()
            }
        }else{
            // TODO: Login

        }
    })
    // No holdups when user tries to kill program
    t.isDaemon = true
    t.start()
}

fun writeUserScheme(file: File?, username: String, password: String){
    val writeTo = FILE_REMEMBER or file
    if(writeTo.isFile) assert(writeTo.delete())
    assert(writeTo.createNewFile())
    val output = FileOutputStream(writeTo)
    output.write((username+"\r\n"+password).toByteArray())
    output.close()
}

fun getRemembered() = getRemembered(null)
fun getRemembered(file: File?): Pair<String, String>? {
    if(FILE_REMEMBER.isFile){
        val v = FileInputStream(FILE_REMEMBER or file)
        val allBytes = ByteArray(v.available())
        var idx = -1
        while(v.available()>0) allBytes[++idx] = v.read().toByte()
        val text = String(allBytes)
        if(!text.contains("\r\n")){
            println("[Credentials] Remember me file ("+(FILE_REMEMBER or file).absolutePath+") contains invalid username/password scheme")
            assert((FILE_REMEMBER or file).delete())
            return null
        }
        val split = text.split("\r\n")
        return Pair(split[0], split[1])
    }
    return null
}

fun bootstrapStreams(verbose: Boolean) {
    System.setOut(ConditionalStream(verbose, System.out))
    System.setErr(ConditionalStream(verbose, System.err))
}

class TextNodeDuckable(n: Node, var fieldName: String) {
    private val duckField: Field?
    private val duckMethods: Pair<Method?, Method?>
    private val obj: Node

    companion object { val rT = CharSequence::class.java; val v: Class<Void> = Void.TYPE }

    init {
        val f = n::class.java.lookupDeclaredField(true, fieldName)
        fieldName = if(fieldName.isNotEmpty()) Character.toUpperCase(fieldName[0])+fieldName.substring(1, fieldName.length) else fieldName
        val m = Pair(n::class.java.lookupDeclaredMethod(true, "get"+fieldName, rT), n::class.java.lookupDeclaredMethod(true, "set"+fieldName, v, rT))
        if((f==null || (f.type !is CharSequence)) && (m.first==null || m.second==null)) throw RuntimeException("Node of type "+n::class.qualifiedName+" is not duckable")
        duckField = if(f!=null && (f.type is CharSequence)) f else null
        duckMethods = m
        obj = n
    }
    constructor(n: Node): this(n, "text")

    fun setText(text: CharSequence) {
        if(duckField!=null) duckField.set(obj, text)
        else duckMethods.second?.invoke(obj, text)
    }
    fun getText() = (if(duckField!=null) duckField.get(obj) else duckMethods.first?.invoke(obj)) as CharSequence

    fun applyLanguage(language: I18n) = setText(language.getString(getText()) ?: getText())
}
enum class VersionTypes{ Alpha, Beta, Release }
enum class AssetType(val assetName: String, val suffix: String){
    Language("lang", ".lang"), CSS("style", ".css"), Image("images", ".jpg"), Logos("images/logos", ".png"), Layout("", ".fxml");
    override fun toString(): String = "AssetType."+name+"{ "+path()+"*"+suffix+" }"
    fun path() = "assets/"+assetName + if(assetName.isEmpty()) "" else "/"
    fun loadFileAsset(name: String) = File(loadURLAsset(name).toURI())
    fun loadURLAsset(name: String) = loadResource("assets/"+(assetName + if(assetName.isEmpty()) "" else "/")+name+suffix)
}
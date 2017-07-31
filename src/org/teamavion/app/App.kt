package org.teamavion.app

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

@Suppress("UNCHECKED_CAST")
fun main(args: Array<String>){ javafx.application.Application.launch(Application::class.java as Class<javafx.application.Application>, *args) }

class Application: javafx.application.Application() {
    override fun start(primaryStage: Stage?) {
        if(primaryStage==null) return
        val parent = FXMLLoader.load<Parent>(javaClass.getResource("../assets/login.fxml"))
        primaryStage.initStyle(StageStyle.DECORATED)
        primaryStage.title = "Team Avion Desktop Application"
        primaryStage.scene = Scene(parent)
        primaryStage.isResizable = false
        primaryStage.show()

        parent.lookup("#Login-Button").onMouseClicked = EventHandler {
            // Hide elements

            val all = parent.lookup("#Login-pane") as Parent hideAllExcept "Login-PI"
            parent.lookup("#Login-logo").isVisible = true



            // Attempt login
            if(areAnyEmpty(*parent.lookupAllText("#Login-Username", "#Login-Password"))){
                val v = Dialog<String>()
                v.contentText = "Please enter both a username and password!"
                v.showAndWait()
            }else
                login(parent lookupText "#Login-Username", parent lookupText "#Login-Password", parent lookupBoolean "#Login-RM", {
                    b, u ->
                    println(u+(if(b) " succeeded" else  " failed"))

                    // Show elements
                    all.forEach { it.isVisible = true }
                    parent.lookup("#Login-PI").isVisible = false
                }, { progress -> (parent.lookup("#Login-PI") as ProgressIndicator).progress = progress })
        }
        parent.lookup("#Login-CWL-Button").onMouseClicked = EventHandler {
            println("Login CWL clicked")
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
    // TODO: Login

    val t = Thread({
        var i: Double = 0.0
        while(i<1){
            i += 0.1
            Thread.sleep(100) // Simulate interaction with internet
            Platform.runLater { progress?.invoke(i) }
        }
        Platform.runLater { onLoginAttemptFinish?.invoke(false, username) }
    })
    t.start()

    if(remember){
        // TODO: Remember
        println(getDocuments())
        val f = File(getDocuments(), "dank.txt")
        f.createNewFile()
        val o = FileOutputStream(f)
        o.write("Hello World".toByteArray())
        o.close()
    }
}

fun areAnyEmpty(vararg args: String): Boolean {
    args.forEach { if(it.isEmpty()) return true }
    return false
}

fun String.isEmpty(): Boolean = equals("")
fun Parent.lookupAllText(vararg args: String): Array<String> {
    val v = Array<String>(args.size, { idx ->
        val s = lookup(args[idx])
        if(s is TextInputControl)
            s.text
        else if(s is Labeled)
            s.text
        else ""
    })
    return v
}
infix fun Parent.lookupText(id: String): String = (lookup(id) as TextField).text
infix fun Parent.lookupBoolean(id: String): Boolean = (lookup(id) as CheckBox).isSelected
infix fun Parent.hideAllExcept(id: String) = childrenUnmodifiable.filter { it.isVisible = it.id == id; !it.isVisible }

fun getDocuments(): String?{
    try {
        val p =  Runtime.getRuntime().exec("reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders\" /v personal")
        p.waitFor()

        val i = p.inputStream
        val b = ByteArray(i.available())
        i.read(b)
        i.close()
        val matcher = Pattern.compile(".*?REG_SZ\\s+(.*)").matcher(String(b))
        if(matcher.find()) return matcher.group(1)
        return null
    } catch(t: Throwable) { return null }
}
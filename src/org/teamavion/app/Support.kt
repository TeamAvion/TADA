@file:Suppress("UNCHECKED_CAST")

package org.teamavion.app

import javafx.collections.ObservableList
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.CheckBox
import javafx.scene.control.Labeled
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.stage.Stage
import org.teamavion.app.Magic.getBlankInstance
import sun.misc.Unsafe
import java.io.*
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.net.URL
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap

/*
NOTE: This KT file is just a support file with a bunch of convenience stuff. It's messy, sorry.
 */




// How to get an instance of Void (or Nothing)
val nothing: Magic.Nothing<Nothing> = getBlankInstance(Nothing::class.java)
val unsafe: Unsafe by lazy {
    val u = Unsafe::class.java.getDeclaredField("theUnsafe")
    u.isAccessible = true
    u.get(null) as Unsafe
}
val AOOverride: Long = unsafe.objectFieldOffset(AccessibleObject::class.java.getDeclaredField("override"))


fun getNothing(): Any? = nothing.instance

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
fun String.indexOfNoPrefix(startIndex: Int, find: String, prefix: String): Int{
    var i = startIndex-1
    while(++i<length) if(substring(i, find.length)==find && (i<prefix.length || substring(i-prefix.length, i)!=prefix)) return i
    return -1
}
fun String.indexOfNoPrefix(find: String, prefix: String) = indexOfNoPrefix(0, find, prefix)
fun String.replaceNoPrefix(find: String, prefix: String, replaceWith: String): String{
    val cArray = toCharArray()
    var build = ""
    val searchPrefix = prefix+find
    var i = -1
    while(++i<length) {
        if(i+find.length<=length && substring(i, i+find.length)==find){
            build += replaceWith
            i += find.length
        }else if(i+searchPrefix.length<=length && substring(i, i+searchPrefix.length)==searchPrefix){
            build+=find
            i+=find.length
        }else build += cArray[i]
    }
    return build
}
// Because why not?
fun Parent.getChildrenModifiable(): ObservableList<Node> =
        safeClassLookup("javafx.collections.FXCollections\$UnmodifiableObservableListImpl")
                ?.getDeclaredField(true, "backingList")
                ?.get(Parent::class.java.getDeclaredField(true, "unmodifiableChildren").get(this))
                as ObservableList<Node>

fun Parent.setBoolean(id: String, checked: Boolean) { (lookup(id) as CheckBox).isSelected = checked }
fun Parent.setText(id: String, text: String) {
    val v = lookup(id)
    if(v is TextInputControl) v.text = text
    else if(v is Labeled) v.text = text
}
fun loadLayoutInto(contentReceiver: Parent, assetName: String, language: I18n?){
    val children = contentReceiver.getChildrenModifiable()
    children.clear()
    children.add(FXMLLoader.load(AssetType.Layout.loadURLAsset("root")))
    if(language!=null) contentReceiver.applyLanguage(language)
}
fun loadLayoutInto(contentReceiver: Parent, assetName: String) = loadLayoutInto(contentReceiver, assetName, null)
fun loadLayoutInto(stage: Stage, contentReceiver: String?, assetName: String, language: I18n?) =
        loadLayoutInto(if(stage.scene.root.lookup(contentReceiver) is Parent) stage.scene.root.lookup(contentReceiver) as Parent else stage.scene.root, assetName, language)
fun loadLayoutInto(stage: Stage, contentReceiver: String?, assetName: String) = loadLayoutInto(stage, contentReceiver, assetName, null)
fun Method.matchesSignature(ret: Class<*>, vararg params: Class<*>) = ret.isAssignableFrom(returnType) and fuzzyEquals(params as Array<Class<*>>, *parameterTypes)
fun fuzzyEquals(types: Array<Class<*>>, vararg matchTo: Class<*>): Boolean {
    if(types.size!=matchTo.size) return false
    var matches = true
    types.forEachIndexed {
        idx, it ->
        matches = matches and it.isAssignableFrom(matchTo[idx])
        if(!matches) return@forEachIndexed
    }
    return matches
}
fun Method.invoke(override: Boolean, on: Any?, vararg params: Any?): Any?{
    forceAccessible(override)
    return invoke(on, *params)
}
fun Field.get(override: Boolean, on: Any?): Any?{
    forceAccessible(override)
    return get(on)
}
fun AccessibleObject.forceAccessible(accessible: Boolean) = unsafe.putBoolean(this, AOOverride, accessible)
fun <T> Class<T>.getDeclaredField(override: Boolean, name: String): Field{
    val f = getDeclaredField(name)
    f.forceAccessible(override)
    return f
}
fun <T> Class<T>.lookupDeclaredField(override: Boolean, name: String): Field?{
    var found: Field? = null
    declaredFields.forEach {
        if(it.name==name){
            it.forceAccessible(true)
            found = it
            return@forEach
        }
    }
    if(found==null && this!=Object::class.java) found = superclass.lookupDeclaredField(override, name)
    return found
}
fun <T> Class<T>.lookupDeclaredMethod(override: Boolean, name: String, ret: Class<*>, vararg params: Class<*>): Method?{
    var found: Method? = null
    declaredMethods.forEach {
        if(it.name==name && ret.isAssignableFrom(it.returnType) && fuzzyEquals(params as Array<Class<*>>, *it.parameterTypes)){
            it.forceAccessible(true)
            found = it
            return@forEach
        }
    }
    if(found==null && this!=Object::class.java) found = superclass.lookupDeclaredMethod(override, name, ret, *params)
    return found
}
fun safeClassLookup(name: String): Class<*>? = try{ Class.forName(name) }catch(e: Exception){ null }
infix fun File.or(file: File?) = file ?: this
infix fun Parent.lookupText(id: String): String = (lookup(id) as TextField).text
infix fun Parent.lookupBoolean(id: String): Boolean = (lookup(id) as CheckBox).isSelected
infix fun Parent.hideAllExcept(id: String) = childrenUnmodifiable.filter { it.isVisible = it.id == id; !it.isVisible }
fun Parent.applyLanguage(language: I18n){
    childrenUnmodifiable.forEach {
        (it as? Parent)?.applyLanguage(language)
        try{ TextNodeDuckable(it).applyLanguage(language) }catch(e: Throwable){ /* Ignored */ }
        try{ TextNodeDuckable(it, "promptText").applyLanguage(language) }catch(e: Throwable){ /* Ignored */ }
    }
}
fun crashReport(message: String){
    val logName = "TADA-error-"+System.currentTimeMillis()+".txt"
    val errorOut = PrintStream(logName)
    errorOut.print("Team Avion Desktop App error dump\r\nSorry for the inconvenience :)\r\n\r\n$message")
    errorOut.close()
}
fun crashReport(include: String, error: Throwable){
    val writer = StringWriter()
    error.printStackTrace(PrintWriter(writer))
    crashReport((if(include.isEmpty()) "" else include+"\r\n")+writer.toString())
}
fun crashReport(error: Throwable) = crashReport("", error)
fun crashReport(thread: Thread, error: Throwable) = crashReport("Crash from thread: "+thread.name, error)
fun getDocumentsFolder(): String?{
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
class SavingStream{

}
class ConditionalStream(private val allowOutput: Boolean, out: OutputStream?) : PrintStream(out) {
    override fun print(s: String?): Unit = if(allowOutput) super.print(s) else nothing.instance
    override fun print(b: Boolean): Unit = if(allowOutput) super.print(b) else nothing.instance
    override fun print(c: Char): Unit = if(allowOutput) super.print(c) else nothing.instance
    override fun print(d: Double): Unit = if(allowOutput) super.print(d) else nothing.instance
    override fun print(f: Float): Unit = if(allowOutput) super.print(f) else nothing.instance
    override fun print(i: Int): Unit = if(allowOutput) super.print(i) else nothing.instance
    override fun print(l: Long): Unit = if(allowOutput) super.print(l) else nothing.instance
    override fun print(obj: Any?): Unit = if(allowOutput) super.print(obj) else nothing.instance
    override fun print(s: CharArray?): Unit = if(allowOutput) super.print(s) else nothing.instance
    override fun println(): Unit = if(allowOutput) super.println() else nothing.instance
    override fun println(x: Any?): Unit = if(allowOutput) super.println(x) else nothing.instance
    override fun println(x: Boolean): Unit = if(allowOutput) super.println(x) else nothing.instance
    override fun println(x: Char): Unit = if(allowOutput) super.println(x) else nothing.instance
    override fun println(x: CharArray?): Unit = if(allowOutput) super.println(x) else nothing.instance
    override fun println(x: Double): Unit = if(allowOutput) super.println(x) else nothing.instance
    override fun println(x: Float): Unit = if(allowOutput) super.println(x) else nothing.instance
    override fun println(x: Int): Unit = if(allowOutput) super.println(x) else nothing.instance
    override fun println(x: Long): Unit = if(allowOutput) super.println(x) else nothing.instance
    override fun println(x: String?): Unit = if(allowOutput) super.println(x) else nothing.instance
    override fun write(b: Int): Unit = if(allowOutput) super.write(b) else nothing.instance
    override fun write(b: ByteArray?): Unit = if(allowOutput) super.write(b) else nothing.instance
    override fun write(buf: ByteArray?, off: Int, len: Int): Unit = if(allowOutput) super.write(buf, off, len) else nothing.instance
    override fun format(format: String?, vararg args: Any?): PrintStream = if(allowOutput) super.format(format, *args) else this
    override fun format(l: Locale?, format: String?, vararg args: Any?): PrintStream = if(allowOutput) super.format(l, format, *args) else this
    override fun flush(): Unit = if(allowOutput) super.flush() else nothing.instance
}

fun <T> callDuck(on: Any?, clazz: Class<*>?, method: String, ret: Class<T>, params: Class<*>, vararg actualParams: Any?): T? =
        (on?.javaClass ?: clazz)?.lookupDeclaredMethod(true, method, ret, params)?.invoke(on, actualParams) as? T


open class I18n{
    companion object {
        val DEFAULT_LANGUAGE = "en_US"
        val DEFAULT_CACHE_SIZE = 512
        fun getStub() = I18n()
    }
    protected val langFile: File?
    protected val cached = HashMap<String, String>()
    protected var maxCacheSize: Int
    private constructor(){
        langFile = null
        maxCacheSize = 0
    }
    constructor(resource: File, loadAllPrefixes: String?, maxSize: Int){
        maxCacheSize = if(maxSize<0) DEFAULT_CACHE_SIZE else maxSize
        langFile = resource
        if(loadAllPrefixes!=null){
            val reader = BufferedReader(FileReader(langFile))
            reader.lines().forEachOrdered {
                val idx = it.indexOf('=')
                if(idx==-1 || it[0]=='#'){
                    if(it.isNotEmpty() && it[0]!='#') System.err.println("Found invalid line in "+langFile.absolutePath+": "+it)
                }
                else{
                    val id = it.substring(0, idx)
                    if(id.startsWith(loadAllPrefixes)) cached.put(id, it.substring(idx+1, it.length).replaceNoPrefix("\\n", "\\", "\n"))
                    if(cached.size==maxCacheSize) return@forEachOrdered
                }
            }
            reader.close()
        }
    }
    constructor(resource: File, loadAllPrefixes: String?): this(resource, loadAllPrefixes, DEFAULT_CACHE_SIZE)
    constructor(rootDir: String, language: String, loadAllPrefixes: String?, maxSize: Int): this(File(ClassLoader.getSystemClassLoader().getResource("$rootDir/$language.lang").toURI()), loadAllPrefixes, maxSize)
    constructor(rootDir: String, language: String, loadAllPrefixes: String?): this(rootDir, language, loadAllPrefixes, DEFAULT_CACHE_SIZE)
    constructor(rootDir: String, language: String, loadAllMappings: Boolean): this(rootDir, language, if(loadAllMappings) "" else null)
    constructor(rootDir: String, loadAllPrefixes: String): this(rootDir, DEFAULT_LANGUAGE, loadAllPrefixes)
    constructor(rootDir: String): this(rootDir, DEFAULT_LANGUAGE, false)
    fun getString(id: CharSequence): String?{
        if(langFile==null) return null
        val v = cached[id]
        if(v!=null) return v
        val reader = BufferedReader(FileReader(langFile))
        var ret: String? = null
        reader.lines().forEachOrdered {
            val idx = it.indexOf('=')
            if(idx==-1 || it[0]=='#'){
                if(it.isNotEmpty() && it[0]!='#') System.err.println("Found invalid line in "+langFile.absolutePath+": "+it)
            }
            else{
                val foundId = it.substring(0, idx)
                if(foundId==id){
                    ret = it.substring(idx+1, it.length).replaceNoPrefix("\\n", "\\", "\n")
                    if(cached.size>=maxCacheSize) cached.remove(cached.keys.first())
                    cached.put(id, ret as String)
                    return@forEachOrdered
                }
            }
        }
        reader.close()
        return ret
    }
    fun clearCache() = cached.clear()
    fun setMaxAllowedCacheSize(size: Int){
        if(size<0) return
        maxCacheSize = size
        while(cached.size>maxCacheSize) cached.remove(cached.keys.first())
    }
    fun getMaxAllowedCacheSize() = maxCacheSize
}
class LanguageRegistry(path: String, name: String){
    val registryFile = File(loadResource("$path/$name.reg").toURI())
    val default: String?
    val languageEntries: Array<String>
    init{
        val list = ArrayList<String>()
        val reader = BufferedReader(FileReader(registryFile))
        var def: String? = null
        reader.lines().forEach {
            if(!it.startsWith('#')){
                if(it.startsWith("Default: ")){
                    def = it.substring(9)
                    list.add(def as String)
                    println("[Language registry] Found default language entry: "+def)
                }else {
                    list.add(it)
                    println("[Language registry] Found language entry: "+it)
                }
            }
        }
        default = if(def==null && list.size>0) list[0] else if(def!=null) def else null
        languageEntries = list.toTypedArray()
    }
}
fun loadResource(name: String): URL = ClassLoader.getSystemClassLoader().getResource(name)
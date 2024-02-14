package top.yukonga.mediaControlBlur.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources.getSystem
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.os.Environment
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.Properties

object AppUtils {

    fun colorFilter(colorInt: Int) = BlendModeColorFilter(colorInt, BlendMode.SRC_IN)

    fun isDarkMode(context: Context): Boolean = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    val Int.dp: Int get() = (this.toFloat().dp).toInt()

    val Float.dp: Float get() = this / getSystem().displayMetrics.density

    fun getBooleanProp(name: String): Boolean {
        getProp(name).let { prop ->
            return if (prop.isEmpty()) false else prop.toBoolean()
        }
    }

    fun getProp(name: String): String {
        var prop = getPropByStream(name)
        if (prop.isEmpty()) prop = getPropByShell(name)
        return prop
    }

    private fun getPropByStream(key: String): String {
        return try {
            val prop = Properties()
            FileInputStream(File(Environment.getRootDirectory(), "build.prop")).use { prop.load(it) }
            prop.getProperty(key, "")
        } catch (_: Exception) {
            ""
        }
    }

    private fun getPropByShell(propName: String): String {
        return try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            BufferedReader(InputStreamReader(p.inputStream), 1024).use { it.readLine() ?: "" }
        } catch (ignore: IOException) {
            ""
        }
    }
}
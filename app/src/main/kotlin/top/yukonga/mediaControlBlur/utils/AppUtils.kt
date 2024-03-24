package top.yukonga.mediaControlBlur.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources.getSystem
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter

object AppUtils {
    fun colorFilter(colorInt: Int) = BlendModeColorFilter(colorInt, BlendMode.SRC_IN)

    fun isDarkMode(context: Context): Boolean = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    val Int.dp: Int get() = (this.toFloat().dp).toInt()

    val Float.dp: Float get() = this / getSystem().displayMetrics.density
}
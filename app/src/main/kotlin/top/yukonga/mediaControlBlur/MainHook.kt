package top.yukonga.mediaControlBlur

import android.app.AndroidAppHelper
import android.graphics.Color
import android.graphics.Paint
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.moduleRes
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import top.yukonga.mediaControlBlur.utils.AppUtils.GREY
import top.yukonga.mediaControlBlur.utils.AppUtils.colorFilterCompat
import top.yukonga.mediaControlBlur.utils.AppUtils.isDarkMode
import top.yukonga.mediaControlBlur.utils.blur.MiBlurUtils.setBlurRoundRect
import top.yukonga.mediaControlBlur.utils.blur.MiBlurUtils.setMiBackgroundBlendColors
import top.yukonga.mediaControlBlur.utils.blur.MiBlurUtils.setMiViewBlurMode
import top.yukonga.mediaControlBlur.utils.blur.MiBlurUtils.supportBackgroundBlur

private const val TAG = "MediaControlBlur"

class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelper.initZygote(startupParam)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        EzXHelper.initHandleLoadPackage(lpparam)
        EzXHelper.setLogTag(TAG)
        EzXHelper.setToastTag(TAG)
        when (lpparam.packageName) {
            "com.android.systemui" -> {
                try {
                    if (!supportBackgroundBlur()) return

                    val miuiMediaControlPanel = loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaControlPanel")
                    val playerTwoCircleView = loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.PlayerTwoCircleView")
                    val miuiExpandableNotificationRow = loadClassOrNull("com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow")
                    val notificationUtil = loadClassOrNull("com.android.systemui.statusbar.notification.NotificationUtil")
                    val zenModeView = loadClassOrNull("com.android.systemui.statusbar.notification.zen.ZenModeView")

                    miuiMediaControlPanel?.methodFinder()?.filterByName("bindPlayer")?.first()?.createHook {
                        after {
                            val context = AndroidAppHelper.currentApplication().applicationContext

                            val mMediaViewHolder = it.thisObject.objectHelper().getObjectOrNullUntilSuperclass("mMediaViewHolder") ?: return@after

                            val titleText = mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("titleText")
                            val artistText = mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("artistText")
                            val seamlessIcon = mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageView>("seamlessIcon")
                            val action0 = mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageButton>("action0")
                            val action1 = mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageButton>("action1")
                            val action2 = mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageButton>("action2")
                            val action3 = mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageButton>("action3")
                            val action4 = mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageButton>("action4")
                            val seekBar = mMediaViewHolder.objectHelper().getObjectOrNullAs<SeekBar>("seekBar")
                            val elapsedTimeView = mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("elapsedTimeView")
                            val totalTimeView = mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("totalTimeView")

                            if (!isDarkMode(context)) {
                                titleText?.setTextColor(Color.BLACK)
                                artistText?.setTextColor(GREY)
                                elapsedTimeView?.setTextColor(GREY)
                                totalTimeView?.setTextColor(GREY)
                                seamlessIcon?.setColorFilter(Color.BLACK)
                                action0?.setColorFilter(Color.BLACK)
                                action1?.setColorFilter(Color.BLACK)
                                action2?.setColorFilter(Color.BLACK)
                                action3?.setColorFilter(Color.BLACK)
                                action4?.setColorFilter(Color.BLACK)
                                seekBar?.progressDrawable?.colorFilter = colorFilterCompat(Color.BLACK)
                                seekBar?.thumb?.colorFilter = colorFilterCompat(Color.BLACK)
                            } else {
                                titleText?.setTextColor(Color.WHITE)
                                seamlessIcon?.setColorFilter(Color.WHITE)
                                action0?.setColorFilter(Color.WHITE)
                                action1?.setColorFilter(Color.WHITE)
                                action2?.setColorFilter(Color.WHITE)
                                action3?.setColorFilter(Color.WHITE)
                                action4?.setColorFilter(Color.WHITE)
                                seekBar?.progressDrawable?.colorFilter = colorFilterCompat(Color.WHITE)
                                seekBar?.thumb?.colorFilter = colorFilterCompat(Color.WHITE)
                            }

                        }
                    }

                    miuiExpandableNotificationRow?.methodFinder()?.filterByName("updateBlurBg")?.first()?.createHook {
                        after {
                            val context = AndroidAppHelper.currentApplication().applicationContext

                            val isBackgroundBlurOpened = XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context)
                            if (isBackgroundBlurOpened == false) return@after

                            val isHeadsUpState = it.thisObject.objectHelper().invokeMethodBestMatch("isHeadsUpState") as Boolean
                            val mExpandedParamsUpdating = it.thisObject.objectHelper().getObjectOrNullUntilSuperclassAs<Boolean>("mExpandedParamsUpdating")
                            val getViewState = it.thisObject.objectHelper().invokeMethodBestMatch("getViewState")
                            val animatingMiniWindowEnter = getViewState?.objectHelper()?.getObjectOrNullUntilSuperclassAs<Boolean>("animatingMiniWindowEnter")

                            if (mExpandedParamsUpdating == null || animatingMiniWindowEnter == null) return@after
                            val z = !(!mExpandedParamsUpdating && !animatingMiniWindowEnter)

                            val intArray = if (isHeadsUpState) {
                                if (isDarkMode(context)) {
                                    moduleRes.getIntArray(R.array.notification_element_blend_headsUp_colors_night)
                                } else {
                                    moduleRes.getIntArray(R.array.notification_element_blend_headsUp_colors_light)
                                }
                            } else {
                                if (z) {
                                    if (isDarkMode(context)) {
                                        moduleRes.getIntArray(R.array.notification_element_blend_keyguard_colors_night)
                                    } else {
                                        moduleRes.getIntArray(R.array.notification_element_blend_keyguard_colors_light)
                                    }
                                } else {
                                    if (isDarkMode(context)) {
                                        moduleRes.getIntArray(R.array.notification_element_blend_shade_colors_night)
                                    } else {
                                        moduleRes.getIntArray(R.array.notification_element_blend_shade_colors_light)
                                    }
                                }
                            }

                            val mBackgroundNormal = it.thisObject.objectHelper().getObjectOrNullUntilSuperclass("mBackgroundNormal")
                            if (mBackgroundNormal != null) {
                                XposedHelpers.callStaticMethod(notificationUtil, "applyElementViewBlend", context, mBackgroundNormal, intArray, z)
                            }
                        }

                        zenModeView?.methodFinder()?.filterByName("updateBackgroundBg")?.first()?.createHook {
                            after {

                                val mRealContent = it.thisObject.objectHelper().getObjectOrNull("mRealContent") ?: return@after

                                val context = AndroidAppHelper.currentApplication().applicationContext

                                val isBackgroundBlurOpened = XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context)
                                if (isBackgroundBlurOpened == false) return@after

                                val mController = it.thisObject.objectHelper().getObjectOrNull("mController")
                                val statusBarStateController = mController?.objectHelper()?.getObjectOrNull("statusBarStateController")
                                val mState = statusBarStateController?.objectHelper()?.getObjectOrNull("mState") as Int

                                val intArray = if (mState == 2) {
                                    if (isDarkMode(context)) {
                                        moduleRes.getIntArray(R.array.notification_element_blend_keyguard_colors_night)
                                    } else {
                                        moduleRes.getIntArray(R.array.notification_element_blend_keyguard_colors_light)
                                    }
                                } else {
                                    if (isDarkMode(context)) {
                                        moduleRes.getIntArray(R.array.notification_element_blend_shade_colors_night)
                                    } else {
                                        moduleRes.getIntArray(R.array.notification_element_blend_shade_colors_light)
                                    }
                                }
                                XposedHelpers.callStaticMethod(notificationUtil, "applyElementViewBlend", context, mRealContent, intArray, true)
                            }
                        }

                        playerTwoCircleView?.methodFinder()?.filterByName("onDraw")?.first()?.createHook {
                            after {

                                val context = AndroidAppHelper.currentApplication().applicationContext

                                val isBackgroundBlurOpened = XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context)
                                if (isBackgroundBlurOpened == false) return@after

                                it.thisObject.objectHelper().getObjectOrNullAs<Paint>("mPaint1")?.alpha = 0
                                it.thisObject.objectHelper().getObjectOrNullAs<Paint>("mPaint2")?.alpha = 0
                                it.thisObject.objectHelper().setObject("mRadius", 0f)
                                it.thisObject.objectHelper().setObject("c1x", 0f)
                                it.thisObject.objectHelper().setObject("c1y", 0f)
                                it.thisObject.objectHelper().setObject("c2x", 0f)
                                it.thisObject.objectHelper().setObject("c2y", 0f)

                                val mediaBg = it.thisObject as ImageView
                                miuiExpandableNotificationRow.methodFinder().filterByName("updateBlurBg").first().createHook {
                                    after { hookParam ->
                                        val isHeadsUpState = hookParam.thisObject.objectHelper().invokeMethodBestMatch("isHeadsUpState") as Boolean
                                        val mExpandedParamsUpdating =
                                            hookParam.thisObject.objectHelper().getObjectOrNullUntilSuperclassAs<Boolean>("mExpandedParamsUpdating")!!
                                        val getViewState = hookParam.thisObject.objectHelper().invokeMethodBestMatch("getViewState")
                                        val animatingMiniWindowEnter =
                                            getViewState?.objectHelper()?.getObjectOrNullUntilSuperclassAs<Boolean>("animatingMiniWindowEnter")!!
                                        val z2 = !(!mExpandedParamsUpdating && !animatingMiniWindowEnter)

                                        val intArray = if (isHeadsUpState) {
                                            if (isDarkMode(context)) moduleRes.getIntArray(R.array.notification_element_blend_headsUp_colors_night) else moduleRes.getIntArray(
                                                R.array.notification_element_blend_headsUp_colors_light
                                            )
                                        } else {
                                            if (z2) {
                                                if (isDarkMode(context)) moduleRes.getIntArray(R.array.notification_element_blend_keyguard_colors_night) else moduleRes.getIntArray(
                                                    R.array.notification_element_blend_keyguard_colors_light
                                                )
                                            } else {
                                                if (isDarkMode(context)) moduleRes.getIntArray(R.array.notification_element_blend_shade_colors_night) else moduleRes.getIntArray(
                                                    R.array.notification_element_blend_shade_colors_light
                                                )
                                            }
                                        }
                                        mediaBg.apply {
                                            setMiViewBlurMode(1)
                                            setBlurRoundRect(60)
                                            setMiBackgroundBlendColors(intArray, 1.0f)
                                        }
                                    }
                                }
                            }
                        }

                        playerTwoCircleView?.methodFinder()?.filterByName("setBackground")?.first()?.createHook {
                            after {
                                val context = AndroidAppHelper.currentApplication().applicationContext

                                val isBackgroundBlurOpened = XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context)
                                if (isBackgroundBlurOpened == false) return@after

                                val mediaBg = it.thisObject as ImageView
                                mediaBg.background = null
                                mediaBg.setImageDrawable(null)
                            }
                        }

                        playerTwoCircleView?.methodFinder()?.filterByName("setPaintColor")?.first()?.createHook {
                            after {
                                val context = AndroidAppHelper.currentApplication().applicationContext

                                val isBackgroundBlurOpened = XposedHelpers.callStaticMethod(notificationUtil, "isBackgroundBlurOpened", context)
                                if (isBackgroundBlurOpened == false) return@after

                                it.result = null
                            }
                        }

                    }

                } catch (t: Throwable) {
                    Log.ex(t)
                }
            }

            else -> return
        }
    }

}
package io.github.noir017.fleetherd.ui

import android.animation.ValueAnimator
import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.noir017.fleetherd.web.ColorUtils

/**
 * Manages Edge-to-Edge insets, smooth dynamic coloring of the status bar and navigation bar,
 * automatic light/dark system icon contrast, and IME (keyboard) avoidance.
 */
class SystemBarController(
    private val window: Window,
    private val rootContainer: View,
    private val statusBarScrim: View,
    private val navBarScrim: View,
    initialColor: Int
) {
    private val insetsController = WindowInsetsControllerCompat(window, window.decorView)
    private var currentStatusColor: Int = initialColor
    private var currentNavColor: Int = initialColor
    private var statusAnimator: ValueAnimator? = null
    private var navAnimator: ValueAnimator? = null

    fun attach() {
        applyColors(currentStatusColor, currentNavColor, lightStatusIcons = null, animate = false)

        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { _, windowInsets ->
            val statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())

            if (statusBarScrim.layoutParams.height != statusBars.top) {
                statusBarScrim.layoutParams = statusBarScrim.layoutParams.apply {
                    height = statusBars.top
                }
            }

            val navHeight = if (imeVisible) 0 else navBars.bottom
            if (navBarScrim.layoutParams.height != navHeight) {
                navBarScrim.layoutParams = navBarScrim.layoutParams.apply {
                    height = navHeight
                }
            }

            val bottomPad = if (imeVisible) ime.bottom else 0
            rootContainer.setPadding(
                maxOf(statusBars.left, navBars.left),
                0,
                maxOf(statusBars.right, navBars.right),
                bottomPad
            )

            windowInsets
        }
        ViewCompat.requestApplyInsets(rootContainer)
    }

    fun applyColors(
        statusColor: Int,
        navColor: Int = statusColor,
        lightStatusIcons: Boolean? = null,
        animate: Boolean = true
    ) {
        val resolvedLightStatus = lightStatusIcons ?: ColorUtils.isLightColor(statusColor)
        val resolvedLightNav = ColorUtils.isLightColor(navColor)

        insetsController.isAppearanceLightStatusBars = resolvedLightStatus
        insetsController.isAppearanceLightNavigationBars = resolvedLightNav

        if (animate && statusColor != currentStatusColor) {
            statusAnimator?.cancel()
            statusAnimator = ValueAnimator.ofArgb(currentStatusColor, statusColor).apply {
                duration = ANIM_MS
                addUpdateListener { anim ->
                    statusBarScrim.setBackgroundColor(anim.animatedValue as Int)
                }
                start()
            }
        } else {
            statusBarScrim.setBackgroundColor(statusColor)
        }
        currentStatusColor = statusColor

        if (animate && navColor != currentNavColor) {
            navAnimator?.cancel()
            navAnimator = ValueAnimator.ofArgb(currentNavColor, navColor).apply {
                duration = ANIM_MS
                addUpdateListener { anim ->
                    navBarScrim.setBackgroundColor(anim.animatedValue as Int)
                }
                start()
            }
        } else {
            navBarScrim.setBackgroundColor(navColor)
        }
        currentNavColor = navColor
    }

    companion object {
        private const val ANIM_MS = 160L
    }
}

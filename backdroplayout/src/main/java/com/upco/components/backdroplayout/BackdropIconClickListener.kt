package com.upco.components.backdroplayout

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.View
import android.view.animation.Interpolator
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.upco.components.R

class BackdropIconClickListener @JvmOverloads internal constructor(
    private val context: Context,
    private val toolbar: Toolbar,
    private val sheet: View,
    private val backdrop: View,
    private val revealHeight: Int = 0,
    private val openTitle: String = "",
    private val closeTitle: String = "",
    private val openIcon: Drawable? = null,
    private val closeIcon: Drawable? = null,
    private val interpolator: Interpolator? = null,
    private val updateHiddenIcon: (Boolean) -> Unit
) : View.OnClickListener, MenuItem.OnMenuItemClickListener {

    private val animatorSet = AnimatorSet()
    private val animDuration = 500L
    private val height: Int
    private var backdropShown = false

    init {
        val displayMetrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        height = displayMetrics.heightPixels
    }

    override fun onClick(view: View) {
        toggleBackdrop(view)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        toggleBackdrop(item)
        return true
    }

    private fun toggleBackdrop(view: View) {
        backdropShown = !backdropShown

        cancelAnimations()
        updateIcon(view)
        updateTitle()
        updateHiddenIcon(backdropShown)

        if (!backdropShown) {
            // Translate first, then change visibility
            translateSheet()
            Handler().postDelayed({
                toggleBackdropVisibility()
            }, animDuration)
        } else {
            toggleBackdropVisibility()
            translateSheet()
        }
    }

    private fun toggleBackdrop(menuItem: MenuItem) {
        backdropShown = !backdropShown

        cancelAnimations()
        updateIcon(menuItem)
        updateTitle()
        updateHiddenIcon(backdropShown)

        if (!backdropShown) {
            // Translate first, then change visibility
            translateSheet()
            Handler().postDelayed({
                toggleBackdropVisibility()
            }, 500)
        } else {
            toggleBackdropVisibility()
            translateSheet()
        }
    }

    private fun cancelAnimations() {
        animatorSet.removeAllListeners()
        animatorSet.end()
        animatorSet.cancel()
    }

    private fun updateTitle() {
        if (openTitle.isNotBlank() && closeTitle.isNotBlank()) {
            if (backdropShown) {
                toolbar.title = openTitle
            } else {
                toolbar.title = closeTitle
            }
        }
    }

    private fun updateIcon(view: View) {
        if (openIcon != null && closeIcon != null) {
            if (view !is ImageView) {
                throw IllegalArgumentException( "updateIcon() must be called on an ImageView")
            }
            if (backdropShown) {
                view.setImageDrawable(closeIcon)
            } else {
                view.setImageDrawable(openIcon)
            }

            // Play the animation, if drawable is AnimatedVectorDrawable
            if (view.drawable is AnimatedVectorDrawableCompat) {
                (view.drawable as AnimatedVectorDrawableCompat).start()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && view.drawable is AnimatedVectorDrawable) {
                (view.drawable as AnimatedVectorDrawable).start()
            }
        }
    }

    private fun updateIcon(menuItem: MenuItem) {
        if (openIcon != null && closeIcon != null) {
            if (backdropShown) {
                menuItem.icon = closeIcon
            } else {
                menuItem.icon = openIcon
            }

            // Play the animation, if drawable is AnimatedVectorDrawable
            if (menuItem.icon is AnimatedVectorDrawableCompat) {
                (menuItem.icon as AnimatedVectorDrawableCompat).start()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && menuItem.icon is AnimatedVectorDrawable) {
                (menuItem.icon as AnimatedVectorDrawable).start()
            }
        }
    }

    private fun toggleBackdropVisibility() {
        if (backdropShown) {
            backdrop.visibility = View.VISIBLE
        } else {
            backdrop.visibility = View.GONE
        }
    }

    private fun translateSheet() {
        val revealMinHeight
                = context.resources.getDimensionPixelSize(R.dimen.backdrop_reveal_min_height)

        val translateY = if (revealHeight < revealMinHeight) {
            height - revealMinHeight
        } else {
            height - revealHeight
        }

        val animator = ObjectAnimator.ofFloat(
            sheet,
            "translationY",
            (if (backdropShown) translateY else 0).toFloat()
        )
        animator.duration = animDuration
        if (interpolator != null) {
            animator.interpolator = interpolator
        }
        animatorSet.play(animator)
        animator.start()
    }
}
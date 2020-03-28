package com.upco.components.backdroplayout

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.getDrawableOrThrow
import androidx.core.content.res.getStringOrThrow
import com.upco.components.R

class BackdropLayout(private val ctx: Context, attrs: AttributeSet, defStyleAttr: Int)
    : FrameLayout(ctx, attrs, defStyleAttr) {

    private var bd1OpenTitle = ""
    private var bd1CloseTitle = ""
    private var bd1OpenIcon: Drawable
    private var bd1CloseIcon: Drawable
    private var bd1RevealHeight = 0
    private var bd1Backdrop: View? = null

    private var bd2OpenTitle = ""
    private var bd2CloseTitle = ""
    private var bd2OpenIcon: Drawable? = null
    private var bd2CloseIcon: Drawable? = null
    private var bd2RevealHeight = 0
    private var bd2Backdrop: View? = null

    private var sheet: View? = null

    private var toolbarId = -1
    private lateinit var toolbar: Toolbar
    private var menuItem: MenuItem? = null
    private val inflater = LayoutInflater.from(ctx)

    constructor(ctx: Context, attrs: AttributeSet) : this(ctx, attrs, 0)

    init {
        clipChildren = true
        clipToPadding = true
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)

        var styledAttrs = ctx.obtainStyledAttributes(attrs, IntArray(1) { R.attr.colorPrimary })
        val bgColor = styledAttrs.getColor(0, 0)
        setBackgroundColor(bgColor)

        styledAttrs = ctx.obtainStyledAttributes(attrs, R.styleable.BackdropLayout)
        try {
            var id: Int

            bd1OpenTitle = styledAttrs.getStringOrThrow(
                R.styleable.BackdropLayout_bd1_openTitle
            )

            bd1CloseTitle = styledAttrs.getStringOrThrow(
                R.styleable.BackdropLayout_bd1_closeTitle
            )

            bd1OpenIcon = styledAttrs.getDrawableOrThrow(
                R.styleable.BackdropLayout_bd1_openIcon
            )

            bd1CloseIcon = styledAttrs.getDrawableOrThrow(
                R.styleable.BackdropLayout_bd1_closeIcon
            )

            bd1RevealHeight = styledAttrs.getDimensionPixelSize(
                R.styleable.BackdropLayout_bd1_revealHeight,
                resources.getDimensionPixelSize(R.dimen.backdrop_reveal_default_height)
            )

            id = styledAttrs.getResourceId(R.styleable.BackdropLayout_sheet, -1)
            if (id != -1) sheet = inflateAndAddView(id)

            id = styledAttrs.getResourceId(R.styleable.BackdropLayout_bd1_backdrop, -1)
            if (id != -1) {
                bd1Backdrop = inflateAndAddView(id)
                bd1Backdrop?.visibility = View.GONE
            }

            if (styledAttrs.hasValue(R.styleable.BackdropLayout_bd2_openTitle)) {
                bd2OpenTitle = styledAttrs.getStringOrThrow(
                    R.styleable.BackdropLayout_bd2_openTitle
                )
            }

            if (styledAttrs.hasValue(R.styleable.BackdropLayout_bd2_closeTitle)) {
                bd2CloseTitle = styledAttrs.getStringOrThrow(
                    R.styleable.BackdropLayout_bd2_closeTitle
                )
            }

            if (styledAttrs.hasValue(R.styleable.BackdropLayout_bd2_openIcon)) {
                bd2OpenIcon = styledAttrs.getDrawableOrThrow(
                    R.styleable.BackdropLayout_bd2_openIcon
                )
            }

            if (styledAttrs.hasValue(R.styleable.BackdropLayout_bd2_closeIcon)) {
                bd2CloseIcon = styledAttrs.getDrawableOrThrow(
                    R.styleable.BackdropLayout_bd2_closeIcon
                )
            }

            if (styledAttrs.hasValue(R.styleable.BackdropLayout_bd2_revealHeight)) {
                bd2RevealHeight = styledAttrs.getDimensionPixelSize(
                    R.styleable.BackdropLayout_bd2_revealHeight,
                    resources.getDimensionPixelSize(R.dimen.backdrop_reveal_default_height)
                )
            }

            if (styledAttrs.hasValue(R.styleable.BackdropLayout_bd2_backdrop)) {
                id = styledAttrs.getResourceId(R.styleable.BackdropLayout_bd2_backdrop, -1)
                if (id != -1) {
                    bd2Backdrop = inflateAndAddView(id)
                    bd2Backdrop?.visibility = View.GONE
                }
            }

            // TODO: Try to find Toolbar as a child view
            toolbarId = styledAttrs.getResourceId(R.styleable.BackdropLayout_toolbar, -1)

        } finally {
            styledAttrs.recycle()
        }

        // Setup top corners background for API 23+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sheet?.background = context?.getDrawable(R.drawable.backdrop_sheet_background_shape)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setToolbar((parent as View).findViewById(toolbarId))
    }

    fun setToolbar(toolbar: Toolbar) {
        this.toolbar = toolbar
        this.toolbar.setNavigationOnClickListener(BackdropIconClickListener(
            ctx,
            toolbar,
            sheet!!,
            bd1Backdrop!!,
            bd1RevealHeight,
            bd1OpenTitle,
            bd1CloseTitle,
            bd1OpenIcon,
            bd1CloseIcon,
            AccelerateDecelerateInterpolator()
        ) { backdropShown ->
            menuItem?.isVisible = !backdropShown
        })
    }

    fun setMenuItem(menuItem: MenuItem) {
        if (bd2Backdrop != null) {
            this.menuItem = menuItem
            this.menuItem?.setOnMenuItemClickListener(BackdropIconClickListener(
                ctx,
                toolbar,
                sheet!!,
                bd2Backdrop!!,
                bd2RevealHeight,
                bd2OpenTitle,
                bd2CloseTitle,
                bd2OpenIcon,
                bd2CloseIcon,
                AccelerateDecelerateInterpolator()
            ) { backdropShown ->
                toolbar.navigationIcon = if (!backdropShown) bd1OpenIcon else null
            })
        } else {
            throw IllegalStateException(
                "setMenuItem() should only be called when a second Backdrop is present and setted up"
            )
        }
    }

    fun setLeftOpenIcon(icon: Drawable) { bd1OpenIcon = icon }

    fun setLeftCloseIcon(icon: Drawable) { bd1CloseIcon = icon }

    fun setRightOpenIcon(icon: Drawable) { bd2OpenIcon = icon }

    fun setRightCloseIcon(icon: Drawable) { bd2CloseIcon = icon }

    private fun inflateAndAddView(@LayoutRes id: Int): View {
        val view = inflater.inflate(id, this, false)
        addView(view)
        return view
    }
}
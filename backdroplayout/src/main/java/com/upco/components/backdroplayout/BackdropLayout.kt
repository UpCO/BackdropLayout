package com.upco.components.backdroplayout

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.getDrawableOrThrow
import androidx.core.content.res.getStringOrThrow
import androidx.core.view.children
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.upco.components.internal.LockableNestedScrollView
import com.upco.components.R
import com.upco.util.DimensionUtils

class BackdropLayout(
    private val ctx: Context,
    private val attrs: AttributeSet?,
    private val defStyleAttr: Int
) : FrameLayout(ctx, attrs, defStyleAttr) {

    private var bd1OpenTitle = ""
    private var bd1CloseTitle = ""
    private var bd1RevealHeight = 0.0f
    private lateinit var bd1OpenIcon: Drawable
    private lateinit var bd1CloseIcon: Drawable
    private lateinit var bd1Backdrop: View

    private var bd2OpenTitle = ""
    private var bd2CloseTitle = ""
    private var bd2RevealHeight = 0.0f
    private var bd2OpenIcon: Drawable? = null
    private var bd2CloseIcon: Drawable? = null
    private var bd2Backdrop: View? = null

    private lateinit var sheet: LinearLayout
    private lateinit var sheetScrolling: LockableNestedScrollView
    private lateinit var sheetContent: View
    private var sheetHeader: View? = null

    private var toolbarId = -1
    private lateinit var toolbar: Toolbar
    private var menuItem: MenuItem? = null
    private val inflater = LayoutInflater.from(ctx)

    private lateinit var bd1Listener: BackdropIconClickListener
    private var bd2Listener: BackdropIconClickListener? = null
    private var selectedListener: BackdropIconClickListener? = null

    private val animatorSet = AnimatorSet()
    private val interpolator = LinearInterpolator()
    private val animDuration = 500L

    constructor(ctx: Context, attrs: AttributeSet) : this(ctx, attrs, 0)
    constructor(ctx: Context) : this(ctx, null, 0)

    init {
        getAttributes()
        setupLayout()
        setupSheet()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val par = parent as ViewGroup
        if (toolbarId != -1) {
            setToolbar(par.findViewById(toolbarId))
        } else {
            // Try to find toolbar as a child of parent
            par.children.forEach { child ->
                if (child is Toolbar) {
                    toolbarId = child.id
                    setToolbar(child)
                }
            }
            // Try to find toolbar as a child of a child of parent
            par.children.forEach { child ->
                if (child is ViewGroup) {
                    child.children.forEach { childOfChild ->
                        if (childOfChild is Toolbar) {
                            toolbarId = childOfChild.id
                            setToolbar(childOfChild)
                        }
                    }
                }
            }
        }
    }

    fun setToolbar(toolbar: Toolbar) {
        this.toolbar = toolbar
        this.bd1Listener = BackdropIconClickListener(
            ctx,
            toolbar,
            sheet,
            bd1Backdrop,
            bd1RevealHeight,
            bd1OpenTitle,
            bd1CloseTitle,
            bd1OpenIcon,
            bd1CloseIcon,
            AccelerateDecelerateInterpolator()
        ) { backdropShown ->
            if (backdropShown) selectedListener = bd1Listener
            menuItem?.isVisible = !backdropShown
            lockSheet(backdropShown)
        }
        this.toolbar.setNavigationOnClickListener(bd1Listener)
    }

    fun setMenuItem(menuItem: MenuItem) {
        if (bd2Backdrop != null) {
            this.menuItem = menuItem
            this.bd2Listener = BackdropIconClickListener(
                ctx,
                toolbar,
                sheet,
                bd2Backdrop!!,
                bd2RevealHeight,
                bd2OpenTitle,
                bd2CloseTitle,
                bd2OpenIcon,
                bd2CloseIcon,
                AccelerateDecelerateInterpolator()
            ) { backdropShown ->
                if (backdropShown) selectedListener = bd2Listener
                toolbar.navigationIcon = if (!backdropShown) bd1OpenIcon else null
                lockSheet(backdropShown)
            }
            this.menuItem?.setOnMenuItemClickListener(bd2Listener)
        } else {
            throw IllegalStateException(
                "setMenuItem() should only be called when a second Backdrop is present and configured"
            )
        }
    }

    fun setLeftOpenIcon(icon: Drawable) { bd1OpenIcon = icon }

    fun setLeftCloseIcon(icon: Drawable) { bd1CloseIcon = icon }

    fun setRightOpenIcon(icon: Drawable) { bd2OpenIcon = icon }

    fun setRightCloseIcon(icon: Drawable) { bd2CloseIcon = icon }

    private fun getAttributes() {
        val styledAttrs = ctx.obtainStyledAttributes(attrs, R.styleable.BackdropLayout)
        try {
            var id: Int

            // Try to get background color and if not defined get ?attr/colorPrimary
            val color = styledAttrs.getColor(R.styleable.BackdropLayout_backgroundColor, -1)
            if (color == -1) {
                val a = ctx.obtainStyledAttributes(attrs, IntArray(1) { R.attr.colorPrimary })
                setBackgroundColor(a.getColor(0, Color.WHITE))
                a.recycle()
            } else {
                setBackgroundColor(color)
            }

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

            bd1RevealHeight = styledAttrs.getDimension(
                R.styleable.BackdropLayout_bd1_revealHeight,
                resources.getDimension(R.dimen.backdrop_reveal_default_height)
            )

            id = styledAttrs.getResourceId(R.styleable.BackdropLayout_bd1_backdrop, -1)
            if (id != -1) {
                bd1Backdrop = inflater.inflate(id, this, false)
                bd1Backdrop.visibility = View.GONE
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
                bd2RevealHeight = styledAttrs.getDimension(
                    R.styleable.BackdropLayout_bd2_revealHeight,
                    resources.getDimension(R.dimen.backdrop_reveal_default_height)
                )
            }

            if (styledAttrs.hasValue(R.styleable.BackdropLayout_bd2_backdrop)) {
                id = styledAttrs.getResourceId(R.styleable.BackdropLayout_bd2_backdrop, -1)
                if (id != -1) {
                    bd2Backdrop = inflater.inflate(id, this, false)
                    bd2Backdrop?.visibility = View.GONE
                }
            }

            id = styledAttrs.getResourceId(R.styleable.BackdropLayout_sheet, -1)
            if (id != -1) {
                sheetContent = inflater.inflate(id, this, false)
            }

            id = styledAttrs.getResourceId(R.styleable.BackdropLayout_subheader, -1)
            if (id != -1) {
                sheetHeader = inflater.inflate(id, this, false)
            }

            toolbarId = styledAttrs.getResourceId(R.styleable.BackdropLayout_toolbar, -1)

        } finally {
            styledAttrs.recycle()
        }
    }

    private fun setupLayout() {
        clipChildren = true
        clipToPadding = true
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        addView(bd1Backdrop)
        addView(bd2Backdrop)
    }

    private fun setupSheet() {
        // Setup sheet layout
        sheet = LinearLayout(ctx, attrs, defStyleAttr).apply {
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                topMargin = DimensionUtils.convertDpToPxInt(resources, 56.0f)
            }
            orientation = VERTICAL
            background = ResourcesCompat.getDrawable(
                resources, R.drawable.backdrop_sheet_background_shape, null
            )
            // Add elevation for Android 21+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = DimensionUtils.convertDpToPx(resources, 1.0f)
            }

            // Set listener on click events
            setOnClickListener {
                // On sheet click, closes the currently opened Backdrop
                selectedListener?.close()
            }

            // Add sheet header
            sheetHeader?.apply {
                layoutParams = LayoutParams(
                    MATCH_PARENT, DimensionUtils.convertDpToPxInt(resources, 48.0f)
                )
                addView(this)
            }

            // Setup lockable nsv layout
            sheetScrolling = LockableNestedScrollView(ctx,attrs,defStyleAttr).apply {
                layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                    topMargin = sheetHeader?.height ?: 0
                }

                val dummyView = FrameLayout(ctx, attrs, defStyleAttr).apply {
                    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    // Add sheet content
                    addView(sheetContent)
                }
                addView(dummyView)
            }
            addView(sheetScrolling)
        }
        addView(sheet)
    }

    private fun lockSheet(backdropShown: Boolean) {
        // Lock/unlock scrolling
        sheetScrolling.locked = backdropShown

        // Enable sheet click only when Backdrop is open
        sheet.isClickable = backdropShown

        // sheetContent alpha fade animation
        val animator = ObjectAnimator.ofFloat(
            sheetContent,
            "alpha",
            if (backdropShown) 0.5f else 1.0f
        )
        animator.duration = animDuration
        animator.interpolator = interpolator
        animatorSet.play(animator)
        animator.start()
    }
}
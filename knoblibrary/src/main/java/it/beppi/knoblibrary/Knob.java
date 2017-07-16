package it.beppi.knoblibrary;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.PopupMenu;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

import it.beppi.balloonpopuplibrary.BalloonPopup;

import static java.lang.Math.PI;

/**
 * Created by Beppi on 06/12/2016.
 */

public class Knob extends View {

    public static final int SWIPEDIRECTION_NONE = 0;
    public static final int SWIPEDIRECTION_VERTICAL = 1;
    public static final int SWIPEDIRECTION_HORIZONTAL = 2;
    public static final int SWIPEDIRECTION_HORIZONTALVERTICAL = 3;
    public static final int SWIPEDIRECTION_CIRCULAR = 4;

    public static final int ONCLICK_NONE = 0;
    public static final int ONCLICK_NEXT = 1;
    public static final int ONCLICK_PREV = 2;
    public static final int ONCLICK_RESET = 3;
    public static final int ONCLICK_MENU = 4;
    public static final int ONCLICK_USER = 5;

    public static final int BALLONANIMATION_POP = 0;
    public static final int BALLONANIMATION_SCALE = 1;
    public static final int BALLONANIMATION_FADE = 2;

    // constructors
    public Knob(Context context) {
        super(context);
        init(null);
    }

    public Knob(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public Knob(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Knob(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    // overrides

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        Resources r = Resources.getSystem();
        if(widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST){
            widthSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, r.getDisplayMetrics());
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
        }

        if(heightMode == MeasureSpec.UNSPECIFIED || heightSize == MeasureSpec.AT_MOST){
            heightSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, r.getDisplayMetrics());
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        final int width = getWidth();
        final int height = getHeight();

        externalRadius = Math.min(width, height) * 0.5f;
        knobRadius = externalRadius * knobRelativeRadius;
        centerX = width/2;
        centerY = height/2;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paintKnob(canvas);
        paintMarkers(canvas);
        paintIndicator(canvas);
        paintCircularIndicator(canvas);
        paintKnobCenter(canvas);
        paintKnobBorder(canvas);
        displayBalloons();
    }

    void paintKnob(Canvas canvas) {
        if (knobDrawableRes != 0 && knobDrawable != null) {
            knobDrawable.setBounds((int)(centerX-knobRadius), (int)(centerY-knobRadius), (int)(centerX+knobRadius), (int)(centerY+knobRadius));
            if (knobDrawableRotates) {
                canvas.save();
                canvas.rotate((float)-Math.toDegrees(PI + currentAngle), centerX, centerY);
                knobDrawable.draw(canvas);
                canvas.restore();
            }
            else
                knobDrawable.draw(canvas);
        } else {
            paint.setColor(knobColor);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(centerX, centerY, knobRadius, paint);
        }
    }

    void paintKnobBorder(Canvas canvas) {
        if (borderWidth == 0) return;
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);
        canvas.drawCircle(centerX, centerY, knobRadius, paint);
    }

    void paintKnobCenter(Canvas canvas) {
        if (knobDrawableRes != 0 && knobDrawable != null) return;
        if (knobCenterRelativeRadius == 0f) return;
        paint.setColor(knobCenterColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, knobCenterRelativeRadius * knobRadius, paint);
    }

    double normalizeAngle(double angle) {
        while (angle < 0) angle += PI*2;
        while (angle >= PI*2) angle -= PI*2;
        return angle;
    }

    double calcAngle(int position) {
        double min = Math.toRadians((double)minAngle);
        double max = Math.toRadians((double)maxAngle - 0.0001);
        double range = max - min;

        if (numberOfStates <= 1)
            return 0;

        double singleStepAngle = range / (numberOfStates-1);
        if (PI*2 - range < singleStepAngle)
            singleStepAngle = range / numberOfStates;
        return normalizeAngle(PI - min - position * singleStepAngle);

        // return Math.PI - position * (2 * Math.PI / numberOfStates);
    }

    void setIndicatorAngleWithDirection() {
        double angleCurr = normalizeAngle(spring.getCurrentValue());
        double angleNew = calcAngle(actualState);
        if (freeRotation) {
            if (angleCurr > angleNew && angleCurr - angleNew > PI) angleNew += PI * 2;
            else if (angleCurr < angleNew && angleNew - angleCurr > PI) angleNew -= PI * 2;
        }
        spring.setCurrentValue(angleCurr);
        spring.setEndValue(angleNew);
    }

    void paintIndicator(Canvas canvas) {
        if (indicatorWidth == 0) return;
        if (indicatorRelativeLength == 0.0f) return;
        paint.setColor(indicatorColor);
        paint.setStrokeWidth(indicatorWidth);

        float startX = centerX + (float)(knobRadius * (1-indicatorRelativeLength) * Math.sin(currentAngle));
        float startY = centerY + (float)(knobRadius * (1-indicatorRelativeLength) * Math.cos(currentAngle));
        float endX = centerX + (float)(knobRadius * Math.sin(currentAngle));
        float endY = centerY + (float)(knobRadius * Math.cos(currentAngle));
        canvas.drawLine(startX, startY, endX, endY, paint);
    }

    void paintCircularIndicator(Canvas canvas) {
        if (circularIndicatorRelativeRadius == 0.0f) return;
        paint.setColor(circularIndicatorColor);
        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.FILL);

        float posX = centerX + (float)(externalRadius * circularIndicatorRelativePosition * Math.sin(currentAngle));
        float posY = centerY + (float)(externalRadius * circularIndicatorRelativePosition * Math.cos(currentAngle));
        canvas.drawCircle(posX, posY, externalRadius * circularIndicatorRelativeRadius, paint);
    }

    void paintMarkers(Canvas canvas) {
        if ((stateMarkersRelativeLength == 0 || stateMarkersWidth == 0) && (stateMarkersAccentRelativeLength == 0 || stateMarkersAccentWidth == 0)) return;
        for (int w=0; w<numberOfStates; w++) {
            boolean big = false;
            boolean selected = false;
            if (stateMarkersAccentPeriodicity != 0)
                big = (w % stateMarkersAccentPeriodicity == 0);
            selected = (w == actualState || (w <= actualState && selectedStateMarkerContinuous));

            paint.setStrokeWidth(big ? stateMarkersAccentWidth : stateMarkersWidth);
            double angle = calcAngle(w);
            float startX = centerX + (float) (externalRadius * (1 - (big ? stateMarkersAccentRelativeLength : stateMarkersRelativeLength)) * Math.sin(angle));
            float startY = centerY + (float) (externalRadius * (1 - (big ? stateMarkersAccentRelativeLength : stateMarkersRelativeLength)) * Math.cos(angle));
            float endX = centerX + (float) (externalRadius * Math.sin(angle));
            float endY = centerY + (float) (externalRadius * Math.cos(angle));
            paint.setColor(selected ? selectedStateMarkerColor : (big ? stateMarkersAccentColor : stateMarkersColor));
            canvas.drawLine(startX, startY, endX, endY, paint);
        }
    }

    int balloonsX() {
        return (int)(centerX + (float)(externalRadius * balloonValuesRelativePosition * Math.sin(currentAngle)));
    }
    int balloonsY() {
        return (int)(centerY + (float)(externalRadius * balloonValuesRelativePosition * Math.cos(currentAngle)));
    }
    String balloonText() {
        if (balloonValuesArray == null)
            return Integer.toString(actualState);
        else
            return balloonValuesArray[actualState].toString();
    }

    void displayBalloons() {
        if (!showBalloonValues) return;
        if (balloonPopup == null || !balloonPopup.isShowing())
            balloonPopup = BalloonPopup.Builder(ctx, this)
                    .text(balloonText())
                    .gravity(BalloonPopup.BalloonGravity.halftop_halfleft)
                    .offsetX(balloonsX())
                    .offsetY(balloonsY())
                    .textSize((int)balloonValuesTextSize)
                    .shape(BalloonPopup.BalloonShape.rounded_square)
                    .timeToLive(balloonValuesTimeToLive)
                    .animation(getBalloonAnimation())
                    .stayWithinScreenBounds(true)
                    .show();
        else {
            balloonPopup.updateOffset(balloonsX(), balloonsY(), true);
            balloonPopup.updateText(balloonText(), true);
            balloonPopup.updateTextSize((int)balloonValuesTextSize, true);  // solo l'ultimo richiede l'aggiornamento del timer?
        }
    }
    BalloonPopup.BalloonAnimation getBalloonAnimation() {
        if (balloonValuesAnimation == 0 && balloonValuesSlightlyTransparent) return BalloonPopup.BalloonAnimation.fade75_and_pop;
        else if (balloonValuesAnimation == 0) return BalloonPopup.BalloonAnimation.fade_and_pop;
        else if (balloonValuesAnimation == 1 && balloonValuesSlightlyTransparent) return BalloonPopup.BalloonAnimation.fade75_and_scale;
        else if (balloonValuesAnimation == 1) return BalloonPopup.BalloonAnimation.fade_and_scale;
        else if (balloonValuesAnimation == 2 && balloonValuesSlightlyTransparent) return BalloonPopup.BalloonAnimation.fade75;
        else return BalloonPopup.BalloonAnimation.fade;
    }

    // default values
    private int numberOfStates = 6;
    private int defaultState = 0;
    private int borderWidth = 2;
    private int borderColor = Color.BLACK;
    private int indicatorWidth = 6;
    private int indicatorColor = Color.BLACK;
    private float indicatorRelativeLength = 0.35f;
    private float circularIndicatorRelativeRadius = 0.0f;
    private float circularIndicatorRelativePosition = 0.7f;
    private int circularIndicatorColor = Color.BLACK;
    private int knobColor = Color.LTGRAY;
    private float knobRelativeRadius = 0.8f;
    private float knobCenterRelativeRadius = 0.45f;
    private int knobCenterColor = Color.DKGRAY;
    private boolean enabled = true;
    private int currentState = defaultState; // can be negative and override expected limits
    private int actualState = currentState; // currentState, modded to the expected limits
    private boolean animation = true;
    private float animationSpeed = 10;
    private float animationBounciness = 40;
    private int stateMarkersWidth = 2;
    private int stateMarkersColor = Color.BLACK;
    private int selectedStateMarkerColor = Color.YELLOW;
    private boolean selectedStateMarkerContinuous = false;
    private float stateMarkersRelativeLength = 0.06f;
    private int swipeDirection = 4;   // circular  (before it was horizontal)
    private int swipeSensibilityPixels = 100;
    private int swipeX=0, swipeY=0;  // used for swipe management
    boolean swipeing = false;        // used for swipe / click management
    private boolean freeRotation = true;
    private float minAngle = 0f;
    private float maxAngle = 360f;
    private int stateMarkersAccentWidth = 3;
    private int stateMarkersAccentColor = Color.BLACK;
    private float stateMarkersAccentRelativeLength = 0.11f;
    private int stateMarkersAccentPeriodicity = 0;  // 0 = off
    private int knobDrawableRes = 0;
    private boolean knobDrawableRotates = true;
    private boolean showBalloonValues = false;
    private int balloonValuesTimeToLive = 400;
    private float balloonValuesRelativePosition = 1.3f;
    private float balloonValuesTextSize = 9;
    private int balloonValuesAnimation = BALLONANIMATION_POP;
    private CharSequence[] balloonValuesArray = null;
    private boolean balloonValuesSlightlyTransparent = true;
    private int clickBehaviour = ONCLICK_NEXT;   // next
    private Runnable userRunnable = null;


    // initialize

    void init(AttributeSet attrs) {
        ctx = getContext();
        loadAttributes(attrs);
        initTools();
        initDrawables();
        initBalloons();
        initListeners();
        initStatus();
    }

    private Paint paint;
    private Context ctx;
    private float externalRadius, knobRadius, centerX, centerY;
    SpringSystem springSystem;
    Spring spring;
    private double currentAngle;
    private int previousState = defaultState;
    private Drawable knobDrawable;
    private BalloonPopup balloonPopup;

    void initTools() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeCap(Paint.Cap.ROUND);

        springSystem = SpringSystem.create();
        spring = springSystem.createSpring();
        spring.setSpringConfig(SpringConfig.fromBouncinessAndSpeed((double)animationSpeed, (double)animationBounciness));
        spring.setOvershootClampingEnabled(false);
    }

    void initDrawables() {
        if (knobDrawableRes != 0) {
            knobDrawable = getResources().getDrawable(knobDrawableRes);
        }
    }

    void loadAttributes(AttributeSet attrs) {
        if (attrs == null) return;

        TypedArray typedArray = ctx.obtainStyledAttributes(attrs, R.styleable.Knob);

        numberOfStates = typedArray.getInt(R.styleable.Knob_kNumberOfStates, numberOfStates);
        defaultState = typedArray.getInt(R.styleable.Knob_kDefaultState, defaultState);

        borderWidth = typedArray.getDimensionPixelSize(R.styleable.Knob_kBorderWidth, borderWidth);
        borderColor = typedArray.getColor(R.styleable.Knob_kBorderColor, borderColor);

        indicatorWidth = typedArray.getDimensionPixelSize(R.styleable.Knob_kIndicatorWidth, indicatorWidth);
        indicatorColor = typedArray.getColor(R.styleable.Knob_kIndicatorColor, indicatorColor);
        indicatorRelativeLength = typedArray.getFloat(R.styleable.Knob_kIndicatorRelativeLength, indicatorRelativeLength);

        circularIndicatorRelativeRadius = typedArray.getFloat(R.styleable.Knob_kCircularIndicatorRelativeRadius, circularIndicatorRelativeRadius);
        circularIndicatorRelativePosition = typedArray.getFloat(R.styleable.Knob_kCircularIndicatorRelativePosition, circularIndicatorRelativePosition);
        circularIndicatorColor = typedArray.getColor(R.styleable.Knob_kCircularIndicatorColor, circularIndicatorColor);

        knobColor = typedArray.getColor(R.styleable.Knob_kKnobColor, knobColor);
        knobRelativeRadius = typedArray.getFloat(R.styleable.Knob_kKnobRelativeRadius, knobRelativeRadius);

        knobCenterRelativeRadius = typedArray.getFloat(R.styleable.Knob_kKnobCenterRelativeRadius, knobCenterRelativeRadius);
        knobCenterColor = typedArray.getColor(R.styleable.Knob_kKnobCenterColor, knobCenterColor);
        
        knobDrawableRes = typedArray.getResourceId(R.styleable.Knob_kKnobDrawable, knobDrawableRes);
        knobDrawableRotates = typedArray.getBoolean(R.styleable.Knob_kKnobDrawableRotates, knobDrawableRotates);

        stateMarkersWidth = typedArray.getDimensionPixelSize(R.styleable.Knob_kStateMarkersWidth, stateMarkersWidth);
        stateMarkersColor = typedArray.getColor(R.styleable.Knob_kStateMarkersColor, stateMarkersColor);
        selectedStateMarkerColor = typedArray.getColor(R.styleable.Knob_kSelectedStateMarkerColor, selectedStateMarkerColor);
        stateMarkersRelativeLength = typedArray.getFloat(R.styleable.Knob_kStateMarkersRelativeLength, stateMarkersRelativeLength);
        selectedStateMarkerContinuous = typedArray.getBoolean(R.styleable.Knob_kSelectedStateMarkerContinuous, selectedStateMarkerContinuous);

        animation = typedArray.getBoolean(R.styleable.Knob_kAnimation, animation);
        animationSpeed = typedArray.getFloat(R.styleable.Knob_kAnimationSpeed, animationSpeed);
        animationBounciness = typedArray.getFloat(R.styleable.Knob_kAnimationBounciness, animationBounciness);

        swipeDirection = swipeAttrToInt(typedArray.getString(R.styleable.Knob_kSwipe));
        swipeSensibilityPixels = typedArray.getInt(R.styleable.Knob_kSwipeSensitivityPixels, swipeSensibilityPixels);

        freeRotation = typedArray.getBoolean(R.styleable.Knob_kFreeRotation, freeRotation);
        minAngle = typedArray.getFloat(R.styleable.Knob_kMinAngle, minAngle);
        maxAngle = typedArray.getFloat(R.styleable.Knob_kMaxAngle, maxAngle);

        stateMarkersAccentWidth = typedArray.getDimensionPixelSize(R.styleable.Knob_kStateMarkersAccentWidth, stateMarkersAccentWidth);
        stateMarkersAccentColor = typedArray.getColor(R.styleable.Knob_kStateMarkersAccentColor, stateMarkersAccentColor);
        stateMarkersAccentRelativeLength = typedArray.getFloat(R.styleable.Knob_kStateMarkersAccentRelativeLength, stateMarkersAccentRelativeLength);
        stateMarkersAccentPeriodicity = typedArray.getInt(R.styleable.Knob_kStateMarkersAccentPeriodicity, stateMarkersAccentPeriodicity);

        showBalloonValues = typedArray.getBoolean(R.styleable.Knob_kShowBalloonValues, showBalloonValues);
        balloonValuesTimeToLive = typedArray.getInt(R.styleable.Knob_kBalloonValuesTimeToLive, balloonValuesTimeToLive);
        balloonValuesRelativePosition = typedArray.getFloat(R.styleable.Knob_kBalloonValuesRelativePosition, balloonValuesRelativePosition);
        balloonValuesTextSize = typedArray.getDimension(R.styleable.Knob_kBalloonValuesTextSize, balloonValuesTextSize);
        balloonValuesAnimation = balloonAnimationAttrToInt(typedArray.getString(R.styleable.Knob_kBalloonValuesAnimation));
        balloonValuesArray = typedArray.getTextArray(R.styleable.Knob_kBalloonValuesArray);
        balloonValuesSlightlyTransparent = typedArray.getBoolean(R.styleable.Knob_kBalloonValuesSlightlyTransparent, balloonValuesSlightlyTransparent);

        clickBehaviour = clickAttrToInt(typedArray.getString(R.styleable.Knob_kClickBehaviour));

        enabled = typedArray.getBoolean(R.styleable.Knob_kEnabled, enabled);

        typedArray.recycle();
    }
    int swipeAttrToInt(String s) {
        if (s == null) return SWIPEDIRECTION_CIRCULAR;
        if (s.equals("0")) return SWIPEDIRECTION_NONE;
        else if (s.equals("1")) return SWIPEDIRECTION_VERTICAL;  // vertical
        else if (s.equals("2")) return SWIPEDIRECTION_HORIZONTAL;  // horizontal
        else if (s.equals("3")) return SWIPEDIRECTION_HORIZONTALVERTICAL;  // both
        else if (s.equals("4")) return SWIPEDIRECTION_CIRCULAR;  // default  - circular
        else return SWIPEDIRECTION_CIRCULAR;
    }
    int clickAttrToInt(String s) {
        if (s == null) return ONCLICK_NEXT;
        if (s.equals("0")) return ONCLICK_NONE;
        else if (s.equals("1")) return ONCLICK_NEXT;  // default - next
        else if (s.equals("2")) return ONCLICK_PREV;  // prev
        else if (s.equals("3")) return ONCLICK_RESET;  // reset
        else if (s.equals("4")) return ONCLICK_MENU;  // menu
        else if (s.equals("5")) return ONCLICK_USER;  // menu
        else return ONCLICK_NEXT;
    }
    int balloonAnimationAttrToInt(String s) {
        if (s == null) return BALLONANIMATION_POP;
        if (s.equals("0")) return BALLONANIMATION_POP;       // pop
        else if (s.equals("1")) return BALLONANIMATION_SCALE;  // scale
        else if (s.equals("2")) return BALLONANIMATION_FADE;  // fade
        else return BALLONANIMATION_POP;
    }

    private void disallowParentToHandleTouchEvents() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    void clickMe(View view) {
        switch (clickBehaviour) {
            case ONCLICK_NONE: break;
            case ONCLICK_NEXT: toggle(animation); break;
            case ONCLICK_PREV: inverseToggle(animation); break;
            case ONCLICK_RESET: revertToDefault(animation); break;
            case ONCLICK_MENU: createPopupMenu(view); break;
            case ONCLICK_USER: runUserBehaviour(); break;
        }
    }

    void initListeners() {
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!enabled) return;
                clickMe(view);
            }
        });

        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (!enabled) return false;
                if (swipeDirection == SWIPEDIRECTION_NONE) { toggle(animation); return false; }
                int action = motionEvent.getAction();
                if (swipeDirection == SWIPEDIRECTION_VERTICAL) {  // vertical
                    int y = (int) motionEvent.getY();
                    if (action == MotionEvent.ACTION_DOWN) {
                        swipeY = y;
                        swipeing = false;
                        disallowParentToHandleTouchEvents(); // needed when Knob's parent is a ScrollView
                    }
                    else if (action == MotionEvent.ACTION_MOVE) {
                        if (y - swipeY > swipeSensibilityPixels) {
                            swipeY = y;
                            swipeing = true;
                            decreaseValue();
                            return true;
                        }
                        else if (swipeY - y > swipeSensibilityPixels) {
                            swipeY = y;
                            swipeing = true;
                            increaseValue();
                            return true;
                        }
                    }
                    else if (action == MotionEvent.ACTION_UP) {
                        if (!swipeing) clickMe(view);    // click
                        return true;
                    }
                    return false;
                }
                else if (swipeDirection == SWIPEDIRECTION_HORIZONTAL) {  // horizontal
                    int x = (int) motionEvent.getX();
                    if (action == MotionEvent.ACTION_DOWN) {
                        swipeX = x;
                        swipeing = false;
                        disallowParentToHandleTouchEvents(); // needed when Knob's parent is a ScrollView
                    }
                    else if (action == MotionEvent.ACTION_MOVE) {
                        if (x - swipeX > swipeSensibilityPixels) {
                            swipeX = x;
                            swipeing = true;
                            increaseValue();
                            return true;
                        }
                        else if (swipeX - x > swipeSensibilityPixels) {
                            swipeX = x;
                            swipeing = true;
                            decreaseValue();
                            return true;
                        }
                    }
                    else if (action == MotionEvent.ACTION_UP) {
                        if (!swipeing) clickMe(view);    // click
                        return true;
                    }
                    return false;
                }
                else if (swipeDirection == SWIPEDIRECTION_HORIZONTALVERTICAL) {  // both
                    int x = (int) motionEvent.getX();
                    int y = (int) motionEvent.getY();
                    if (action == MotionEvent.ACTION_DOWN) {
                        swipeX = x;
                        swipeY = y;
                        swipeing = false;
                        disallowParentToHandleTouchEvents(); // needed when Knob's parent is a ScrollView
                    }
                    else if (action == MotionEvent.ACTION_MOVE) {
                        if (x - swipeX > swipeSensibilityPixels || swipeY - y > swipeSensibilityPixels ) {
                            swipeX = x;
                            swipeY = y;
                            swipeing = true;
                            increaseValue();
                            return true;
                        }
                        else if (swipeX - x > swipeSensibilityPixels || y - swipeY > swipeSensibilityPixels) {
                            swipeX = x;
                            swipeY = y;
                            swipeing = true;
                            decreaseValue();
                            return true;
                        }
                    }
                    else if (action == MotionEvent.ACTION_UP) {
                        if (!swipeing) clickMe(view);    // click
                        return true;
                    }
                    return false;
                }
                else if (swipeDirection == SWIPEDIRECTION_CIRCULAR) { // circular
                    int x = (int) motionEvent.getX();
                    int y = (int) motionEvent.getY();
                    if (action == MotionEvent.ACTION_DOWN) {
                        swipeing = false;
                        disallowParentToHandleTouchEvents(); // needed when Knob's parent is a ScrollView
                    }
                    else if (action == MotionEvent.ACTION_MOVE) {
                        double angle = Math.atan2((double)(y-centerY), (double)(x-centerX));
                        swipeing = true;
                        setValueByAngle(angle, animation);
                        return true;
                    }
                    else if (action == MotionEvent.ACTION_UP) {
                        if (!swipeing) clickMe(view);    // click
                        return true;
                    }
                    return false;

                }

                return false;
            }
        });

        spring.addListener(new SimpleSpringListener(){
                               @Override
                               public void onSpringUpdate(Spring spring) {
                                   currentAngle = spring.getCurrentValue();
                                   postInvalidate();
                               }});
    }

    void createPopupMenu(View view) {
        PopupMenu mPopupMenu = new PopupMenu(getContext(), view);
        if (balloonValuesArray == null)
            for (int w=0; w<numberOfStates; w++)
                mPopupMenu.getMenu().add(Menu.NONE, w+1, w+1, Integer.toString(w));
        else
            for (int w=0; w<numberOfStates; w++)
                mPopupMenu.getMenu().add(Menu.NONE, w+1, w+1, balloonValuesArray[w].toString());

        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int i = item.getItemId()-1;
                setState(i);
                return true;
            }
        });

        mPopupMenu.show();

    }

    void initStatus() {
        currentState = defaultState;
        previousState = defaultState;
        calcActualState();
        currentAngle = calcAngle(currentState);
        spring.setCurrentValue(currentAngle);
    }

    void initBalloons() {

    }

    // behaviour

    public void toggle(boolean animate) {
        increaseValue(animate);
    }
    public void toggle() {
        toggle(animation);
    }

    public void inverseToggle(boolean animate) {
        decreaseValue(animate);
    }
    public void inverseToggle() { inverseToggle(animation);}

    public void revertToDefault(boolean animate) {
        setState(defaultState, animate);
    }
    public void revertToDefault() { revertToDefault(animation); }

    private void calcActualState() {
        actualState = currentState % numberOfStates;
        if (actualState < 0) actualState += numberOfStates;
    }

    public void increaseValue(boolean animate) {
        previousState = currentState;
        currentState = (currentState+1); // % numberOfStates;
        if (!freeRotation && currentState >= numberOfStates) currentState = numberOfStates-1;
        calcActualState();
        if(listener != null) listener.onState(actualState);
        takeEffect(animate);
    }
    public void increaseValue() { increaseValue(animation);}

    public void decreaseValue(boolean animate) {
        previousState = currentState;
        currentState = (currentState-1); // % numberOfStates;
        if (!freeRotation && currentState<0) currentState = 0;
        calcActualState();
        if(listener != null) listener.onState(actualState);
        takeEffect(animate);
    }
    public void decreaseValue() { decreaseValue(animation);}

    public void setValueByAngle(double angle, boolean animate) {  // sets the value of the knob given an angle instead of a state
        if (numberOfStates <= 1)
            return;
        previousState = currentState;
        double min = Math.toRadians((double)minAngle);
        double max = Math.toRadians((double)maxAngle - 0.0001);
        double range = max - min;

        double singleStepAngle = range / (numberOfStates);
        if (PI*2 - range < singleStepAngle)
            singleStepAngle = range / numberOfStates;

        min = (float)normalizeAngle(min);
        while (min > max) max += 2*PI;      // both min and max are positive and in the correct order.

        angle = normalizeAngle(angle + PI/2);
        while (angle < min) angle += 2*PI;             // set angle after minangle
        if (angle > max) { // if angle is out of range because the range is limited set to the closer limit
            if (angle - max > min - angle + PI*2)
                angle = min;
            else
                angle = max;
        }

        currentState = (int)((angle - min) / singleStepAngle);   // calculate value
        if (!freeRotation && Math.abs(currentState - previousState) == numberOfStates-1)    // manage free rotation
            currentState = previousState;

        calcActualState();
        if(listener != null) listener.onState(actualState);
        takeEffect(animate);
    }

    private void takeEffect(boolean animate) {
        if (animate) {
            setIndicatorAngleWithDirection();
        } else {
            spring.setCurrentValue(calcAngle(actualState));
        }
        postInvalidate();
    }

    // public listener interface

    private OnStateChanged listener;
    public interface OnStateChanged{
        public void onState(int state);
    }

    public void setOnStateChanged(OnStateChanged onStateChanged) {
        listener = onStateChanged;
    }

    // methods

    public void setState(int newState) {
        setState(newState, animation);
    }
    public void setState(int newState, boolean animate) {
        forceState(newState, animate);
        if(listener != null) listener.onState(currentState);
    }
    public void forceState(int newState) {
        forceState(newState, animation);
    }
    public void forceState(int newState, boolean animate) {
        previousState = currentState;
        currentState = newState;
        calcActualState();
        takeEffect(animate);
    }
    public int getState() {
        return actualState;
    }

    // getters and setters

    public int getNumberOfStates() {
        return numberOfStates;
    }

    public void setNumberOfStates(int numberOfStates) {
        setNumberOfStates(numberOfStates, animation);
    }
    public void setNumberOfStates(int numberOfStates, boolean animate) {
        this.numberOfStates = numberOfStates;
        takeEffect(animate);
    }

    public int getDefaultState() {
        return defaultState;
    }

    public void setDefaultState(int defaultState) {
        this.defaultState = defaultState;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
        takeEffect(animation);
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
        takeEffect(animation);
    }

    public int getIndicatorWidth() {
        return indicatorWidth;
    }

    public void setIndicatorWidth(int indicatorWidth) {
        this.indicatorWidth = indicatorWidth;
        takeEffect(animation);
    }

    public int getIndicatorColor() {
        return indicatorColor;
    }

    public void setIndicatorColor(int indicatorColor) {
        this.indicatorColor = indicatorColor;
        takeEffect(animation);
    }

    public float getIndicatorRelativeLength() {
        return indicatorRelativeLength;
    }

    public void setIndicatorRelativeLength(float indicatorRelativeLength) {
        this.indicatorRelativeLength = indicatorRelativeLength;
        takeEffect(animation);
    }

    public int getKnobColor() {
        return knobColor;
    }

    public void setKnobColor(int knobColor) {
        this.knobColor = knobColor;
        takeEffect(animation);
    }

    public float getKnobRelativeRadius() {
        return knobRelativeRadius;
    }

    public void setKnobRelativeRadius(float knobRelativeRadius) {
        this.knobRelativeRadius = knobRelativeRadius;
        takeEffect(animation);
    }

    public float getKnobCenterRelativeRadius() {
        return knobCenterRelativeRadius;
    }

    public void setKnobCenterRelativeRadius(float knobCenterRelativeRadius) {
        this.knobCenterRelativeRadius = knobCenterRelativeRadius;
        takeEffect(animation);
    }

    public int getKnobCenterColor() {
        return knobCenterColor;
    }

    public void setKnobCenterColor(int knobCenterColor) {
        this.knobCenterColor = knobCenterColor;
        takeEffect(animation);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        takeEffect(animation);
    }

    public boolean isAnimation() {
        return animation;
    }

    public void setAnimation(boolean animation) {
        this.animation = animation;
    }

    public float getAnimationSpeed() {
        return animationSpeed;
    }

    public void setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    public float getAnimationBounciness() {
        return animationBounciness;
    }

    public void setAnimationBounciness(float animationBounciness) {
        this.animationBounciness = animationBounciness;
    }

    public int getStateMarkersWidth() {
        return stateMarkersWidth;
    }

    public void setStateMarkersWidth(int stateMarkersWidth) {
        this.stateMarkersWidth = stateMarkersWidth;
        takeEffect(animation);
    }

    public int getStateMarkersColor() {
        return stateMarkersColor;
    }

    public void setStateMarkersColor(int stateMarkersColor) {
        this.stateMarkersColor = stateMarkersColor;
        takeEffect(animation);
    }

    public int getSelectedStateMarkerColor() {
        return selectedStateMarkerColor;
    }

    public void setSelectedStateMarkerColor(int selectedStateMarkerColor) {
        this.selectedStateMarkerColor = selectedStateMarkerColor;
        takeEffect(animation);
    }

    public float getStateMarkersRelativeLength() {
        return stateMarkersRelativeLength;
    }

    public void setStateMarkersRelativeLength(float stateMarkersRelativeLength) {
        this.stateMarkersRelativeLength = stateMarkersRelativeLength;
        takeEffect(animation);
    }

    public float getKnobRadius() {
        return knobRadius;
    }

    public void setKnobRadius(float knobRadius) {
        this.knobRadius = knobRadius;
        takeEffect(animation);
    }

    public boolean isFreeRotation() {
        return freeRotation;
    }

    public void setFreeRotation(boolean freeRotation) {
        this.freeRotation = freeRotation;
    }

    public int getSwipeDirection() {
        return swipeDirection;
    }

    public void setSwipeDirection(int swipeDirection) {
        this.swipeDirection = swipeDirection;
    }

    public int getSwipeSensibilityPixels() {
        return swipeSensibilityPixels;
    }

    public void setSwipeSensibilityPixels(int swipeSensibilityPixels) {
        this.swipeSensibilityPixels = swipeSensibilityPixels;
    }

    public int getStateMarkersAccentWidth() {
        return stateMarkersAccentWidth;
    }

    public void setStateMarkersAccentWidth(int stateMarkersAccentWidth) {
        this.stateMarkersAccentWidth = stateMarkersAccentWidth;
        takeEffect(animation);
    }

    public int getStateMarkersAccentColor() {
        return stateMarkersAccentColor;
    }

    public void setStateMarkersAccentColor(int stateMarkersAccentColor) {
        this.stateMarkersAccentColor = stateMarkersAccentColor;
        takeEffect(animation);
    }

    public float getStateMarkersAccentRelativeLength() {
        return stateMarkersAccentRelativeLength;
    }

    public void setStateMarkersAccentRelativeLength(float stateMarkersAccentRelativeLength) {
        this.stateMarkersAccentRelativeLength = stateMarkersAccentRelativeLength;
        takeEffect(animation);
    }

    public int getStateMarkersAccentPeriodicity() {
        return stateMarkersAccentPeriodicity;
    }

    public void setStateMarkersAccentPeriodicity(int stateMarkersAccentPeriodicity) {
        this.stateMarkersAccentPeriodicity = stateMarkersAccentPeriodicity;
        takeEffect(animation);
    }

    public int getKnobDrawableRes() {
        return knobDrawableRes;
    }

    public void setKnobDrawableRes(int knobDrawableRes) {
        this.knobDrawableRes = knobDrawableRes;
        takeEffect(animation);
    }

    public boolean isKnobDrawableRotates() {
        return knobDrawableRotates;
    }

    public void setKnobDrawableRotates(boolean knobDrawableRotates) {
        this.knobDrawableRotates = knobDrawableRotates;
        takeEffect(animation);
    }

    public float getCircularIndicatorRelativeRadius() {
        return circularIndicatorRelativeRadius;
    }

    public void setCircularIndicatorRelativeRadius(float circularIndicatorRelativeRadius) {
        this.circularIndicatorRelativeRadius = circularIndicatorRelativeRadius;
        takeEffect(animation);
    }

    public float getCircularIndicatorRelativePosition() {
        return circularIndicatorRelativePosition;
    }

    public void setCircularIndicatorRelativePosition(float circularIndicatorRelativePosition) {
        this.circularIndicatorRelativePosition = circularIndicatorRelativePosition;
        takeEffect(animation);
    }

    public int getCircularIndicatorColor() {
        return circularIndicatorColor;
    }

    public void setCircularIndicatorColor(int circularIndicatorColor) {
        this.circularIndicatorColor = circularIndicatorColor;
        takeEffect(animation);
    }

    public boolean isSelectedStateMarkerContinuous() {
        return selectedStateMarkerContinuous;
    }

    public void setSelectedStateMarkerContinuous(boolean selectedStateMarkerContinuous) {
        this.selectedStateMarkerContinuous = selectedStateMarkerContinuous;
        takeEffect(animation);
    }

    public float getMinAngle() {
        return minAngle;
    }

    public void setMinAngle(float minAngle) {
        this.minAngle = minAngle;
        takeEffect(animation);
    }

    public float getMaxAngle() {
        return maxAngle;
    }

    public void setMaxAngle(float maxAngle) {
        this.maxAngle = maxAngle;
        takeEffect(animation);
    }

    public float getExternalRadius() {
        return externalRadius;
    }

    public void setExternalRadius(float externalRadius) {
        this.externalRadius = externalRadius;
        takeEffect(animation);
    }

    public Drawable getKnobDrawable() {
        return knobDrawable;
    }

    public void setKnobDrawable(Drawable knobDrawable) {
        this.knobDrawable = knobDrawable;
        takeEffect(animation);
    }

    public boolean isShowBalloonValues() {
        return showBalloonValues;
    }

    public void setShowBalloonValues(boolean showBalloonValues) {
        this.showBalloonValues = showBalloonValues;
    }

    public int getBalloonValuesTimeToLive() {
        return balloonValuesTimeToLive;
    }

    public void setBalloonValuesTimeToLive(int balloonValuesTimeToLive) {
        this.balloonValuesTimeToLive = balloonValuesTimeToLive;
    }

    public float getBalloonValuesRelativePosition() {
        return balloonValuesRelativePosition;
    }

    public void setBalloonValuesRelativePosition(float balloonValuesRelativePosition) {
        this.balloonValuesRelativePosition = balloonValuesRelativePosition;
    }

    public float getBalloonValuesTextSize() {
        return balloonValuesTextSize;
    }

    public void setBalloonValuesTextSize(float balloonValuesTextSize) {
        this.balloonValuesTextSize = balloonValuesTextSize;
    }

    public boolean isBalloonValuesSlightlyTransparent() {
        return balloonValuesSlightlyTransparent;
    }

    public void setBalloonValuesSlightlyTransparent(boolean balloonValuesSlightlyTransparent) {
        this.balloonValuesSlightlyTransparent = balloonValuesSlightlyTransparent;
    }

    public int getClickBehaviour() {
        return clickBehaviour;
    }

    public void setClickBehaviour(int clickBehaviour) {
        this.clickBehaviour = clickBehaviour;
    }

    public void setUserBehaviour(Runnable userRunnable) {
        // when "user" click behaviour is selected
        this.userRunnable = userRunnable;
    }

    public void runUserBehaviour() {   // to be initialized with setUserBehaviour()
        if (userRunnable == null) return;
        userRunnable.run();
    }
}

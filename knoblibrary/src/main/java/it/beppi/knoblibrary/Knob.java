package it.beppi.knoblibrary;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

/**
 * Created by Beppi on 06/12/2016.
 */

public class Knob extends View {

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
    }

    void paintKnob(Canvas canvas) {
        paint.setColor(knobColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, knobRadius, paint);
    }

    void paintKnobBorder(Canvas canvas) {
        if (borderWidth == 0) return;
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);
        canvas.drawCircle(centerX, centerY, knobRadius, paint);
    }

    void paintKnobCenter(Canvas canvas) {
        if (knobCenterRelativeRadius == 0f) return;
        paint.setColor(knobCenterColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, knobCenterRelativeRadius * knobRadius, paint);
    }

    double calcAngle(int position) {
        return Math.PI - position * (2 * Math.PI / numberOfStates);
    }

    double calcAngleWithDirection(int position) {
      double angle = calcAngle(position);
        double angle0 = spring.getCurrentValue();
        double diff1 = Math.abs(angle0 - angle);
        double diff2 = Math.abs(angle - 2 * Math.PI - angle0);

//        Log.d("beppim", "angle  " + Double.toString(angle));
//        Log.d("beppim", "angle0 " + Double.toString(angle0));
//        Log.d("beppim", "diff1  " + Double.toString(diff1));
//        Log.d("beppim", "diff2  " + Double.toString(diff2));

        if (diff1<diff2) return angle; else return angle - 2 * Math.PI;
    }

    void paintIndicator(Canvas canvas) {
        if (indicatorWidth == 0) return;
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

        float posX = centerX + (float)(externalRadius * circularIndicatorRelativePosition * Math.sin(currentAngle));
        float posY = centerY + (float)(externalRadius * circularIndicatorRelativePosition * Math.cos(currentAngle));
        canvas.drawCircle(posX, posY, externalRadius * circularIndicatorRelativeRadius, paint);
    }

    void paintMarkers(Canvas canvas) {
        paint.setStrokeWidth(stateMarkersWidth);
        int currentStateModded = currentState % numberOfStates;
        for (int w=0; w<numberOfStates; w++) {
            double angle = calcAngle(w);
            float startX = centerX + (float)(externalRadius * (1-stateMarkersRelativeLength) * Math.sin(angle));
            float startY = centerY + (float)(externalRadius * (1-stateMarkersRelativeLength) * Math.cos(angle));
            float endX = centerX + (float)(externalRadius * Math.sin(angle));
            float endY = centerY + (float)(externalRadius * Math.cos(angle));
            paint.setColor(w==currentStateModded ? selectedStateMarkerColor : stateMarkersColor);
            canvas.drawLine(startX, startY, endX, endY, paint);
        }
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
    private int currentState = defaultState;
    private boolean animation = true;
    private float animationSpeed = 8;
    private float animationBounciness = 10;
    private int stateMarkersWidth = 3;
    private int stateMarkersColor = Color.BLACK;
    private int selectedStateMarkerColor = Color.YELLOW;
    private float stateMarkersRelativeLength = 0.08f;


    // initialize

    void init(AttributeSet attrs) {
        ctx = getContext();
        loadAttributes(attrs);
        initTools();
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


    void initTools() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeCap(Paint.Cap.ROUND);

        springSystem = SpringSystem.create();
        spring = springSystem.createSpring();
        spring.setSpringConfig(SpringConfig.fromBouncinessAndSpeed((double)animationSpeed, (double)animationBounciness));
        spring.setOvershootClampingEnabled(false);
    }

    void loadAttributes(AttributeSet attrs) {
        if (attrs == null) return;

        TypedArray typedArray = ctx.obtainStyledAttributes(attrs, R.styleable.KnobSelector);

        numberOfStates = typedArray.getInt(R.styleable.KnobSelector_kNumberOfStates, numberOfStates);
        defaultState = typedArray.getInt(R.styleable.KnobSelector_kDefaultState, defaultState);

        borderWidth = typedArray.getDimensionPixelSize(R.styleable.KnobSelector_kBorderWidth, borderWidth);
        borderColor = typedArray.getColor(R.styleable.KnobSelector_kBorderColor, borderColor);

        indicatorWidth = typedArray.getDimensionPixelSize(R.styleable.KnobSelector_kIndicatorWidth, indicatorWidth);
        indicatorColor = typedArray.getColor(R.styleable.KnobSelector_kIndicatorColor, indicatorColor);
        indicatorRelativeLength = typedArray.getFloat(R.styleable.KnobSelector_kIndicatorRelativeLength, indicatorRelativeLength);

        circularIndicatorRelativeRadius = typedArray.getFloat(R.styleable.KnobSelector_kCircularIndicatorRelativeRadius, circularIndicatorRelativeRadius);
        circularIndicatorRelativePosition = typedArray.getFloat(R.styleable.KnobSelector_kCircularIndicatorRelativePosition, circularIndicatorRelativePosition);
        circularIndicatorColor = typedArray.getColor(R.styleable.KnobSelector_kCircularIndicatorColor, circularIndicatorColor);

        knobColor = typedArray.getColor(R.styleable.KnobSelector_kKnobColor, knobColor);
        knobRelativeRadius = typedArray.getFloat(R.styleable.KnobSelector_kKnobRelativeRadius, knobRelativeRadius);

        knobCenterRelativeRadius = typedArray.getFloat(R.styleable.KnobSelector_kKnobCenterRelativeRadius, knobCenterRelativeRadius);
        knobCenterColor = typedArray.getColor(R.styleable.KnobSelector_kKnobCenterColor, knobCenterColor);

        stateMarkersWidth = typedArray.getDimensionPixelSize(R.styleable.KnobSelector_kStateMarkersWidth, stateMarkersWidth);
        stateMarkersColor = typedArray.getColor(R.styleable.KnobSelector_kStateMarkersColor, stateMarkersColor);
        selectedStateMarkerColor = typedArray.getColor(R.styleable.KnobSelector_kSelectedStateMarkerColor, selectedStateMarkerColor);
        stateMarkersRelativeLength = typedArray.getFloat(R.styleable.KnobSelector_kStateMarkersRelativeLength, stateMarkersRelativeLength);

        animation = typedArray.getBoolean(R.styleable.KnobSelector_kAnimation, animation);
        animationSpeed = typedArray.getFloat(R.styleable.KnobSelector_kAnimationSpeed, animationSpeed);
        animationBounciness = typedArray.getFloat(R.styleable.KnobSelector_kAnimationBounciness, animationBounciness);

        enabled = typedArray.getBoolean(R.styleable.KnobSelector_kEnabled, enabled);

        typedArray.recycle();
    }

    void initListeners() {
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle(animation);
            }
        });
        spring.addListener(new SimpleSpringListener(){
                               @Override
                               public void onSpringUpdate(Spring spring) {
                                   currentAngle = spring.getCurrentValue();
                                   postInvalidate();
                               }});
    }

    void initStatus() {
        currentState = defaultState;
        previousState = defaultState;
        currentAngle = calcAngle(currentState);
        spring.setCurrentValue(currentAngle);
    }

    // behaviour

    public void toggle(boolean animate) {
        previousState = currentState;
        currentState = (currentState+1); // % numberOfStates;
        if(listener != null) listener.onState(currentState % numberOfStates);
        takeEffect(animate);
    }
    public void toggle() {
        toggle(animation);
    }


    private void takeEffect(boolean animate) {
        if (animate) {
            spring.setEndValue(calcAngleWithDirection(currentState));
        } else {
            spring.setCurrentValue(calcAngle(currentState));
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
        takeEffect(animate);
    }
    public int getState() {
        return currentState % numberOfStates;
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
}

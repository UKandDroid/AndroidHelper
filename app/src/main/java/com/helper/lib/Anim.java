package com.helper.lib;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ubaid on 15/06/2016.
 */
// Version 1.1.0
public class Anim {
    public static final String TYPE_VALUE = "value";
    public static final String TYPE_SCALE = "scale";
    public static final String TYPE_ROTATE = "rotate";
    public static final String TYPE_SCALE_X = "scale_x";
    public static final String TYPE_SCALE_Y = "scale_y";
    public static final String TYPE_TRANSLATE_X = "trans_x";
    public static final String TYPE_TRANSLATE_Y = "trans_y";

    public static final String INTER_CYCLE = "cycle";
    public static final String INTER_LINEAR = "linear";
    public static final String INTER_BOUNCE = "bounce";
    public static final String INTER_OVERSHOOT = "overshoot";
    public static final String INTER_ACCELERATE = "accelerate";
    public static final String INTER_DECELERATE = "decelerate";
    public static final String INTER_ANTICIPATE = "anticipate";
    public static final String INTER_ACC_DECELERATE = "acc_decelerate";
    public static final String INTER_ANTICIPATE_OVERSHOOT = "anti_overshoot";
    private static String LOG_TAG = "Anim";

    private View view;
    private float pivotX = 0.5f, pivotY =0.5f;                    // Animation bug, if view has been translated, pivot 0.5 does not work
    private float width, height;
    private float moveX =0, moveY =0;
    private Flow flowAnimation;
    private AnimationSet animationSet = new AnimationSet(false);
    private String iDefaultInter = INTER_ACC_DECELERATE;
    private Flow.Code valueChangeList;
    private List<AnimData> listAnimData = new ArrayList<>();
    private boolean bLayoutChangeCalled = false;

    public Anim(){}
    public Anim(View v){ setView(v);}

    public void setView(View v){
        view = v;
        v.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (!bLayoutChangeCalled) {
                    flowAnimation.runOnUI(-1);                              // Load animations
                    bLayoutChangeCalled = true;
                }
            }
        });
    }
    // METHOD sets default interpolator, used when none is provided
    public void setInterpolator( String i){ iDefaultInter = i;}

    // METHOD add animation, method gets start time from previous animation and sets it
    public void addAnimation(String iType, float iStart, float iEnd, long iDuration){
        addAnimation(iType, iDefaultInter, iStart, iEnd, iDuration);
    }

    // METHOD add animation, method gets start time from previous animation and sets it
    public void addAnimation(String iType, String iInterpolator, float iStart, float iEnd, long iDuration){
        long iStartTime = 0;
        int iCount = listAnimData.size();
        if (iCount > 0){
            iCount--;
            AnimData animData = listAnimData.get(iCount);
            iStartTime = animData.iStartTime+ animData.iDuration;
        }
        addAnimation(iType, iInterpolator, iStart, iEnd, iDuration, iStartTime);
    }

    public void addAnimation(String iType, String iInterpolator, float iStart, float iEnd,  long iDuration, long iStartTime){
        addAnimation( iType,  iInterpolator,  iStart,  iEnd,   iDuration,  iStartTime, false, 0);
    }

    public void addValueAnimator(int iValueAction, String iInterpolator, int iStart, int iEnd, long iDuration, long iStartTime){
        addAnimation( TYPE_VALUE,  iInterpolator,  iStart,  iEnd,   iDuration,  iStartTime, true, iValueAction);
    }

    public void setValueChangeListener(Flow.Code code){
        valueChangeList = code;
    }

    private void addAnimation(String sType, String sInterpolator, float iStart, float iEnd,  long iDuration, long iStartTime, boolean bValueAnimation, int iValueAction){
        Cloneable animator = null ;
        switch (sType){

            case TYPE_VALUE:
                animator = ValueAnimator.ofInt((int) iStart, (int) iEnd);
                break;
        }

        if(bValueAnimation){                                // If its a value animation, not UI animation
            ((ValueAnimator)animator).setDuration(iDuration);
            switch (sInterpolator){
                case INTER_CYCLE: ((ValueAnimator)animator).setInterpolator(new CycleInterpolator(1)); break;
                case INTER_LINEAR: ((ValueAnimator)animator).setInterpolator(new LinearInterpolator()); break;
                case INTER_BOUNCE: ((ValueAnimator)animator).setInterpolator(new BounceInterpolator()); break;
                case INTER_OVERSHOOT: ((ValueAnimator)animator).setInterpolator(new OvershootInterpolator()); break;
                case INTER_ACCELERATE: ((ValueAnimator)animator).setInterpolator(new AccelerateInterpolator()); break;
                case INTER_DECELERATE: ((ValueAnimator)animator).setInterpolator(new DecelerateInterpolator()); break;
                case INTER_ANTICIPATE: ((ValueAnimator)animator).setInterpolator(new AnticipateInterpolator()); break;
                case INTER_ACC_DECELERATE: ((ValueAnimator)animator).setInterpolator(new AccelerateDecelerateInterpolator()); break;
                case INTER_ANTICIPATE_OVERSHOOT: ((ValueAnimator)animator).setInterpolator(new AnticipateOvershootInterpolator()); break;
            }
        }

        AnimData anim = new AnimData();
        anim.iDuration = iDuration;
        anim.iStart = iStart;
        anim.iEnd = iEnd;
        anim.sType = sType;
        anim.iAction = iValueAction;
        anim.sInterpolator = sInterpolator;
        anim.anim = animator;
        anim.iAction = iValueAction;
        anim.bValueAnim = bValueAnimation;
        anim.iStartTime = iStartTime;

        listAnimData.add(anim);
    }



    private Animation getAnimation(AnimData animData){
        Animation animator = null ;
        switch (animData.sType){
            case TYPE_SCALE:
                animator = new ScaleAnimation(animData.iStart, animData.iEnd, animData.iStart, animData.iEnd, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case TYPE_SCALE_X:
                animator = new ScaleAnimation(animData.iStart, animData.iEnd, 1.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case TYPE_SCALE_Y:
                animator = new ScaleAnimation(1.0f, 1.0f, animData.iStart, animData.iEnd, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case TYPE_ROTATE:
                animator = new RotateAnimation(animData.iStart, animData.iEnd, Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
                break;

            case TYPE_TRANSLATE_X:
                moveX += (animData.iEnd-animData.iStart);
                pivotX = 0.5f + ((moveX/pxToDp(view.getWidth()))*0.5f);
                animator = new TranslateAnimation(animData.iStart, animData.iEnd, 0, 0);
                break;

            case TYPE_TRANSLATE_Y:
                moveY += (animData.iEnd-animData.iStart);
                pivotY = 0.5f +((moveY/pxToDp(view.getHeight()))*0.5f);
                animator = new TranslateAnimation(0, 0, animData.iStart, animData.iEnd);
                break;
        }

        animator.setDuration(animData.iDuration);
        animator.setStartOffset(animData.iStartTime);

        switch (animData.sInterpolator){
            case INTER_CYCLE: animator.setInterpolator(new CycleInterpolator(1)); break;
            case INTER_LINEAR: animator.setInterpolator(new LinearInterpolator()); break;
            case INTER_BOUNCE: animator.setInterpolator(new BounceInterpolator()); break;
            case INTER_OVERSHOOT: animator.setInterpolator(new OvershootInterpolator()); break;
            case INTER_ACCELERATE: animator.setInterpolator(new AccelerateInterpolator()); break;
            case INTER_DECELERATE: animator.setInterpolator(new DecelerateInterpolator()); break;
            case INTER_ANTICIPATE: animator.setInterpolator(new AnticipateInterpolator()); break;
            case INTER_ACC_DECELERATE: animator.setInterpolator(new AccelerateDecelerateInterpolator()); break;
            case INTER_ANTICIPATE_OVERSHOOT: animator.setInterpolator(new AnticipateOvershootInterpolator()); break;
        }

        return animator;
    }


    // METHOD starts Value animators, view animators start onLayoutChange Listener for the view
    public void start(){
        flowAnimation = new Flow(actionCode);
        for(int i=0; i < listAnimData.size(); i++){
            AnimData animData = listAnimData.get(i);
            if(animData.bValueAnim){
                flowAnimation.runDelayedOnUI(i, animData.bValueAnim, 0, listAnimData.get(i).iStartTime);
            }
        }
    }

    Flow.Code actionCode = new Flow.Code() {
        @Override public void onAction(int iAction, boolean bSuccess, int iExtra, Object data) {
            switch (iAction){
                case 1:                 // Run Value animations
                    final ValueAnimator valueAnim = (ValueAnimator) listAnimData.get(iAction).anim;
                    final int iValueAction = listAnimData.get(iAction).iAction;
                    valueAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override public void onAnimationUpdate(ValueAnimator animation) {
                            Integer value = (Integer) animation.getAnimatedValue();
                            if(valueChangeList != null){
                                valueChangeList.onAction(iValueAction, true, value, null);
                            }}});
                    valueAnim.start();
                    break;

                case -1:                // load animation bit late so we have view width and height
                    for(int i=0; i < listAnimData.size(); i++){
                        AnimData animData = listAnimData.get(i);
                        if(!animData.bValueAnim){
                            animationSet.addAnimation(getAnimation(animData));
                        }
                    }
                    view.clearAnimation();
                    view.setAnimation(animationSet);
                    animationSet.start();
                    animationSet.setFillAfter(true);
                    break;
            }
        }} ;


    // METHOD - Convert pixels to dp
    private  int pxToDp( int iPixels){
        DisplayMetrics displayMetrics = view.getContext().getResources().getDisplayMetrics();
        int dp = Math.round(iPixels / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    public void stop(){
        flowAnimation.stop();
        animationSet.cancel();
        int iCount = listAnimData.size();
        if(view != null){ view.clearAnimation(); }
        for(int i =0; i < iCount; i++){
            AnimData anim = listAnimData.get(i);
            if(anim.bValueAnim){
                ((ValueAnimator)anim.anim).cancel();
            } else {
                ((Animation)anim.anim).cancel();
            }
        }
    }

    // CLASS to hold animation params
    class AnimData {
        Object anim;
        long iStartTime, iDuration;
        public int iAction;
        float iStart, iEnd;
        boolean bValueAnim = false;
        String sType, sInterpolator;
    }
}

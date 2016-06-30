package com.helper.lib;

import android.animation.ValueAnimator;
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
    private Flow flowAnimation;
    private AnimationSet animationSet;
    private String iDefaultInter = INTER_ACC_DECELERATE;
    private Flow.Code valueChangeList;
    private List<AnimData> listAnimation = new ArrayList<>();

    public Anim(){}
    public Anim(View v){ setView(v);}

    public void setView(View v){ view = v; }
    // METHOD sets default interpolator, used when none is provided
    public void setInterpolator( String i){ iDefaultInter = i;}

    // METHOD add animation, method gets start time from previous animation and sets it
    public void addAnimation(String iType, float iStart, float iEnd, long iDuration){
        addAnimation(iType, iDefaultInter, iStart, iEnd, iDuration);
    }

    // METHOD add animation, method gets start time from previous animation and sets it
    public void addAnimation(String iType, String iInterpolator, float iStart, float iEnd, long iDuration){
        long iStartTime = 0;
        int iCount = listAnimation.size();
        if (iCount > 0){
            iCount--;
            AnimData animData = listAnimation.get(iCount);
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

    private void addAnimation(String iType, String iInterpolator, float iStart, float iEnd,  long iDuration, long iStartTime, boolean bValueAnimation, int iValueAction){
        Cloneable animator = null ;
        switch (iType){
            case TYPE_SCALE:
                animator = new ScaleAnimation(iStart, iEnd, iStart, iEnd, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case TYPE_SCALE_X:
                animator = new ScaleAnimation(iStart, iEnd, 1.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case TYPE_SCALE_Y:
                animator = new ScaleAnimation(1.0f, 1.0f, iStart, iEnd, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case TYPE_ROTATE:
                animator = new RotateAnimation(iStart, iEnd, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case TYPE_TRANSLATE_X:
                animator = new TranslateAnimation(iStart, iEnd, 0, 0);
                break;

            case TYPE_TRANSLATE_Y:
                animator = new TranslateAnimation(0, 0, iStart, iEnd);
                break;

            case TYPE_VALUE:
                animator = ValueAnimator.ofInt((int) iStart, (int) iEnd);
                break;
        }

        if(bValueAnimation){                                // If its a value animation, not UI animation
            ((ValueAnimator)animator).setDuration(iDuration);
            switch (iInterpolator){
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
        } else {
            ((Animation)animator).setFillAfter(true);
            ((Animation)animator).setDuration(iDuration);
            switch (iInterpolator){
                case INTER_CYCLE: ((Animation)animator).setInterpolator(new CycleInterpolator(1)); break;
                case INTER_LINEAR: ((Animation)animator).setInterpolator(new LinearInterpolator()); break;
                case INTER_BOUNCE: ((Animation)animator).setInterpolator(new BounceInterpolator()); break;
                case INTER_OVERSHOOT: ((Animation)animator).setInterpolator(new OvershootInterpolator()); break;
                case INTER_ACCELERATE: ((Animation)animator).setInterpolator(new AccelerateInterpolator()); break;
                case INTER_DECELERATE: ((Animation)animator).setInterpolator(new DecelerateInterpolator()); break;
                case INTER_ANTICIPATE: ((Animation)animator).setInterpolator(new AnticipateInterpolator()); break;
                case INTER_ACC_DECELERATE: ((Animation)animator).setInterpolator(new AccelerateDecelerateInterpolator()); break;
                case INTER_ANTICIPATE_OVERSHOOT: ((Animation)animator).setInterpolator(new AnticipateOvershootInterpolator()); break;
            }
        }

        AnimData anim = new AnimData();
        anim.iDuration = iDuration;
        anim.anim = animator;
        anim.iAction = iValueAction;
        anim.bValueAnim = bValueAnimation;
        anim.iStartTime = iStartTime;
        listAnimation.add(anim);
    }
    // METHOD starts animation for the views
    public void start(){
        animationSet = new AnimationSet(false);
        flowAnimation = new Flow(actionCode);
        for(int i=0; i < listAnimation.size(); i++){
            AnimData animData = listAnimation.get(i);
            flowAnimation.runDelayedOnUI(i, animData.bValueAnim, 0, listAnimation.get(i).iStartTime);
        }
    }

    Flow.Code actionCode = new Flow.Code() {
        @Override public void onAction(int iAction, boolean bSuccess, int iExtra, Object data) {
            if(bSuccess){
                final ValueAnimator valueAnim = (ValueAnimator)listAnimation.get(iAction).anim;
                final int iValueAction = listAnimation.get(iAction).iAction;
                valueAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override public void onAnimationUpdate(ValueAnimator animation) {
                        Integer value = (Integer) animation.getAnimatedValue();
                        if(valueChangeList != null){
                            valueChangeList.onAction(iValueAction, true, value, null);
                        }}});
                valueAnim.start();
            } else {
                Animation anim = (Animation)listAnimation.get(iAction).anim;
                List<Animation> setAnim = animationSet.getAnimations();   // Remove completed animations from animation set
                animationSet = new AnimationSet(false);                   // By creating a new set and add only those animations
                animationSet.setFillAfter(true);                          // that are not ended yet
                for(int i=0; i< setAnim.size(); i++){
                    if(!setAnim.get(i).hasEnded())
                        animationSet.addAnimation(setAnim.get(i));
                }
                animationSet.addAnimation(anim);
                view.clearAnimation();
                view.setAnimation(animationSet);
                animationSet.start();
            }
        }} ;

    public void stop(){
        flowAnimation.stop();
        animationSet.cancel();
        int iCount = listAnimation.size();
        if(view != null){ view.clearAnimation(); }
        for(int i =0; i < iCount; i++){
            AnimData anim = listAnimation.get(i);
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
        boolean bValueAnim = false;
    }
}

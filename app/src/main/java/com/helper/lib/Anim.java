package com.helper.lib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
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

// Version 1.2.2
// Added start delay in every animation instead added to startTime list
// Added method set repeat count for animation
// Added ValueAnimator
// Added alpha animation
public class Anim {
    public static final int ANIM_START = 0;
    public static final int ANIM_RUNNING = 1;
    public static final int ANIM_END = 2;
    // INTERFACE callback for Value Animation Updates
    public interface ValueListener { void onValueChange( int iState, float value); }

    public static final int TYPE_SCALE = 0;
    public static final int TYPE_ROTATE = 1;
    public static final int TYPE_SCALE_X = 2;
    public static final int TYPE_SCALE_Y = 3;
    public static final int TYPE_TRANSLATE_X = 4;
    public static final int TYPE_TRANSLATE_Y = 5;
    public static final int TYPE_ALPHA = 6;
    public static final int TYPE_VALUE = 7;


    public static final int INTER_CYCLE = 0;
    public static final int INTER_LINEAR = 1;
    public static final int INTER_BOUNCE = 2;
    public static final int INTER_OVERSHOOT = 3;
    public static final int INTER_ACCELERATE = 4;
    public static final int INTER_DECELERATE = 5;
    public static final int INTER_ANTICIPATE = 6;
    public static final int INTER_ACC_DECELERATE = 7;
    public static final int INTER_ANTICIPATE_OVERSHOOT = 8;
    public static final int REPEAT_INFINITE = Animation.INFINITE;
    private static String LOG_TAG = "Anim";


    private View view;
    private Flow flowAnimation;
    private AnimationSet animationSet;
    private float fAnimValue = 0;
    private ValueAnimator valueAnim = null;
    private ValueListener valueListener = null;
    private int iDefaultInter = INTER_ACC_DECELERATE;
    private List<Long> listStartTime = new ArrayList<>();
    private List<Long> listDuration = new ArrayList<>();
    private List<Animation> listAnimation = new ArrayList<>();

    public Anim(){}
    public Anim(View v){ setView(v);}


    public void setView(View v){ view = v; }

    // METHOD sets default interpolator, used when none is provided
    public void setInterpolator( int i){ iDefaultInter = i;}

    // METHOD add animation, method gets start time from previous animation and sets it
    public void addAnimation(int iType, float iStart, float iEnd, long iDuration){
        addAnimation(iType, iDefaultInter, iStart, iEnd, iDuration);
    }

    public void setValueListener(ValueListener listener){
        valueListener = listener;
        if(valueAnim != null){
            valueAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator animation) {
                    if(valueListener!= null){
                        fAnimValue = (Float)animation.getAnimatedValue();
                        valueListener.onValueChange(ANIM_RUNNING, fAnimValue);
                    }}
            });

            valueAnim.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    valueListener.onValueChange(ANIM_END, fAnimValue);
                }
            });
        } else {
            throw new RuntimeException("Value animator not set");
        }
    }

    // METHOD add animation, method gets start time from previous animation and sets it
    public void addAnimation(int iType, int iInterpolator, float iStart, float iEnd, long iDuration){
        long iStartTime = 0;
        int iCount = listStartTime.size();
        if(iCount > 0){
            iCount--;
            iStartTime = listStartTime.get(iCount)+ listDuration.get(iCount);
        }
        addAnimation(iType, iInterpolator, iStart, iEnd, iDuration, iStartTime);
    }

    public void addAnimation(int iType, int iInterpolator, float iStart, float iEnd,  long iDuration, long iStartTime){
        Animation animator = null ;

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

            case TYPE_ALPHA:
                animator = new AlphaAnimation( iStart, iEnd);
                break;

            case TYPE_VALUE:
                valueAnim = ValueAnimator.ofFloat(iStart, iEnd);
                break;
        }

        if(animator != null){

            switch (iInterpolator){
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

            animator.setFillAfter(true);                // animation stays as it ended, view gone/Invisible wont work, unless animation stop is called
            animator.setDuration(iDuration);
            listDuration.add(iDuration);
            listStartTime.add(0l);                   // start delay is added in every animation instead as start offset
            animator.setStartOffset(iStartTime);     // start delay for every animation
            listAnimation.add(animator);
        }  else {

            switch (iInterpolator){
                case INTER_CYCLE: valueAnim.setInterpolator(new CycleInterpolator(1)); break;
                case INTER_LINEAR: valueAnim.setInterpolator(new LinearInterpolator()); break;
                case INTER_BOUNCE: valueAnim.setInterpolator(new BounceInterpolator()); break;
                case INTER_OVERSHOOT: valueAnim.setInterpolator(new OvershootInterpolator()); break;
                case INTER_ACCELERATE: valueAnim.setInterpolator(new AccelerateInterpolator()); break;
                case INTER_DECELERATE: valueAnim.setInterpolator(new DecelerateInterpolator()); break;
                case INTER_ANTICIPATE: valueAnim.setInterpolator(new AnticipateInterpolator()); break;
                case INTER_ACC_DECELERATE: valueAnim.setInterpolator(new AccelerateDecelerateInterpolator()); break;
                case INTER_ANTICIPATE_OVERSHOOT: valueAnim.setInterpolator(new AnticipateOvershootInterpolator()); break;
            }
            fAnimValue = iStart;
            valueAnim.setDuration(iDuration);
            valueAnim.setStartDelay(iStartTime);
        }
    }

    public void setRepeatCount(int index, int iCount){
        listAnimation.get(index).setRepeatCount(iCount);
    }


    // METHOD starts animation for the views
    public void start(){
        animationSet = new AnimationSet(false);
        flowAnimation = new Flow(actionCode);
        if(valueAnim != null){
            if(valueListener!= null){ valueListener.onValueChange(ANIM_START, fAnimValue);}
            valueAnim.start();
        }

        for(int i=0; i < listStartTime.size(); i++){
            flowAnimation.runDelayedOnUI(i, listStartTime.get(i));
        }
    }

    Flow.Code actionCode = new Flow.Code() {
        @Override public void onAction(int iAction, boolean bSuccess, int iExtra, Object data) {
            Animation anim =  listAnimation.get(iAction);
            animationSet.addAnimation(anim);
            view.clearAnimation();
            view.setAnimation(animationSet);
            animationSet.setFillAfter(true);
            animationSet.start();
        }} ;

    public void stop(){
        if(flowAnimation != null)
            flowAnimation.stop();
        if(animationSet != null)
            animationSet.cancel();
        int iCount = listAnimation.size();
        if(view != null){ view.clearAnimation(); }
        if(valueAnim != null){ valueAnim.cancel(); }

        for(int i =0; i < iCount; i++){
            listAnimation.get(i).cancel();
        }
    }


}

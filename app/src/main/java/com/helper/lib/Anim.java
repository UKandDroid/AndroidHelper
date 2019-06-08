package com.helper.lib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BaseInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;


import java.util.ArrayList;
import java.util.List;


// Version 1.2.5
// Removed Flow dependency
// Added documentation
public class Anim implements LifecycleObserver {
    public static final int ANIM_START = 0;
    public static final int ANIM_RUNNING = 1;
    public static final int ANIM_END = 2;

    // INTERFACE callback for Value Animation Updates
    public interface ValueListener { void onValueChange(int iState, float value); }

    public static final int TYPE_SCALE = 0; // size of view increases but does not force other views to move
    public static final int TYPE_ROTATE = 1;
    public static final int TYPE_SCALE_X = 2;
    public static final int TYPE_SCALE_Y = 3;
    public static final int TYPE_TRANSLATE_X = 4;
    public static final int TYPE_TRANSLATE_Y = 5;
    public static final int TYPE_ALPHA = 6;
    public static final int TYPE_VALUE = 7;
    public static final int TYPE_HEIGHT = 8; //  the view gets other views to move, does not happen with scale or translate animation
    public static final int TYPE_WIDTH = 9; //  the view gets other views to move

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
    private AnimationSet animationSet;
    private float fAnimValue = 0;
    private ValueListener valueListener = null;
    private ValueAnimator valueAnim = null;
    private int iDefaultInter = INTER_ACC_DECELERATE;
    private List<Long> listStartTime = new ArrayList<>();
    private List<Long> listDuration = new ArrayList<>();
    private List<Animation> listViewAnimation = new ArrayList<>();
    private List<ValueAnimator> listValueAnimation = new ArrayList<>();

    public Anim(){}
    public Anim(View v){ setView(v);}

    /** @param v   View that needs to be animated */
    public void setView(View v){ view = v; }

    /** Sets default interpolator, used when none is provided
     * @param i     default interpolator for animation, if not set when adding animation */
    public void setInterpolator( int i){ iDefaultInter = i;}

    /**  Adds an animation to list for view
     * @param iType   Animation type, TYPE_HEIGHT, TYPE_SCALE, etc
     * @param start   start value for animation
     * @param end     end value for animation
     * @param iDuration    animation duration in milli secs */
    public void addAnimation(int iType, float start, float end, long iDuration){
        addAnimation(iType, iDefaultInter, start, end, iDuration);
    }

    /** If value animation type is set, the value changes will be returned through this listener
     * @param listener   listener for value updates changes */
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

    /**  Adds an animation to list for view
     * @param iType             Animation type, TYPE_HEIGHT, TYPE_SCALE, etc
     * @param iInterpolator     interpolator for animation INTER_CYCLE, INTER_LINEAR, INTER_BOUNCE etc
     * @param start             start value for animation
     * @param end               end value for animation
     * @param iDuration         animation duration in milli secs */
    public void addAnimation(int iType, int iInterpolator, float start, float end, long iDuration){
        long iStartTime = 0;
        int iCount = listStartTime.size();
        if(iCount > 0){
            iCount--;
            iStartTime = listStartTime.get(iCount)+ listDuration.get(iCount);
        }
        addAnimation(iType, iInterpolator, start, end, iDuration, iStartTime);
    }

    /**  Adds an animation to list for view
     * @param iType             Animation type, TYPE_HEIGHT, TYPE_SCALE, etc
     * @param iInterpolator     interpolator for animation INTER_CYCLE, INTER_LINEAR, INTER_BOUNCE etc
     * @param start             start value for animation
     * @param end               end value for animation
     * @param iDuration         animation duration in milli secs
     * @param iStartTime        start time for animation, if animation needs to be delayed, or in case of multiple
     * animation does not start at the same time*/
    public void addAnimation(int iType, int iInterpolator, float start, float end, long iDuration, long iStartTime){
        Cloneable animator = null ;

        switch (iType){
            case TYPE_VALUE:
            case TYPE_HEIGHT:
            case TYPE_WIDTH:
                animator = ValueAnimator.ofFloat(start, end);
                break;

            case TYPE_SCALE:
                animator = new ScaleAnimation(start, end, start, end, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case TYPE_SCALE_X:
                animator = new ScaleAnimation(start, end, 1.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case TYPE_SCALE_Y:
                animator = new ScaleAnimation(1.0f, 1.0f, start, end, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case TYPE_ROTATE:
                animator = new RotateAnimation(start, end, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case TYPE_TRANSLATE_X:
                animator = new TranslateAnimation(start, end, 0, 0);
                break;

            case TYPE_TRANSLATE_Y:
                animator = new TranslateAnimation(0, 0, start, end);
                break;

            case TYPE_ALPHA:
                animator = new AlphaAnimation( start, end);
                break;

        }

        Interpolator interpolator;
        if(animator != null){
            switch (iInterpolator){
                case INTER_CYCLE: interpolator = new CycleInterpolator(1); break;
                case INTER_LINEAR: interpolator = new LinearInterpolator(); break;
                case INTER_BOUNCE: interpolator = new BounceInterpolator(); break;
                case INTER_OVERSHOOT: interpolator = new OvershootInterpolator(); break;
                case INTER_ACCELERATE: interpolator = new AccelerateInterpolator(); break;
                case INTER_DECELERATE: interpolator = new DecelerateInterpolator(); break;
                case INTER_ANTICIPATE: interpolator = new AnticipateInterpolator(); break;
                case INTER_ACC_DECELERATE: interpolator = new AccelerateDecelerateInterpolator(); break;
                case INTER_ANTICIPATE_OVERSHOOT: interpolator = new AnticipateOvershootInterpolator(); break;
                default:interpolator = new LinearInterpolator(); break;
            }

            if(animator instanceof ValueAnimator)
                ((ValueAnimator)animator).setInterpolator(interpolator);
            else
                ((Animation)animator).setInterpolator(interpolator);

            switch (iType){
                case TYPE_VALUE:
                    fAnimValue = start;
                    ((Animator) animator).setDuration(iDuration);
                    ((Animator) animator).setStartDelay(iStartTime);
                    valueAnim = ((ValueAnimator) animator);
                    listValueAnimation.add(((ValueAnimator) animator));
                    if(valueListener!= null){ valueListener.onValueChange(ANIM_START, fAnimValue);}
                    break;

                case TYPE_WIDTH:
                case TYPE_HEIGHT:
                    final ViewGroup.LayoutParams param = view.getLayoutParams();
                    ((Animator) animator).setDuration(iDuration);
                    ((Animator) animator).setStartDelay(iStartTime);
                    listValueAnimation.add(((ValueAnimator) animator));
                    ((ValueAnimator) animator).addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            if(iType == TYPE_HEIGHT) { param.height = ((Float) valueAnimator.getAnimatedValue()).intValue();
                            } else { param.width = ((Float)valueAnimator.getAnimatedValue()).intValue(); }
                            view.setLayoutParams(param);
                        }});
                    break;
                default:
                    ((Animation) animator).setFillAfter(true);                // animation stays as it ended, view gone/Invisible wont work, unless animation stop is called
                    ((Animation) animator).setDuration(iDuration);
                    listDuration.add(iDuration);
                    ((Animation) animator).setStartOffset(iStartTime);     // start delay for every animation
                    listViewAnimation.add(((Animation)animator));
            }
        }
    }

    /** For repeated animations
     * @param iAnimIndex    index of animation to be repeated
     * @param iCount        no of times to be repeated, REPEAT_INFINITE  for infinite loop*/
    public void setRepeatCount(int iAnimIndex, int iCount){
        listViewAnimation.get(iAnimIndex).setRepeatCount(iCount);
    }

    /** starts animation for the view */
    public void start(){
        // Start value based animations
        for(int i =0; i< listValueAnimation.size();i++){
            listValueAnimation.get(i).start();
        }

        // Start view based animations
        animationSet = new AnimationSet(false);
        for(int i=0; i < listViewAnimation.size(); i++){
            Animation anim =  listViewAnimation.get(i);
            animationSet.addAnimation(anim);
            view.clearAnimation();
            view.setAnimation(animationSet);
            animationSet.setFillAfter(true);
            animationSet.start();
        }
    }


    /** stops animation for the view */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stop(){
        if(animationSet != null) animationSet.cancel();
        if(valueAnim != null) valueAnim.cancel();
        if(view != null) view.clearAnimation();

        for(int i =0; i < listValueAnimation.size(); i++){
            listValueAnimation.get(i).cancel();
        }

        for(int i =0; i < listViewAnimation.size(); i++){
            listViewAnimation.get(i).cancel();
        }
    }

    // METHOD for logging
    private void log(String sLog){ log(1, sLog); }
    private void loge(String sLog){ loge(1, sLog); }
    private void logw(String sLog){ logw(1, sLog); }
    private void log(int iLevel, String sLog) { if(iLevel <= 2) { Log.d(LOG_TAG, sLog); } }
    private void loge(int iLevel, String sLog){ if(iLevel <= 2) { Log.e(LOG_TAG, sLog); } }
    private void logw(int iLevel, String sLog){ if(iLevel <= 2) { Log.w(LOG_TAG, sLog); } }
}

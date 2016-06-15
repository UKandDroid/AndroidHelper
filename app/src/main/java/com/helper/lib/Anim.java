package com.helper.lib;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
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
public class Anim {
    public static final int ROTATE = 0;
    public static final int SCALE_X = 1;
    public static final int SCALE_Y = 2;
    public static final int TRANSLATE_X = 3;
    public static final int TRANSLATE_Y = 4;

    public static final int INTER_CYCLE = 0;
    public static final int INTER_LINEAR = 1;
    public static final int INTER_BOUNCE = 2;
    public static final int INTER_OVERSHOOT = 3;
    public static final int INTER_ACCELERATE = 4;
    public static final int INTER_DECELERATE = 5;
    public static final int INTER_ANTICIPATE = 6;
    public static final int INTER_ACC_DECELERATE = 7;
    public static final int INTER_ANTICIPATE_OVERSHOOT = 8;

    private View view;
    private Flow flowAnimation;
    private Animation animator;
    private int iDefaultInter = INTER_ACC_DECELERATE;
    private List<Long> listStartTime = new ArrayList<>();
    private List<Long> listDuration = new ArrayList<>();
    private List<Animation> listAnimation = new ArrayList<>();

    public void setView(View v){ view = v; }
    // METHOD sets default interpolator, used when none is provided
    public void setInterpolator( int i){ iDefaultInter = i;}

    // METHOD add animation, method gets start time from previous animation and sets it
    public void addAnimation(int iType, float iStart, float iEnd, long iDuration){
    addAnimation(iType, iDefaultInter, iStart, iEnd, iDuration);
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
        switch (iType){
            case SCALE_X:
                animator = new ScaleAnimation(iStart, iEnd, 1.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case SCALE_Y:
                animator = new ScaleAnimation(1.0f, 1.0f, iStart, iEnd, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case ROTATE:
                animator = new RotateAnimation(iStart, iEnd, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                break;

            case TRANSLATE_X:
                animator = new TranslateAnimation(iStart, iEnd, 0, 0);
                break;

            case TRANSLATE_Y:
                animator = new TranslateAnimation(0, 0, iStart, iEnd);
                break;
        }

        animator.setFillAfter(true);
        animator.setDuration(iDuration);
        switch (iInterpolator){
            case INTER_CYCLE: animator.setInterpolator(new BounceInterpolator()); break;
            case INTER_LINEAR: animator.setInterpolator(new LinearInterpolator()); break;
            case INTER_BOUNCE: animator.setInterpolator(new BounceInterpolator()); break;
            case INTER_OVERSHOOT: animator.setInterpolator(new OvershootInterpolator()); break;
            case INTER_ACCELERATE: animator.setInterpolator(new AccelerateInterpolator()); break;
            case INTER_DECELERATE: animator.setInterpolator(new DecelerateInterpolator()); break;
            case INTER_ANTICIPATE: animator.setInterpolator(new AnticipateInterpolator()); break;
            case INTER_ACC_DECELERATE: animator.setInterpolator(new AccelerateDecelerateInterpolator()); break;
            case INTER_ANTICIPATE_OVERSHOOT: animator.setInterpolator(new AnticipateOvershootInterpolator()); break;
        }

        listDuration.add(iDuration);
        listStartTime.add(iStartTime);
        listAnimation.add(animator);
    }

    public void start(){
        flowAnimation = new Flow(actionCode);
        for(int i=0; i < listStartTime.size(); i++){
            flowAnimation.runDelayed(i, listStartTime.get(i));
        }
    }

    Flow.ActionCode actionCode = new Flow.ActionCode() {
        @Override public void onAction(int iAction, boolean bSuccess, int iExtra, Object data) {
            Animation anim =  listAnimation.get(iAction);
            view.setAnimation(anim);
            anim.start();
        }
    } ;

    public void stop(){
        flowAnimation.stop();
        int iCount = listAnimation.size();
        if(view != null){ view.clearAnimation(); }
        for(int i =0; i < iCount; i++){
            listAnimation.get(i).cancel();
        }
    }
}

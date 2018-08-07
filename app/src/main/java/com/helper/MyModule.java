package com.helper;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Ubaid on 24/07/2018.
 */
@Module
public class MyModule {

    @Provides
    @Singleton
    @Named("one")
    static MyExample getMyExample(){
        MyExample m1 = new MyExample();
        m1.s1 = "one";
        return m1;
    }

    @Provides
    @Singleton
    @Named("two")
    static  MyExample getMyExample2(){
        MyExample m2 = new MyExample();
        m2.s1 = "two";
        return m2;
    }

}

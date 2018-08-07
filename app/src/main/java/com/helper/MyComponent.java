package com.helper;

import javax.inject.Singleton;

import dagger.Component;
import dagger.Module;

/**
 * Created by Ubaid on 24/07/2018.
 */

@Singleton
@Component(modules = MyModule.class)
interface MyComponent {
void inject(MainActivity mainActivity);
}

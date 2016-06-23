package com.helper.lib;

import android.app.Activity;

import no.nordicsemi.android.dfu.DfuBaseService;

/**
 * Created by Ubaid on 22/06/2016.
 */
public class FirwareUpdater  extends   DfuBaseService {

    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return MainActivity.class;
    }
}

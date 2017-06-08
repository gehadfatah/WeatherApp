package com.example.godaa.sunshine.sync;

import android.accounts.Account;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.net.Authenticator;

/**
 * A bound Service that instantiates the authenticator when started
 */
public class SunshineAuthenticatorService extends Service {
    private static final String TAG = "SunshineAuthenticatore";
    private static final String ACCOUNT_TYPE = "com.example.godaa.sunshine";
    public static final String ACCOUNT_NAME = "sgodaa";
    //private Authenticator mAuthenticator;
    // Instance field that stores the authenticator object
    private SunshineAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new SunshineAuthenticator(this);

        super.onCreate();
    }


    @Override
    public void onDestroy() {
        Log.i(TAG, "Serdkjes");
    }
    public static Account GetAccount() {
        // Note: Normally the account name is set to the user's identity (username or email
        // address). However, since we aren't actually using any user accounts, it makes more sense
        // to use a generic string in this case.
        //
        // This string should *not* be localized. If the user switches locale, we would not be
        // able to locate the old account, and may erroneously register multiple accounts.
        final String accountName = ACCOUNT_NAME;
        return new Account(accountName, ACCOUNT_TYPE);
    }
    /**
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}

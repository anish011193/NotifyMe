package com.example.notifyme.model;

import android.app.Application;


public class UserClient extends Application {

    private User user = null;
    private static UserClient mInstance;

    public static synchronized UserClient getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance   = this;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}

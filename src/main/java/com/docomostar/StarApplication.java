package com.docomostar;

import com.nttdocomo.ui.IApplication;

public abstract class StarApplication extends IApplication {
    public static final int APP_STATE_ACTIVE = 1;
    public static final int APP_STATE_SUSPENDED = 2;
    public static final int STAR_STATE_STARTED = 0;
    public static final int STAR_STATE_FULLACTIVE = 1;
    public static final int STAR_STATE_SEMIACTIVE = 2;
    public static final int STAR_TYPE_FULLAPPLI = 1;
    public static final int STAR_TYPE_MINIAPPLI = 2;
    public static final int STAR_FACESTATE_FULL_NORMAL = 1;
    public static final int ACTIVATED_FROM_STARTED_STATE = 0;
    public static final int ACTIVATED_BY_NATIVE = -3;

    private volatile int appState = STAR_STATE_STARTED;

    private final StarApplicationManager manager = new StarApplicationManager(this);

    public static StarApplication getThisStarApplication() {
        IApplication app = IApplication.getCurrentApp();
        return app instanceof StarApplication starApplication ? starApplication : null;
    }

    public final StarApplicationManager getStarApplicationManager() {
        return manager;
    }

    @Override
    public void start() {
        started(getLaunchType());
        appState = STAR_STATE_FULLACTIVE;
        activated(ACTIVATED_FROM_STARTED_STATE);
    }

    @Override
    public void resume() {
        appState = STAR_STATE_FULLACTIVE;
        activated(ACTIVATED_BY_NATIVE);
    }

    public void started(int launchType) {
        started();
    }

    public void started() {
        return;
    }

    public void activated() {
        return;
    }

    public void activated(int appState) {
        activated();
    }

    public void stateChanged(StarEventObject event) {
        stateChanged();
    }

    public void stateChanged() {
        return;
    }

    public void stateChanged(boolean fullActive) {
        stateChanged();
    }

    public final int getAppType() {
        return STAR_TYPE_FULLAPPLI;
    }

    public final int getAppFaceState() {
        return STAR_FACESTATE_FULL_NORMAL;
    }

    public void suspend() {
        return;
    }

    public int getAppState() {
        return appState;
    }
}

package com.jordanfitzgibbon.heartratemonitor;

import java.util.TimerTask;

class HRMTimerTask extends TimerTask {

    MainActivity parent;

    public HRMTimerTask(MainActivity parent) {
        this.parent = parent;
    }

    @Override
    public void run() {
        parent.RunTimerTask();
    }
}
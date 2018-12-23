package com.massivecraft.factions.zcore.persist;

import com.massivecraft.factions.zcore.MPlugin;

public class SaveTask implements Runnable {
    static private boolean running = false;

    MPlugin p;

    public SaveTask(final MPlugin p) {
        this.p = p;
    }

    @Override
    public void run() {
        if (!p.getAutoSave() || SaveTask.running) { return; }
        SaveTask.running = true;
        p.preAutoSave();
        EM.saveAllToDisc();
        p.postAutoSave();
        SaveTask.running = false;
    }
}

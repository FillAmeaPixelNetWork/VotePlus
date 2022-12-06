package fap.pigeon.voteplus.tasks;

import fap.pigeon.voteplus.VotePlus;

public class VoteCdTask extends Thread {

    private final VotePlus votePlus;

    public VoteCdTask(VotePlus votePlus) {
        this.votePlus = votePlus;
    }

    @Override
    public void run() {
        try {
            sleep(votePlus.config.getInt("投票冷却时间") * 1000L);
            votePlus.voteData = null;
            votePlus.voteCdTask = null;
        } catch (Exception e) {
        }
    }

}

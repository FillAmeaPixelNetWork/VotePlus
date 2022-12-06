package fap.pigeon.voteplus.tasks;

import cn.nukkit.Server;
import cn.nukkit.utils.BossBarColor;
import cn.nukkit.utils.DummyBossBar;
import cn.nukkit.utils.TextFormat;
import fap.pigeon.voteplus.VoteData;
import fap.pigeon.voteplus.VotePlus;

import java.util.HashMap;

public class VoteTask extends Thread {

    private final VotePlus votePlus;
    private final Server server = Server.getInstance();

    public static final HashMap<String, DummyBossBar> dummyBossBars = new HashMap<>();

    public VoteTask(VotePlus votePlus) {
        this.votePlus = votePlus;
    }

    @Override
    public void run() {
        while (votePlus.voteData != null) {
            try {
                VoteData voteData = votePlus.voteData;
                voteData.minOver--;

                // show status
                if (voteData.minOver > 0) {
                    server.getOnlinePlayers().values().forEach(player -> {
                        String color = voteData.minOver % 2 == 0 ? TextFormat.RED.toString() : TextFormat.WHITE.toString();
                        float time = Math.max(0, voteData.minOver * 100f / voteData.maxOver);
                        color = color + votePlus.getVoteStatus();

                        if (dummyBossBars.containsKey(player.getName())) {
                            DummyBossBar dummyBossBar = dummyBossBars.get(player.getName());
                            dummyBossBar.setColor(BossBarColor.PURPLE);
                            dummyBossBar.setLength(time);
                            dummyBossBar.setText(color);
                        } else {
                            dummyBossBars.put(
                                    player.getName(),
                                    votePlus.creBossPar(player, color, time)
                            );
                        }
                    });

                }

                if (voteData.minOver == 0) {
                    dummyBossBars.values().forEach(dummyBossBar -> {
                        dummyBossBar.setLength(100f);
                        dummyBossBar.setColor(BossBarColor.GREEN);
                        dummyBossBar.setText(votePlus.getVoteResultMsg());
                    });
                    server.broadcastMessage(votePlus.getVoteResultMsg());
                    votePlus.overVote();
                }

                if (voteData.minOver == -5) {
                    dummyBossBars.values().forEach(DummyBossBar::destroy);
                    dummyBossBars.clear();
                    // 进入冷却倒计时
                    VotePlus.executor.execute(() -> (votePlus.voteCdTask = new VoteCdTask(votePlus)).start());
                    break;
                }

                sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

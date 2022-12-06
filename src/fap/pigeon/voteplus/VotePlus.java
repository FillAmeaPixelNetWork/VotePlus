package fap.pigeon.voteplus;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.*;
import fap.pigeon.voteplus.ecoAPI.EcoPlayer;
import fap.pigeon.voteplus.menu.VoteMenu;
import fap.pigeon.voteplus.tasks.VoteCdTask;
import fap.pigeon.voteplus.tasks.VoteTask;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class VotePlus extends PluginBase implements Listener {

    public VoteMenu menu;
    public Config config;

    public static Executor executor = Executors.newWorkStealingPool();

    public VoteData voteData;
    public VoteCdTask voteCdTask;

    public static String[] modes = new String[]{
            "§a同意", "§c反对"
    };

    @Override
    public void onEnable() {
        if (!getDataFolder().mkdirs()) {
            debug("VotePlus Enabled!");
        }

        config = new Config(getDataFolder() + "/config.yml", 2, new ConfigSection() {{
            put("投票冷却时间", 60); // sec
            put("投票结束时间", 60); // sec
            put("未投票玩家默认结果", 1); // 0=赞成 1=反对
            put("同意该投票触发指令", new ArrayList<String>() {{
                add("kill %target"); // 对被投票的玩家
            }});
            put("反对该投票触发指令", new ArrayList<String>() {{
                add("kill %target");
                add("give %player 1 1");
            }});
        }});

        this.menu = new VoteMenu(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    public Config getPlayerConfig(Player player) {
        File file = new File(getDataFolder(), "players/" + player.getName());
        return new Config(file, 2, new ConfigSection() {{
            put("money", 0);
            put("ban", false);
            put("banXboxId", "0");
            put("banTime", 0);
        }});
    }

    public void startVote(Player originator, Player target, String reason) {
        // 服务器小于两人不能进行投票
        if (getServer().getOnlinePlayers().values().size() < 3) {
            originator.sendMessage("§c无法发起投票！人数不足！");
            return;
        }

        // 可以发起投票
        if (voteData == null && voteCdTask == null) {
            // 创建投票
            voteData = new VoteData(
                    originator,
                    target,
                    reason,
                    config.getInt("投票结束时间")
            );
            // 发起人加入同意
            VotePlayer.add(originator, 0);
            // 投票倒计时
            executor.execute(() -> new VoteTask(this).start());
            originator.sendMessage("§a开始对玩家 " + target.getName() + " 进行投票操作！");
        } else {
            originator.sendMessage("§c发起失败！当前正在投票中或冷却中...");
        }

    }

    public void checkVote() {
        int mode = config.getInt("未投票玩家默认结果");
        // 没有参与投票默认投票
        getServer().getOnlinePlayers().values().stream()
                .filter(player -> VotePlayer.get(player) == null)
                .forEach(player -> {
                    VotePlayer.set(player, mode);
                    player.sendMessage("你未进行投票！默认投票为： " + modes[mode]);
                });
        voteData.result = getVoteResult();
        VotePlayer.players.clear();
    }

    public void overVote() {
        checkVote();
        disCmd();
    }

    public void disCmd() {
        String cmdK;
        if (voteData.result < 2) {
            if (voteData.result == 0) {
                cmdK = "同意";
            } else {
                cmdK = "反对";
            }
            config.getStringList(cmdK + "该投票触发指令").forEach(s -> getServer().dispatchCommand(
                    getServer().getConsoleSender(),
                    s.replaceAll("%target", "\"" + voteData.target.getName() + "\"")
            ));
        }
    }

    public int getVoteResult() {
        int win = VotePlayer.winners().size();
        int lost = VotePlayer.lost().size();
        int i;
        if (win > lost) {
            i = 0;
        } else if (win < lost) {
            i = 1;
        } else {
            i = 2;
        }
        return i;
    }

    public String getVoteResultMsg() {
        String[] strings = {
                "§a已同意该投票操作",
                "§c已反对该投票操作",
                "§b同意和反对一致，投票操作失效"
        };
        return strings[getVoteResult()];
    }

    public String getVoteStatus() {
        int win = VotePlayer.winners().size();
        int lost = VotePlayer.lost().size();

        String t = "正在对" + voteData.target.getName() + "进行投票操作！ /vote <y=同意 n=反对>";
        String reason = "发起原因： " + voteData.reason;
        String result = "同意: " + win + "                    " + " 反对: " + lost;

        return t + "\n\n" + reason + "\n" + result;
    }

    public void vote(Player player, int mode) {
        String t;
        if (voteData == null) {
            t = "§c当前没有发起投票！";
        } else {
            if (VotePlayer.get(player) == null) {
                VotePlayer.add(player, mode);
                t = "你已" + modes[mode] + "§f该投票！";
            } else {
                t = "§c你已参与这次投票了！";
            }
        }
        player.sendMessage(t);
    }

    public void cancel(Player player) {
        if (voteData != null && voteData.orig == player) {
            voteData = null;
            // 进入冷却倒计时
            VotePlus.executor.execute(() -> (voteCdTask = new VoteCdTask(this)).start());
            VoteTask.dummyBossBars.values().forEach(DummyBossBar::destroy);
            VoteTask.dummyBossBars.clear();
            player.sendMessage("§a你已取消当前投票操作！");
            return;
        }
        player.sendMessage("§c当前没有正在投票操作或你不是该投票的发起人！");
    }

    public DummyBossBar creBossPar(Player player, String text, float health) {
        DummyBossBar.Builder builder = new DummyBossBar.Builder(player);
        builder.text(text);
        builder.length(health);
        builder.color(BossBarColor.PURPLE);

        DummyBossBar build = builder.build();
        build.create();

        return build;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String t = "";
        switch (command.getName()) {
            case "vote":
                // UI
                if (args.length == 0 && sender instanceof Player) {
                    Player player = (Player) sender;
                    menu.openMenu(player);
                    return true;
                }
                // /vote <y/n>
                if (args.length == 1) {
                    Player player = (Player) sender;
                    switch (args[0]) {
                        case "y":
                        case "yes":
                            vote(player, 0);
                            break;
                        case "n":
                        case "no":
                            vote(player, 1);
                            break;
                        case "cancel":
                            cancel(player);
                            break;
                    }
                }
                // vote <player> <reason>
                if (args.length == 2 && sender instanceof Player) {
                    Player player = (Player) sender;
                    Player exact = getServer().getPlayerExact(args[0]);
                    if (exact == null) {
                        t = "目标 " + args[0] + " 不存在！";
                    } else {
                        startVote(player, exact, args[1]);
                    }
                    break;
                }
                break;
            case "ban":
                if (args.length == 3 && sender.isOp()) {
                    EcoPlayer ecoPlayer = EcoPlayer.get(args[0]);
                    if (ecoPlayer == null) {
                        sender.sendMessage("玩家不存在！");
                        return true;
                    }
                    if (ecoPlayer.setBan(Integer.parseInt(args[1]), Integer.parseInt(args[2]))) {
                        t = "已封禁该玩家！";
                    } else {
                        t = "该玩家封禁中！";
                    }
                }
                break;
            case "addmoney":
                if (args.length == 2 && sender.isOp()) {
                    EcoPlayer ecoPlayer = EcoPlayer.get(args[0]);
                    if (ecoPlayer == null) {
                        sender.sendMessage("玩家： " + args[0] + " 不存在！");
                        return true;
                    }
                    ecoPlayer.add(Integer.parseInt(args[1]));
                    t = "增加玩家： " + args[0] + " 金币: " + args[1];
                } else {
                    t = "权限不足！";
                }
                break;
            case "reducemoney":
                if (args.length == 2 && sender.isOp()) {
                    EcoPlayer ecoPlayer = EcoPlayer.get(args[0]);
                    if (ecoPlayer == null) {
                        sender.sendMessage("玩家： " + args[0] + " 不存在！");
                        return true;
                    }
                    ecoPlayer.reduce(Integer.parseInt(args[1]));
                    t = "减少玩家： " + args[0] + " 金币: " + args[1];
                }
                break;
        }

        if (!t.isEmpty()) {
            sender.sendMessage(t);
        }

        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        EcoPlayer.set(event.getPlayer().getName(), getPlayerConfig(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        EcoPlayer ecoPlayer = EcoPlayer.get(player.getName());
        if (ecoPlayer != null) {
            ecoPlayer.save();
        }
        EcoPlayer.remove(player.getName());
    }

    public static void debug(String debug) {
        MainLogger.getLogger().notice(debug);
    }
}

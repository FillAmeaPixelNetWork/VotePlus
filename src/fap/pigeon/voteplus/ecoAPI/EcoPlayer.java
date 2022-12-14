package fap.pigeon.voteplus.ecoAPI;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.LoginChainData;
import fap.pigeon.voteplus.VotePlus;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class EcoPlayer {

    public static HashMap<String, EcoPlayer> players = new HashMap<>();

    public static void set(String n, Config config) {
        Player exact = Server.getInstance().getPlayerExact(
                n.replaceAll("_", " ")
        );
        if (exact == null)
            return;
        players.put(n, new EcoPlayer(exact, config));
    }

    public static EcoPlayer get(String n) {
        Player playerExact = Server.getInstance().getPlayerExact(
                n.replaceAll("_", " ")
        );
        if (playerExact == null) {
            return null;
        }
        return players.getOrDefault(playerExact.getName(), null);
    }

    public static boolean remove(String n) {
        EcoPlayer ecoPlayer = get(n);
        if (ecoPlayer != null) {
            players.remove(n);
            return true;
        }
        return false;
    }

    protected final Player player;
    private final Config config;
    private final ConfigSection configSection;
    private final LoginChainData loginChainData;

    public EcoPlayer(Player player, Config config) {
        this.player = player;
        this.config = config;
        this.configSection = config.getRootSection();
        this.loginChainData = player.getLoginChainData();
        checkBan();
    }

    public void set(int money) {
        configSection.set("money", money);
    }

    public int get() {
        return configSection.getInt("money", 0);
    }

    public void reduce(int money) {
        set(Math.max(0, get() - money));
    }

    public void add(int money) {
        set(get() + money);
    }

    public void checkBan() {
        boolean ban = configSection.getBoolean("ban");

        if (ban) {
            // ????????????xboxid
            if (loginChainData.isXboxAuthed() &&
                    loginChainData.getXUID().equals(configSection.getString("banXboxId"))) {
                VotePlus.debug(player.getName() + "??????XboxId????????????????????????...");
            } else {
                VotePlus.debug(player.getName() + "?????????XboxId???????????????xboxid??????");
            }

            long now = new Date().getTime();
            long after = config.getLong("banTime");

            ban = now < after;

            if (ban) {
                player.kick("????????????????????????????????????" + getDataFormat(after));
            } else {
                configSection.set("ban", false);
                configSection.set("banTime", 0);
                save();
                VotePlus.debug(player.getName() + "???????????????????????? ??????????????????");
            }
        }

    }

    public boolean setBan(int day, int sec) {
        if (configSection.getBoolean("ban")) {
            return false;
        }
        configSection.set("ban", true);
        configSection.set("banXboxId", loginChainData.getXUID());
        configSection.set("banTime", getDateAfter(day, sec));
        save();
        checkBan();
        return true;
    }

    public Long getDateAfter(int day, int sec) {
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        now.set(Calendar.DATE, now.get(Calendar.DATE) + day);
        now.set(Calendar.SECOND, now.get(Calendar.SECOND) + sec);
        return now.getTime().getTime();
    }

    public String getDataFormat(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy???MM???dd??? HH???mm???ss???");
        return format.format(new Date(time));
    }

    public void save() {
        config.setAll(configSection);
        config.save();
    }

}

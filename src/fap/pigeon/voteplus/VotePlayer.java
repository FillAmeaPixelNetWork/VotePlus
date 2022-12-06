package fap.pigeon.voteplus;

import cn.nukkit.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VotePlayer {

    public static List<VotePlayer> players = new ArrayList<>();

    public static VotePlayer get(String n) {
        return players.stream()
                .filter(mtp -> n.equals(mtp.getName()))
                .findFirst()
                .orElse(null);
    }

    public static VotePlayer get(Player player) {
        return get(player.getName());
    }

    public static void set(Player player, int mode) {
        players.add(new VotePlayer(player, mode));
    }

    public static boolean add(Player player, int mode) {
        if (get(player.getName()) == null) {
            set(player, mode);
            return true;
        }
        return false;
    }

    public static List<VotePlayer> winners() {
        return players.stream().filter(vp -> vp.mode == 0).collect(Collectors.toList());
    }

    public static List<VotePlayer> lost() {
        return players.stream().filter(vp -> vp.mode == 1).collect(Collectors.toList());
    }

    private final Player player;
    private final int mode; // 0=赞成/1=反对

    public VotePlayer(Player player, int mode) {
        this.player = player;
        this.mode = mode;
    }

    public String getName() {
        return player.getName();
    }

}

package fap.pigeon.voteplus;

import cn.nukkit.Player;

public class VoteData {

    public int minOver;
    public int maxOver;
    public Player orig; // 发起人
    public Player target; // 被发起
    public String reason; // 发起原因
    public int result = 0; // 0=同意方 1=反对方 2=平局

    public VoteData(Player orig, Player target, String reason, int maxOver) {
        this.orig = orig;
        this.target = target;
        this.reason = reason;
        this.minOver = maxOver;
        this.maxOver = maxOver;
    }

}

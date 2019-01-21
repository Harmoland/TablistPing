package top.sunbread.TablistPing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.packet.ScoreboardDisplay;
import net.md_5.bungee.protocol.packet.ScoreboardObjective;
import net.md_5.bungee.protocol.packet.ScoreboardObjective.HealthDisplay;
import net.md_5.bungee.protocol.packet.ScoreboardScore;

public final class TablistPing extends Plugin implements Listener, Runnable {
    
    public static final String OBJECTIVE_NAME = "! !TP! !";
    
    private Set<UUID> targets = new HashSet<>();
    private Map<UUID, Integer> lastPing = new HashMap<>();
    
    @Override
    public synchronized void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getScheduler().schedule(this, this, 0, 100, TimeUnit.MILLISECONDS);
        for(ProxiedPlayer target : getProxy().getPlayers()) {
            initScoreboard(target);
            displayScoreboard(target);
            for(ProxiedPlayer player : getProxy().getPlayers())
                updateScoreboard(player, player.getPing(), target);
            targets.add(target.getUniqueId());
        }
    }
    
    @Override
    public synchronized void onDisable() {
        getProxy().getPluginManager().unregisterListeners(this);
        getProxy().getScheduler().cancel(this);
        for(UUID target : targets)
            clearScoreboard(getProxy().getPlayer(target));
        targets.clear();
        lastPing.clear();
    }
    
    @EventHandler
    public synchronized void onServerSwitch(ServerSwitchEvent event) {
        if(!targets.contains(event.getPlayer().getUniqueId())) {
            initScoreboard(event.getPlayer());
            displayScoreboard(event.getPlayer());
            for(ProxiedPlayer player : getProxy().getPlayers())
                updateScoreboard(player, player.getPing(), event.getPlayer());
            targets.add(event.getPlayer().getUniqueId());
        }
    }
    
    @EventHandler
    public synchronized void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if(targets.contains(event.getPlayer().getUniqueId())) {
            clearScoreboard(event.getPlayer());
            targets.remove(event.getPlayer().getUniqueId());
        }
        lastPing.remove(event.getPlayer().getUniqueId());
    }
    
    @Override
    public synchronized void run() {
        for(UUID target : targets)
            displayScoreboard(getProxy().getPlayer(target));
        /* The "getProxy().getPlayers()" below is thread safe */
        for(ProxiedPlayer player : getProxy().getPlayers()) {
            int ping = player.getPing();
            if(!lastPing.containsKey(player.getUniqueId()) ||
               lastPing.get(player.getUniqueId()) != ping)
                for(UUID target : targets)
                    updateScoreboard(player, ping, getProxy().getPlayer(target));
            lastPing.put(player.getUniqueId(), ping);
        }
    }
    
    private void initScoreboard(ProxiedPlayer target) {
        ScoreboardObjective objectiveCreatePacket = new ScoreboardObjective();
        objectiveCreatePacket.setName(OBJECTIVE_NAME);
        objectiveCreatePacket.setAction((byte)0); // 0: create
        objectiveCreatePacket.setValue("");
        objectiveCreatePacket.setType(HealthDisplay.INTEGER);
        target.unsafe().sendPacket(objectiveCreatePacket);
    }
    
    private void displayScoreboard(ProxiedPlayer target) {
        ScoreboardDisplay scoreboardDisplayPacket = new ScoreboardDisplay();
        scoreboardDisplayPacket.setPosition((byte)0); // 0: list
        scoreboardDisplayPacket.setName(OBJECTIVE_NAME);
        target.unsafe().sendPacket(scoreboardDisplayPacket);
    }
    
    private void updateScoreboard(ProxiedPlayer player, int score, ProxiedPlayer target) {
        ScoreboardScore updateScorePacket = new ScoreboardScore();
        updateScorePacket.setItemName(player.getName());
        updateScorePacket.setAction((byte)0); // 0: create/update
        updateScorePacket.setScoreName(OBJECTIVE_NAME);
        updateScorePacket.setValue(score);
        target.unsafe().sendPacket(updateScorePacket);
    }
    
    private void clearScoreboard(ProxiedPlayer target) {
        ScoreboardDisplay clearDisplayPacket = new ScoreboardDisplay();
        clearDisplayPacket.setPosition((byte)0); // 0: list
        clearDisplayPacket.setName("");
        ScoreboardObjective objectiveRemovePacket = new ScoreboardObjective();
        objectiveRemovePacket.setName(OBJECTIVE_NAME);
        objectiveRemovePacket.setAction((byte)1); // 1: remove
        target.unsafe().sendPacket(clearDisplayPacket);
        target.unsafe().sendPacket(objectiveRemovePacket);
    }
    
}

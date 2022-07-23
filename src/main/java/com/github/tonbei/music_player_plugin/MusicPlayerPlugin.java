package com.github.tonbei.music_player_plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class MusicPlayerPlugin extends JavaPlugin {

    public static final Gson GSON = new GsonBuilder().enableComplexMapKeySerialization().disableHtmlEscaping().create();

    public static final String CHANNEL = "musicplayer:multi";

    private PacketManager packetManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        //this.getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        packetManager = new PacketManager(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //if (!(sender instanceof Player)) return false;
        if (args.length <= 0) return false;

        Map<String, String> packetData = new HashMap<>();

        if (args[0].equalsIgnoreCase("play")) {
            packetData.put("playStatus", "PLAY");
            if (args.length > 1)
                packetData.put("URL", args[1]);
            if (args.length > 2 && args[2].equalsIgnoreCase("loop"))
                packetData.put("LOOP", args[2]);
        } else if (args[0].equalsIgnoreCase("stop")) {
            packetData.put("playStatus", "STOP");
        }

        getLogger().warning(packetData.toString());

        Bukkit.getOnlinePlayers().forEach(player -> packetManager.sendPacket(packetData, player));
        //packetManager.sendPacket(packetData, (Player) sender);
        return true;
    }
}

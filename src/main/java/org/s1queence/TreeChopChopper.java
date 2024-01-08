package org.s1queence;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.s1queence.plugin.libs.YamlDocument;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static org.s1queence.api.S1TextUtils.consoleLog;
import static org.s1queence.api.S1TextUtils.getConvertedTextFromConfig;

public final class TreeChopChopper extends JavaPlugin implements CommandExecutor {
    private YamlDocument config;

    @Override
    public void onEnable() {
        try {
            config = YamlDocument.create(new File(getDataFolder(), "config.yml"), Objects.requireNonNull(getResource("config.yml")));
        } catch (IOException ignored) {

        }

        Objects.requireNonNull(getServer().getPluginCommand("treechopchopper")).setExecutor(this);
        getServer().getPluginManager().registerEvents(new ChopListener(this), this);
        consoleLog(getConvertedTextFromConfig(config, "onEnable_msg", this.getName()), this);

    }

    @Override
    public void onDisable() {
        consoleLog(getConvertedTextFromConfig(config, "onDisable_msg", this.getName()), this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) return false;
        if (!args[0].equalsIgnoreCase("reload")) return false;

        try {
            File optionsCfgFile = new File(getDataFolder(), "config.yml");
            if (!optionsCfgFile.exists()) config = YamlDocument.create(new File(getDataFolder(), "config.yml"), Objects.requireNonNull(getResource("config.yml")));
            config.reload();
        } catch (IOException ignored) {

        }

        String reloadMsg = getConvertedTextFromConfig(config, "onReload_msg", this.getName());
        if (sender instanceof Player) sender.sendMessage(reloadMsg);
        consoleLog(reloadMsg, this);

        return true;
    }

    public YamlDocument getPluginConfig() {
        return config;
    }

}

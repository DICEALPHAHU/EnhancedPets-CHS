package dev.asteriamc.enhancedpets.commands;

import dev.asteriamc.enhancedpets.Enhancedpets;
import dev.asteriamc.enhancedpets.gui.PetManagerGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Bukkit; 
import org.bukkit.OfflinePlayer;

public class PetCommand implements CommandExecutor {
    private final Enhancedpets plugin;
    private final PetManagerGUI guiManager;

    public PetCommand(Enhancedpets plugin, PetManagerGUI guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("petadmin")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以执行这个命令。");
                return true;
            }
            if (!player.hasPermission("enhancedpets.admin")) {
                player.sendMessage(ChatColor.RED + "您没有权限执行此命令");
                return true;
            }
            if (args.length != 1) {
                player.sendMessage(ChatColor.RED + "用法: /" + label + " <玩家名>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.isOnline() && !target.hasPlayedBefore()) {
                player.sendMessage(ChatColor.RED + "玩家 '" + ChatColor.YELLOW + args[0] + ChatColor.RED + "' 不存在。");
                return true;
            }
            
            if (!plugin.getPetManager().isOwnerLoaded(target.getUniqueId())) {
                plugin.getPetManager().loadPetsForPlayer(target.getUniqueId());
            }
            guiManager.setViewerOwnerOverride(player.getUniqueId(), target.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> guiManager.openMainMenu(player), 3L);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("enhancedpets.reload")) {
                sender.sendMessage(ChatColor.RED + "你没有权限重新加载这个插件。");
                return true;
            } else {
                this.plugin.reloadPluginConfig(sender);
                return true;
            }
        } else if (args.length == 0) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("enhancedpets.use")) {
                    player.sendMessage(ChatColor.RED + "你没有权限使用这个命令。");
                    return true;
                } else {
                    this.guiManager.clearViewerOwnerOverride(player.getUniqueId());
                    this.guiManager.openMainMenu(player);
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "宠物GUI节目仅限于玩家开启. 使用 '/" + label + " reload' 重新加载。");
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "无效用法。使用 '/" + label + "' 来打开GUI菜单或 '/" + label + " reload' 重新加载配置文件。");
            return true;
        }
    }
}


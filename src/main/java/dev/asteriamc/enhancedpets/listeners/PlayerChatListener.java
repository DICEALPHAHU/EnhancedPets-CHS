package dev.asteriamc.enhancedpets.listeners;

import dev.asteriamc.enhancedpets.Enhancedpets;
import dev.asteriamc.enhancedpets.data.PetData;
import dev.asteriamc.enhancedpets.gui.PetGUIListener;
import dev.asteriamc.enhancedpets.gui.PetManagerGUI;
import dev.asteriamc.enhancedpets.manager.PetManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerChatListener implements Listener {
    private final Enhancedpets plugin;
    private final PetManager petManager;
    private final PetManagerGUI guiManager;
    private final PetGUIListener guiListener;

    public PlayerChatListener(Enhancedpets plugin, PetManager petManager, PetManagerGUI guiManager, PetGUIListener guiListener) {
        this.plugin = plugin;
        this.petManager = petManager;
        this.guiManager = guiManager;
        this.guiListener = guiListener;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String input = event.getMessage();


        Map<UUID, UUID> renameInputMap = this.guiListener.getAwaitingRenameInputMap();
        if (renameInputMap.containsKey(playerUUID)) {
            event.setCancelled(true);
            UUID petContextUUID = renameInputMap.remove(playerUUID);
            handleRenameInput(player, input, petContextUUID);
            return;
        }


        Map<UUID, UUID> friendlyInputMap = this.guiListener.getAwaitingFriendlyInputMap();
        if (friendlyInputMap.containsKey(playerUUID)) {
            event.setCancelled(true);
            UUID petContextUUID = friendlyInputMap.remove(playerUUID);
            handleSingleFriendlyInput(player, input, petContextUUID);
            return;
        }


        Map<UUID, Boolean> batchFriendlyInputMap = this.guiListener.getAwaitingBatchFriendlyInputMap();
        if (batchFriendlyInputMap.containsKey(playerUUID)) {
            event.setCancelled(true);
            batchFriendlyInputMap.remove(playerUUID);
            handleBatchFriendlyInput(player, input);
        }
    }

    private void handleRenameInput(Player player, String input, UUID petContextUUID) {
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "取消宠物重命名。");
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.guiManager.openPetMenu(player, petContextUUID));
            return;
        }

        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            PetData petData = this.petManager.getPetData(petContextUUID);
            if (petData == null) {
                player.sendMessage(ChatColor.RED + "错误：宠物已不存在。");
                this.guiManager.openMainMenu(player);
                return;
            }

            if (input.matches("^[a-zA-Z0-9_-]+$")) {
                String oldName = petData.getDisplayName();
                petData.setDisplayName(input);
                Entity petEntity = Bukkit.getEntity(petContextUUID);
                if (petEntity instanceof LivingEntity) {
                    petEntity.setCustomName(ChatColor.translateAlternateColorCodes('&', input));
                }
                this.petManager.updatePetData(petData);
                player.sendMessage(ChatColor.GREEN + "Renamed " + ChatColor.AQUA + oldName + ChatColor.GREEN + " to " + ChatColor.AQUA + input + ChatColor.GREEN + ".");
            } else {
                player.sendMessage(ChatColor.RED + "使用了无效的名称字符。昵称未变化。");
            }
            this.guiManager.openPetMenu(player, petContextUUID);
        });
    }

    private void handleSingleFriendlyInput(Player player, String input, UUID petContextUUID) {
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "取消添加友好玩家。");

            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.guiManager.openFriendlyPlayerMenu(player, petContextUUID, 0));
            return;
        }

        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            PetData petData = this.petManager.getPetData(petContextUUID);
            if (petData == null) {
                player.sendMessage(ChatColor.RED + "错误：宠物数据在等待输入时丢失。");
                this.guiManager.openMainMenu(player);
                return;
            }

            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(input);
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                player.sendMessage(ChatColor.RED + "Player '" + ChatColor.YELLOW + input + ChatColor.RED + "' not found.");
                this.guiListener.getAwaitingFriendlyInputMap().put(player.getUniqueId(), petContextUUID);
                player.sendMessage(ChatColor.GOLD + "请重试，或者键入 'cancel'.");
                return;
            }

            UUID targetUUID = targetPlayer.getUniqueId();
            String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : input;
            if (targetUUID.equals(petData.getOwnerUUID())) {
                player.sendMessage(ChatColor.YELLOW + "主人对他们的宠物总是很友好。");
            } else if (petData.isFriendlyPlayer(targetUUID)) {
                player.sendMessage(ChatColor.YELLOW + targetName + " 已经加入到友好名单。");
            } else {
                petData.addFriendlyPlayer(targetUUID);
                this.petManager.updatePetData(petData);
                player.sendMessage(ChatColor.GREEN + "已添加 " + ChatColor.YELLOW + targetName + ChatColor.GREEN + " to " + ChatColor.AQUA + petData.getDisplayName() + " 的友好名单");
            }

            this.guiManager.openFriendlyPlayerMenu(player, petContextUUID, 0);
        });
    }

    private void handleBatchFriendlyInput(Player player, String input) {
        Set<UUID> selectedPets = this.guiManager.getBatchActionsGUI().getPlayerSelections().get(player.getUniqueId());
        if (selectedPets == null || selectedPets.isEmpty()) {
            player.sendMessage(ChatColor.RED + "错误：宠物选择已取消。");
            return;
        }

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "取消添加友好玩家。");
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.guiManager.openBatchFriendlyPlayerMenu(player, selectedPets, 0));
            return;
        }

        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(input);
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                player.sendMessage(ChatColor.RED + "玩家的 " + ChatColor.YELLOW + input + ChatColor.RED + " 没有找到");
                this.guiListener.getAwaitingBatchFriendlyInputMap().put(player.getUniqueId(), true);
                player.sendMessage(ChatColor.GOLD + "请重试，或者键入 'cancel'.");
                return;
            }

            UUID targetUUID = targetPlayer.getUniqueId();
            String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : input;
            int addCount = 0;
            for (UUID petUUID : selectedPets) {
                PetData petData = this.petManager.getPetData(petUUID);
                if (petData != null && !petData.isFriendlyPlayer(targetUUID) && !petData.getOwnerUUID().equals(targetUUID)) {
                    petData.addFriendlyPlayer(targetUUID);
                    addCount++;
                }
            }
            java.util.List<PetData> toSave = selectedPets.stream()
                    .map(this.petManager::getPetData)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());
            this.petManager.saveAllPetData(toSave);
            player.sendMessage(ChatColor.GREEN + "已添加 " + ChatColor.YELLOW + targetName + ChatColor.GREEN + " 到 " + addCount + " 宠物的友好名单。");
            this.guiManager.openBatchFriendlyPlayerMenu(player, selectedPets, 0);
        });
    }
}



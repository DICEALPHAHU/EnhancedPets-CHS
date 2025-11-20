package dev.asteriamc.enhancedpets.gui;

import dev.asteriamc.enhancedpets.Enhancedpets;
import dev.asteriamc.enhancedpets.data.BehaviorMode;
import dev.asteriamc.enhancedpets.data.PetData;
import dev.asteriamc.enhancedpets.manager.PetManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public class PetManagerGUI {
    public static final NamespacedKey PET_UUID_KEY = new NamespacedKey(Enhancedpets.getInstance(), "pet_uuid");
    public static final NamespacedKey ACTION_KEY = new NamespacedKey(Enhancedpets.getInstance(), "gui_action");
    public static final NamespacedKey TARGET_PLAYER_UUID_KEY = new NamespacedKey(Enhancedpets.getInstance(), "target_player_uuid");
    public static final String MAIN_MENU_TITLE = ChatColor.DARK_AQUA + "Your Enhanced Pets";
    public static final String PET_MENU_TITLE_PREFIX = ChatColor.DARK_AQUA + "Manage Pet: ";
    public static final String FRIENDLY_MENU_TITLE_PREFIX = ChatColor.DARK_AQUA + "Friendly Players: ";
    public static final NamespacedKey PAGE_KEY = new NamespacedKey(Enhancedpets.getInstance(), "gui_page");
    
    public static final NamespacedKey COLOR_KEY = new NamespacedKey(Enhancedpets.getInstance(), "display_color");

    private final Enhancedpets plugin;
    private final PetManager petManager;
    private final BatchActionsGUI batchActionsGUI;
    private final Map<UUID, UUID> viewerOwnerOverride = new java.util.concurrent.ConcurrentHashMap<>();

    public PetManagerGUI(Enhancedpets plugin) {
        this.plugin = plugin;
        this.petManager = plugin.getPetManager();
        this.batchActionsGUI = new BatchActionsGUI(plugin, this);
    }

    public static Integer extractPetIdFromName(String name) {
        int lastHashIndex = name.lastIndexOf(" #");
        if (lastHashIndex != -1 && lastHashIndex < name.length() - 2) {
            String numberPart = name.substring(lastHashIndex + 2);
            try {
                return Integer.parseInt(numberPart);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public BatchActionsGUI getBatchActionsGUI() {
        return this.batchActionsGUI;
    }

    public void openMainMenu(Player player) {
        openMainMenu(player, 0);
    }

    public void setViewerOwnerOverride(UUID viewer, UUID owner) { 
        if (viewer != null && owner != null) viewerOwnerOverride.put(viewer, owner);
    }
    public void clearViewerOwnerOverride(UUID viewer) { 
        if (viewer != null) viewerOwnerOverride.remove(viewer);
    }
    public UUID getViewerOwnerOverride(UUID viewer) { 
        return viewerOwnerOverride.get(viewer);
    }
    public UUID getEffectiveOwner(Player viewer) { 
        UUID o = getViewerOwnerOverride(viewer.getUniqueId());
        return o != null ? o : viewer.getUniqueId();
    }


    public void openMainMenu(Player player, int page) {
        UUID effectiveOwner = getEffectiveOwner(player); 
        List<PetData> pets = this.petManager.getPetsOwnedBy(effectiveOwner);

        boolean didUpdate = false;
        for (PetData petData : pets) {
            Entity petEntity = Bukkit.getEntity(petData.getPetUUID());
            if (petEntity != null && petEntity.isValid() && petEntity.getCustomName() != null) {
                String gameName = ChatColor.stripColor(petEntity.getCustomName());
                if (!gameName.isEmpty() && !gameName.equals(petData.getDisplayName())) {
                    petData.setDisplayName(gameName);
                    didUpdate = true;
                }
            }
        }
        if (didUpdate) {
            this.petManager.saveAllPetData(pets);
        }

        pets.sort(Comparator.comparing(PetData::isFavorite).reversed()
                .thenComparing(p -> p.getEntityType().name())
                .thenComparing((p1, p2) -> {
                    String name1 = p1.getDisplayName();
                    String name2 = p2.getDisplayName();
                    Integer id1 = extractPetIdFromName(name1);
                    Integer id2 = extractPetIdFromName(name2);
                    if (id1 != null && id2 != null) {
                        return Integer.compare(id1, id2);
                    }
                    return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                }));

        final int petsPerPage = 45;
        final int totalPets = pets.size();

        if (totalPets <= petsPerPage && page == 0) {
            int rows = (int) Math.ceil((double) totalPets / 9.0);
            int invSize = Math.max(27, (rows + 1) * 9);

            Inventory gui = Bukkit.createInventory(player, invSize, MAIN_MENU_TITLE);

            if (pets.isEmpty()) {
                gui.setItem(13, this.createItem(Material.BARRIER, ChatColor.RED + "未找到宠物",
                        Collections.singletonList(ChatColor.GRAY + "驯服一些狼或者猫！")));
            } else {
                for (int i = 0; i < pets.size(); i++) {
                    gui.setItem(i, this.createPetItem(pets.get(i)));
                }

                if (effectiveOwner.equals(player.getUniqueId())) {
                    gui.setItem(invSize - 8, createActionButton(Material.COMPASS, ChatColor.AQUA + "扫描我拥有的宠物", "scan_for_pets", null,
                            Arrays.asList(ChatColor.GRAY + "扫描已加载区域中您未注册的宠物。", "", ChatColor.YELLOW + "点击扫描并同步。")));
                }
                gui.setItem(invSize - 5, this.createItem(Material.PAPER, ChatColor.AQUA + "页 1 / 1", Collections.singletonList(ChatColor.GRAY + "所有宠物: " + totalPets)));
                gui.setItem(invSize - 2, createActionButton(Material.HOPPER, ChatColor.GOLD + "批量操作", "batch_actions", null,
                        Collections.singletonList(ChatColor.GRAY + "批量管理宠物。")));
            }
            player.openInventory(gui);
        } else {
            int totalPages = (int) Math.ceil((double) totalPets / petsPerPage);
            page = Math.max(0, Math.min(page, totalPages - 1));
            int invSize = 54;

            Inventory gui = Bukkit.createInventory(player, invSize, MAIN_MENU_TITLE);
            int startIndex = page * petsPerPage;
            int endIndex = Math.min(startIndex + petsPerPage, totalPets);
            List<PetData> petsToShow = pets.subList(startIndex, endIndex);

            for (int i = 0; i < petsToShow.size(); i++) {
                gui.setItem(i, this.createPetItem(petsToShow.get(i)));
            }

            if (page > 0) {
			gui.setItem(invSize - 9, this.createNavigationButton(Material.ARROW, ChatColor.GREEN + "上一页", "main_page", page - 1, null));
            }
            if (endIndex < totalPets) {
                gui.setItem(invSize - 1, this.createNavigationButton(Material.ARROW, ChatColor.GREEN + "下一页", "main_page", page + 1, null));
            }
            if (effectiveOwner.equals(player.getUniqueId())) {
                gui.setItem(invSize - 8, createActionButton(Material.COMPASS, ChatColor.AQUA + "扫描我拥有的宠物", "scan_for_pets", null,
                        Arrays.asList(ChatColor.GRAY + "扫描已加载区域中您未注册的宠物。", "", ChatColor.YELLOW + "点击扫描并同步。")));
            }
            gui.setItem(invSize - 5, this.createItem(Material.PAPER, ChatColor.AQUA + "页 " + (page + 1) + " / " + totalPages,
                    Collections.singletonList(ChatColor.GRAY + "所有宠物: " + totalPets)));
            gui.setItem(invSize - 2, createActionButton(Material.HOPPER, ChatColor.GOLD + "批量操作", "batch_actions", null,
                    Collections.singletonList(ChatColor.GRAY + "批量管理宠物。")));

            player.openInventory(gui);
        }
    }

    public void openBatchManagementMenu(Player player, Set<UUID> selectedPetUUIDs) {
        if (selectedPetUUIDs == null || selectedPetUUIDs.isEmpty()) {
            player.sendMessage(ChatColor.RED + "您尚未选择任何宠物进行管理操作。");
            this.openMainMenu(player);
            return;
        }

        List<PetData> selectedPets = selectedPetUUIDs.stream()
                .map(petManager::getPetData)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String title = ChatColor.DARK_AQUA + "批量管理 " + selectedPets.size() + " 宠物";
        Inventory gui = Bukkit.createInventory(player, 54, title);

        long favoriteCount = selectedPets.stream().filter(PetData::isFavorite).count();
        boolean allFavorites = favoriteCount == selectedPets.size();
        boolean anyFavorites = favoriteCount > 0;

        BehaviorMode commonMode = selectedPets.stream().map(PetData::getMode).distinct().count() == 1
                ? selectedPets.get(0).getMode() : BehaviorMode.BATCH;

        List<Entity> petEntities = selectedPets.stream().map(p -> Bukkit.getEntity(p.getPetUUID())).filter(Objects::nonNull).toList();
        boolean allCanSit = !petEntities.isEmpty() && petEntities.stream().allMatch(e -> e instanceof Sittable);
        long sittingCount = petEntities.stream().filter(e -> e instanceof Sittable s && s.isSitting()).count();
        boolean allSitting = allCanSit && sittingCount == petEntities.size();
        boolean anySitting = allCanSit && sittingCount > 0;

        long protectedCount = selectedPets.stream().filter(PetData::isProtectedFromPlayers).count();
        boolean allProtected = protectedCount == selectedPets.size();
        boolean anyProtected = protectedCount > 0;

        PetData batchData = new PetData(null, player.getUniqueId(), null, "Batch");
        batchData.setMode(commonMode);

        String favoriteDisplayName = (anyFavorites ? ChatColor.GOLD + "★ " : "") + ChatColor.YELLOW + ChatColor.BOLD + "管理 " + selectedPets.size() + " 宠物";
        gui.setItem(4, this.createActionButton(Material.HOPPER, favoriteDisplayName, "batch_toggle_favorite", null,
                Arrays.asList(
                        ChatColor.GRAY + "已选择: " + ChatColor.WHITE + selectedPets.size() + " 宠物",
                        "",
                        allFavorites ? ChatColor.YELLOW + "点击将所有标记的从最爱中移除" : ChatColor.GREEN + "点击标记所有为最爱"
                )
        ));

        List<UUID> babyUUIDs = selectedPetUUIDs.stream()
                .map(Bukkit::getEntity)
                .filter(e -> e instanceof Ageable a && !a.isAdult())
                .map(Entity::getUniqueId)
                .toList();

        if (!babyUUIDs.isEmpty()) {
            long pausedCount = babyUUIDs.stream()
                    .map(petManager::getPetData)
                    .filter(Objects::nonNull)
                    .filter(PetData::isGrowthPaused)
                    .count();
            int totalBabies = babyUUIDs.size();

            String status;
            if (pausedCount == totalBabies) {
                status = "全部暂停";
            } else if (pausedCount > 0) {
                status = "一部分暂停";
            } else {
                status = "都在成长";
            }

            Material mat = pausedCount == totalBabies ? Material.GOLDEN_CARROT : Material.CARROT;
            gui.setItem(26, createActionButton(
                    mat,
                    (pausedCount == totalBabies ? ChatColor.GREEN : ChatColor.YELLOW) + "切换成长状态 （幼宠）",
                    "batch_toggle_growth_pause",
                    null,
                    List.of(
                            ChatColor.GRAY + "当前: " + status,
                            ChatColor.GRAY + "仅影响选择的幼宠.",
                            "",
                            ChatColor.YELLOW + "点击切换所有幼崽"
                    )
            ));
        }

        gui.setItem(11, this.createModeButton(Material.FEATHER, "设置友善状态", BehaviorMode.PASSIVE, batchData));
        gui.setItem(13, this.createModeButton(Material.IRON_SWORD, "设置中立状态", BehaviorMode.NEUTRAL, batchData));
        gui.setItem(15, this.createModeButton(Material.DIAMOND_SWORD, "设置敌对状态", BehaviorMode.AGGRESSIVE, batchData));

        if (allCanSit) {
            String sitStandName = allSitting ? ChatColor.GREEN + "让宠物站立" : ChatColor.YELLOW + "让宠物坐下";
            String sitStandStatus = "当前: " + (allSitting ? "全部坐下" : anySitting ? "Mixed" : "全部站立");
            gui.setItem(20, this.createActionButton(allSitting ? Material.ARMOR_STAND : Material.SADDLE, sitStandName, "batch_toggle_sit", null, Collections.singletonList(ChatColor.GRAY + sitStandStatus)));
        }

        gui.setItem(22, this.createActionButton(Material.ENDER_PEARL, ChatColor.LIGHT_PURPLE + "传送宠物到你的位置", "batch_teleport", null, Collections.singletonList(ChatColor.GRAY + "召唤所有已选择宠物。")));
        gui.setItem(24, this.createActionButton(Material.MILK_BUCKET, ChatColor.AQUA + "镇静宠物", "batch_calm", null, Collections.singletonList(ChatColor.GRAY + "Clears targets for all selected pets.")));

        String protName = allProtected
                ? ChatColor.YELLOW + "禁用互不攻击状态"
                : ChatColor.GREEN + "启用互不攻击状态";
        String protStatus = "当前: " + (allProtected ? "全部保护" : anyProtected ? "部分保护" : "全部未保护");
        gui.setItem(28, this.createActionButton(
                Material.SHIELD,
                protName,
                "batch_toggle_protection",
                null,
                Arrays.asList(ChatColor.GRAY + protStatus, "", ChatColor.GRAY + "玩家无法伤害宠物,", ChatColor.GRAY + "并且它们不会攻击玩家。")
        ));

	gui.setItem(30, this.createActionButton(Material.PLAYER_HEAD, ChatColor.GREEN + "管理友好玩家", "batch_manage_friendly", null, Collections.singletonList(ChatColor.GRAY + "管理所有的友好玩家。")));
        gui.setItem(32, this.createActionButton(Material.LEAD, ChatColor.GOLD + "赠予宠物", "batch_open_transfer", null, Arrays.asList(ChatColor.GRAY + "将所选宠物给某个玩家", ChatColor.YELLOW + "⚠ 该操作不可逆!")));

        EntityType type = selectedPets.get(0).getEntityType();
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta meta = backButton.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "返回宠物选择");
        meta.getPersistentDataContainer().set(BatchActionsGUI.BATCH_ACTION_KEY, PersistentDataType.STRING, "open_pet_select");
        meta.getPersistentDataContainer().set(BatchActionsGUI.PET_TYPE_KEY, PersistentDataType.STRING, type.name());
        backButton.setItemMeta(meta);
        gui.setItem(45, backButton);

        gui.setItem(53, this.createActionButton(Material.BARRIER, ChatColor.RED + "放归宠物", "batch_free_pet_prompt", null, Arrays.asList(ChatColor.DARK_RED + "" + ChatColor.BOLD + "警告:", ChatColor.RED + "这将是永久性的！")));

        player.openInventory(gui);
    }

    public void openBatchConfirmFreeMenu(Player player, Set<UUID> selectedPetUUIDs) {
        String title = ChatColor.DARK_RED + "确认放归: " + selectedPetUUIDs.size() + " 宠物";
        Inventory gui = Bukkit.createInventory(player, 27, title);

        gui.setItem(13, this.createItem(
                Material.HOPPER,
                ChatColor.YELLOW + "放归 " + selectedPetUUIDs.size() + " 宠物",
                Arrays.asList(
                        ChatColor.DARK_RED + "⚠ 警告 ⚠",
                        ChatColor.RED + "该操作不可逆!",
                        ChatColor.RED + "选择的宠物将被永久放归自然。"
                )
        ));

        gui.setItem(11, this.createActionButton(
                Material.LIME_WOOL,
                ChatColor.GREEN + "取消",
                "open_batch_manage",
                null,
                Collections.singletonList(ChatColor.GRAY + "保留你的宠物")
        ));

        gui.setItem(15, this.createActionButton(
                Material.RED_WOOL,
                ChatColor.DARK_RED + "确认放归宠物",
                "batch_confirm_free",
                null,
                Arrays.asList(
                        ChatColor.RED + "点击永久放归宠物",
                        ChatColor.DARK_RED + "该操作不可逆!"
                )
        ));

        player.openInventory(gui);
    }

    public void openBatchFriendlyPlayerMenu(Player player, Set<UUID> selectedPetUUIDs, int page) {
	String title = ChatColor.DARK_AQUA + "批量选择转化为友好态度: " + selectedPetUUIDs.size() + " 宠物";
        Inventory gui = Bukkit.createInventory(player, 54, title);

        List<PetData> pets = selectedPetUUIDs.stream().map(petManager::getPetData).filter(Objects::nonNull).toList();
        Set<UUID> commonFriendlies = pets.isEmpty() ? new HashSet<>()
                : new HashSet<>(pets.get(0).getFriendlyPlayers());
        for (int i = 1; i < pets.size(); i++) {
            commonFriendlies.retainAll(pets.get(i).getFriendlyPlayers());
        }
        List<UUID> friendlyList = new ArrayList<>(commonFriendlies);

        int itemsPerPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) friendlyList.size() / itemsPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, friendlyList.size());

        for (int i = startIndex; i < endIndex; i++) {
            UUID friendlyUUID = friendlyList.get(i);
            OfflinePlayer friendly = Bukkit.getOfflinePlayer(friendlyUUID);
            ItemStack head = this.createPlayerHead(friendlyUUID, ChatColor.YELLOW + friendly.getName(), "remove_batch_friendly", null, Collections.singletonList(ChatColor.RED + "点击从所有已选宠物移除。"));
            gui.setItem(i - startIndex, head);
        }

        if (page > 0) {
		gui.setItem(45, createActionButton(Material.ARROW, ChatColor.GREEN + "上一页", "batch_friendly_page", null, null));
        }
	gui.setItem(48, createActionButton(Material.ANVIL, ChatColor.GREEN + "添加友好玩家", "add_batch_friendly_prompt", null, Collections.singletonList(ChatColor.GRAY + "将一名玩家设为所有已选宠物的友善目标。")));
        if (endIndex < friendlyList.size()) {
            gui.setItem(50, createActionButton(Material.ARROW, ChatColor.GREEN + "下一页", "batch_friendly_page", null, null));
        }
        gui.setItem(53, createActionButton(Material.ARROW, ChatColor.YELLOW + "返回批量管理", "open_batch_manage", null, null));

        player.openInventory(gui);
    }

    public void openBatchTransferMenu(Player player, Set<UUID> selectedPetUUIDs) {
        String title = ChatColor.DARK_AQUA + "赠予 " + selectedPetUUIDs.size() + " 宠物";

        List<Player> eligiblePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(getEffectiveOwner(player)))
                .map(p -> (Player) p)
                .sorted(Comparator.comparing(Player::getName))
                .toList();

        int rows = (int) Math.ceil((double) eligiblePlayers.size() / 9.0);
        int invSize = Math.max(18, (rows + 1) * 9);
        if (eligiblePlayers.isEmpty()) invSize = 27;
        invSize = Math.min(54, invSize);

        Inventory gui = Bukkit.createInventory(player, invSize, title);

        if (eligiblePlayers.isEmpty()) {
            gui.setItem(invSize == 27 ? 13 : 22, this.createItem(Material.BARRIER, ChatColor.RED + "没有玩家在线", Collections.singletonList(ChatColor.GRAY + "没有可赠予的玩家!")));
        } else {
            for (int i = 0; i < eligiblePlayers.size(); i++) {
                if (i >= 45) break;
                Player target = eligiblePlayers.get(i);
                ItemStack head = this.createPlayerHead(target.getUniqueId(), ChatColor.YELLOW + target.getName(), "batch_transfer_to_player", null, Arrays.asList(ChatColor.GRAY + "点击赠予 " + selectedPetUUIDs.size() + " 宠物", ChatColor.GRAY + "给 " + target.getName(), "", ChatColor.YELLOW + "⚠ 此操作不可逆!"));
                gui.setItem(i, head);
            }
        }

        gui.setItem(invSize - 1, this.createActionButton(Material.ARROW, ChatColor.YELLOW + "返回批量管理", "open_batch_manage", null, null));
        player.openInventory(gui);
    }

    public void openPetMenu(Player player, UUID petUUID) {
        PetData petData = this.petManager.getPetData(petUUID);
        if (petData == null) {
            player.sendMessage(ChatColor.RED + "无法找到该宠物数据。它可能已被放归或死亡。");
            this.openMainMenu(player);
            return;
        }

        String title = PET_MENU_TITLE_PREFIX + ChatColor.AQUA + petData.getDisplayName();
        if (title.length() > 32) {
            title = title.substring(0, 29) + "...";
        }

        Inventory gui = Bukkit.createInventory(player, 54, title);
        boolean isFavorite = petData.isFavorite();
        boolean protectionEnabled = petData.isProtectedFromPlayers();
        ChatColor nameColor = getNameColor(petData);
        Material headerIcon = getDisplayMaterialForPet(petData);

        if (petData.isDead()) {
            gui = Bukkit.createInventory(player, 27, title);
            ItemStack skull = new ItemStack(Material.SKELETON_SKULL);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "[死亡] " + petData.getDisplayName());
            meta.setLore(List.of(
                    ChatColor.DARK_RED + "该宠物已死亡!",
                    ChatColor.GRAY + "在上方选择一个操作."
            ));
            skull.setItemMeta(meta);
            gui.setItem(13, skull);
            Material reviveItem = this.plugin.getConfigManager().getReviveItem();
            gui.setItem(11, this.createActionButton(
            		reviveItem,
                    ChatColor.GREEN + "复活宠物",
                    "confirm_revive_pet",
                    petUUID,
                    List.of(ChatColor.GRAY + "复活这只宠物通过 '" + reviveItem.name() + "'.")
            ));
            gui.setItem(15, this.createActionButton(
                    Material.BARRIER,
                    ChatColor.RED + "移除宠物",
                    "confirm_remove_pet",
                    petUUID,
                    List.of(ChatColor.GRAY + "永久删除这只宠物。")
            ));
            player.openInventory(gui);
            return;
        }

        Entity petEntity = Bukkit.getEntity(petUUID);

        List<String> headerLore = new ArrayList<>();
        headerLore.add(ChatColor.GRAY + "类型: " + ChatColor.WHITE + petData.getEntityType().name());
        headerLore.add(ChatColor.GRAY + "模式: " + ChatColor.WHITE + petData.getMode().name());
        if (petEntity instanceof LivingEntity livingEntity && petEntity.isValid()) {
            double health = livingEntity.getHealth();
            double maxHealth = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		headerLore.add(ChatColor.RED + "生命值: " + ChatColor.WHITE + String.format("%.1f", health) + " / " + String.format("%.1f", maxHealth));
        } else {
		headerLore.add(ChatColor.GRAY + "生命值: 未知 (未加载)");
        }
        headerLore.add(ChatColor.GRAY + "保护: " + (protectionEnabled ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        int friendlyCount = petData.getFriendlyPlayers().size();
        if (friendlyCount > 0) {
		headerLore.add("" + ChatColor.GREEN + friendlyCount + " 友好玩家" + (friendlyCount == 1 ? "" : "s"));
        }
        if (petEntity instanceof Ageable ageable && !ageable.isAdult()) {
            headerLore.add(ChatColor.LIGHT_PURPLE + "幼宠");
        }
        headerLore.add("");
        headerLore.add(ChatColor.YELLOW + "左键：自定义展示"); 
        headerLore.add(ChatColor.YELLOW + "右键：切换最爱");

        
        ItemStack header = new ItemStack(headerIcon);
        ItemMeta hMeta = header.getItemMeta();
        String favoriteStar = isFavorite ? ChatColor.GOLD + "★ " : "";
        hMeta.setDisplayName(favoriteStar + nameColor + petData.getDisplayName());
        hMeta.setLore(headerLore);
        hMeta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "pet_header");
        hMeta.getPersistentDataContainer().set(PET_UUID_KEY, PersistentDataType.STRING, petUUID.toString());
        hMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        header.setItemMeta(hMeta);
        gui.setItem(4, header);

        
        Entity petEnt = Bukkit.getEntity(petUUID);
        if (petEnt instanceof Ageable a && !a.isAdult()) {
            boolean paused = petData.isGrowthPaused();
            gui.setItem(8, createActionButton(
                    paused ? Material.GOLDEN_CARROT : Material.CARROT,
                    (paused ? ChatColor.GREEN : ChatColor.YELLOW) + "停止成长",
                    "toggle_growth_pause",
                    petUUID,
                    List.of(
                            ChatColor.GRAY + "当前: " + (paused ? "暂停" : "成长中"),
                            "",
                            ChatColor.YELLOW + "点击切换"
                    )
            ));
        }

        gui.setItem(11, this.createModeButton(Material.FEATHER, "设置友善状态", BehaviorMode.PASSIVE, petData));
        gui.setItem(13, this.createModeButton(Material.IRON_SWORD, "设置中立状态", BehaviorMode.NEUTRAL, petData));
        gui.setItem(15, this.createModeButton(Material.DIAMOND_SWORD, "设置敌对状态", BehaviorMode.AGGRESSIVE, petData));

        boolean canSit = petEntity instanceof Sittable;
        if (canSit) {
            boolean isSitting = false;
            if (petEntity instanceof Sittable s) {
                isSitting = s.isSitting();
            }
            gui.setItem(20, this.createActionButton(
                    isSitting ? Material.ARMOR_STAND : Material.SADDLE,
                    isSitting ? ChatColor.GREEN + "让宠物站立" : ChatColor.YELLOW + "让宠物坐下",
                    "toggle_sit",
                    petData.getPetUUID(),
                    Collections.singletonList(ChatColor.GRAY + "当前: " + (isSitting ? "坐下" : "站立"))
            ));
        }

        gui.setItem(22, this.createActionButton(
                Material.ENDER_PEARL,
                ChatColor.LIGHT_PURPLE + "传送宠物到你的位置",
                "teleport_pet",
                petData.getPetUUID(),
                Collections.singletonList(ChatColor.GRAY + "召唤宠物到你的位置。")
        ));

        gui.setItem(24, this.createActionButton(
                Material.MILK_BUCKET,
                ChatColor.AQUA + "安抚宠物",
                "calm_pet",
                petData.getPetUUID(),
                Arrays.asList(
                        ChatColor.GRAY + "清除宠物当前的目标",
                        ChatColor.GRAY + "并重设所有愤怒值"
                )
        ));

        gui.setItem(29, this.createActionButton(
                Material.ANVIL,
	ChatColor.GREEN + "重命名宠物",
                "rename_pet_prompt",
                petData.getPetUUID(),
                Arrays.asList(
                        ChatColor.GRAY + "左键：点击并在聊天栏输入新的昵称",
                        ChatColor.GRAY + "Shift+点击: 重设为默认名字"
                )
        ));

        gui.setItem(31, this.createActionButton(
                Material.PLAYER_HEAD,
                ChatColor.GREEN + "管理友好玩家",
                "manage_friendly",
                petData.getPetUUID(),
                Collections.singletonList(ChatColor.GRAY + "设置宠物不会攻击的玩家")
        ));

        gui.setItem(33, this.createActionButton(
                Material.LEAD,
                ChatColor.GOLD + "赠予宠物",
                "open_transfer",
                petData.getPetUUID(),
                Arrays.asList(
                        ChatColor.GRAY + "把宠物赠予其他玩家",
                        ChatColor.YELLOW + "⚠ 此操作不可逆!"
                )
        ));

        String protName = protectionEnabled
                ? ChatColor.YELLOW + "禁用互不攻击状态"
                : ChatColor.GREEN + "启用互不攻击状态";
        List<String> protLore = new ArrayList<>();
        protLore.add(ChatColor.GRAY + "当前: " + (protectionEnabled ? ChatColor.GREEN + "已启用" : ChatColor.RED + "已禁用"));
        protLore.add("");
        protLore.add(ChatColor.GRAY + "玩家无法伤害宠物,");
        protLore.add(ChatColor.GRAY + "并且它们不会攻击玩家。");
        gui.setItem(35, this.createActionButton(
                Material.SHIELD,
                protName,
                "toggle_mutual_protection",
                petData.getPetUUID(),
                protLore
        ));

        


        gui.setItem(49, this.createActionButton(Material.ARROW, ChatColor.YELLOW + "返回宠物列表", "back_to_main", null, null));

        gui.setItem(53, this.createActionButton(
                Material.BARRIER,
                ChatColor.RED + "放归这只宠物",
                "confirm_free_pet_prompt",
                petData.getPetUUID(),
                Arrays.asList("" + ChatColor.DARK_RED + ChatColor.BOLD + "警告:", ChatColor.RED + "这是永久性的!")
        ));

        player.openInventory(gui);
    }

    
    public void openConfirmFreeMenu(Player player, UUID petUUID) {
        PetData petData = this.petManager.getPetData(petUUID);
        if (petData == null) {
            player.sendMessage(ChatColor.RED + "错误：无法找到该宠物数据。");
            this.openMainMenu(player);
            return;
        }

        String title = ChatColor.DARK_RED + "确认放归: " + ChatColor.RED + petData.getDisplayName();
        if (title.length() > 32) {
            title = title.substring(0, 29) + "...";
        }

        Inventory gui = Bukkit.createInventory(player, 27, title);

        gui.setItem(13, this.createItem(
                this.getPetMaterial(petData.getEntityType()),
                ChatColor.YELLOW + petData.getDisplayName(),
                Arrays.asList(
                        ChatColor.GRAY + "类型: " + ChatColor.WHITE + petData.getEntityType().name(),
                        "",
                        ChatColor.DARK_RED + "⚠ 警告 ⚠",
                        ChatColor.RED + "此操作不可逆!",
                        ChatColor.RED + "宠物将会被永久放归自然。"
                ),
                petUUID
        ));

        gui.setItem(11, this.createActionButton(
                Material.LIME_WOOL,
                ChatColor.GREEN + "Cancel",
                "cancel_free",
                petUUID,
                Collections.singletonList(ChatColor.GRAY + "保留你的宠物")
        ));

        gui.setItem(15, this.createActionButton(
                Material.RED_WOOL,
                ChatColor.DARK_RED + "确认放归宠物",
                "confirm_free",
                petUUID,
                Arrays.asList(
                        ChatColor.RED + "点击永久放归宠物",
                        ChatColor.DARK_RED + "此操作不可逆!"
                )
        ));

        player.openInventory(gui);
    }

    
    public void openTransferMenu(Player player, UUID petUUID) {
        PetData petData = this.petManager.getPetData(petUUID);
        if (petData == null) {
            player.sendMessage(ChatColor.RED + "错误：无法找到该宠物数据。");
            this.openMainMenu(player);
            return;
        }

        String title = ChatColor.DARK_AQUA + "赠予: " + ChatColor.AQUA + petData.getDisplayName();
        if (title.length() > 32) {
            title = title.substring(0, 29) + "...";
        }

        List<Player> eligiblePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(getEffectiveOwner(player)))
                .sorted(Comparator.comparing(Player::getName))
                .collect(Collectors.toList());


        int rows = (int) Math.ceil((double) eligiblePlayers.size() / 9.0);
        int invSize = Math.max(18, (rows + 1) * 9);
        if (eligiblePlayers.isEmpty()) invSize = 27;
        invSize = Math.min(54, invSize);

        Inventory gui = Bukkit.createInventory(player, invSize, title);

        if (eligiblePlayers.isEmpty()) {
            gui.setItem(invSize == 27 ? 13 : 22, this.createItem(
                    Material.BARRIER,
                    ChatColor.RED + "没有玩家在线",
                    Collections.singletonList(ChatColor.GRAY + "没有可赠予的玩家!")
            ));
        } else {
            for (int i = 0; i < eligiblePlayers.size(); i++) {
                if (i >= 45) break;
                Player target = eligiblePlayers.get(i);
                ItemStack head = this.createPlayerHead(target.getUniqueId(), ChatColor.YELLOW + target.getName(), "transfer_to_player", petUUID,
                        Arrays.asList(ChatColor.GRAY + "点击赠予 " + petData.getDisplayName(), ChatColor.GRAY + "给 " + target.getName(), "", ChatColor.YELLOW + "⚠ 此操作不可逆!"));
                gui.setItem(i, head);
            }
        }


        gui.setItem(invSize - 1, this.createActionButton(Material.ARROW, ChatColor.YELLOW + "返回宠物管理 ", "back_to_pet", petUUID, null));
        player.openInventory(gui);
    }

    
    public void openFriendlyPlayerMenu(Player player, UUID petUUID, int page) {
        PetData petData = this.petManager.getPetData(petUUID);
        if (petData == null) {
            player.sendMessage(ChatColor.RED + "错误：无法找到该宠物数据。.");
            this.openMainMenu(player);
            return;
        }

        String title = FRIENDLY_MENU_TITLE_PREFIX + ChatColor.AQUA + petData.getDisplayName();
        if (title.length() > 32) {
            title = title.substring(0, 29) + "...";
        }

        List<UUID> friendlyList = new ArrayList<>(petData.getFriendlyPlayers());

        int itemsPerPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil(friendlyList.size() / (double) itemsPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, friendlyList.size());

        Inventory gui = Bukkit.createInventory(player, 54, title);

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            UUID friendlyUUID = friendlyList.get(i);
            OfflinePlayer friendly = Bukkit.getOfflinePlayer(friendlyUUID);
            String name = friendly.getName() != null ? friendly.getName() : friendlyUUID.toString().substring(0, 8);
            ItemStack head = this.createPlayerHead(
                    friendlyUUID,
                    ChatColor.YELLOW + name,
                    "remove_friendly",
                    petUUID,
		Collections.singletonList(ChatColor.RED + "点击移出友好玩家名单")
            );
            gui.setItem(slot++, head);
        }

        if (page > 0) {
            gui.setItem(45, this.createFriendlyNavButton(Material.ARROW, ChatColor.GREEN + "上一页", "friendly_page", petUUID, page - 1));
        }

	gui.setItem(48, this.createActionButton(Material.ANVIL, ChatColor.GREEN + "添加友好玩家", "add_friendly_prompt", petUUID, Collections.singletonList(ChatColor.GRAY + "点击并在聊天框输入友好玩家的昵称。")));

        if (endIndex < friendlyList.size()) {
            gui.setItem(50, this.createFriendlyNavButton(Material.ARROW, ChatColor.GREEN + "下一页", "friendly_page", petUUID, page + 1));
        }

        gui.setItem(53, this.createActionButton(Material.ARROW, ChatColor.YELLOW + "返回宠物管理", "back_to_pet", petUUID, null));

        player.openInventory(gui);
    }


    
    public void openColorPicker(Player player, UUID petUUID) {
        PetData petData = petManager.getPetData(petUUID);
        if (petData == null) {
            player.sendMessage(ChatColor.RED + "错误：无法找到该宠物。");
            openMainMenu(player);
            return;
        }
        String title = ChatColor.DARK_AQUA + "选择颜色: " + petData.getDisplayName();
        if (title.length() > 32) title = ChatColor.DARK_AQUA + "选择颜色";

        Inventory gui = Bukkit.createInventory(player, 27, title);

        
        LinkedHashMap<Material, ChatColor> choices = new LinkedHashMap<>();
        choices.put(Material.WHITE_DYE, ChatColor.WHITE);
        choices.put(Material.ORANGE_DYE, ChatColor.GOLD);
        choices.put(Material.MAGENTA_DYE, ChatColor.LIGHT_PURPLE);
        choices.put(Material.LIGHT_BLUE_DYE, ChatColor.AQUA);
        choices.put(Material.YELLOW_DYE, ChatColor.YELLOW);
        choices.put(Material.LIME_DYE, ChatColor.GREEN);
        choices.put(Material.PINK_DYE, ChatColor.LIGHT_PURPLE);
        choices.put(Material.GRAY_DYE, ChatColor.GRAY);
        choices.put(Material.LIGHT_GRAY_DYE, ChatColor.WHITE);
        choices.put(Material.CYAN_DYE, ChatColor.DARK_AQUA);
        choices.put(Material.PURPLE_DYE, ChatColor.DARK_PURPLE);
        choices.put(Material.BLUE_DYE, ChatColor.BLUE);
        choices.put(Material.BROWN_DYE, ChatColor.GOLD);
        choices.put(Material.GREEN_DYE, ChatColor.DARK_GREEN);
        choices.put(Material.RED_DYE, ChatColor.RED);
        choices.put(Material.BLACK_DYE, ChatColor.BLACK);

        int i = 0;
        for (Map.Entry<Material, ChatColor> entry : choices.entrySet()) {
            if (i >= 26) break; 
            Material mat = entry.getKey();
            ChatColor color = entry.getValue();
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(color + color.name());
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "点击设置该颜色."));
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "choose_color");
            meta.getPersistentDataContainer().set(PET_UUID_KEY, PersistentDataType.STRING, petUUID.toString());
            meta.getPersistentDataContainer().set(COLOR_KEY, PersistentDataType.STRING, color.name());
            item.setItemMeta(meta);
            gui.setItem(i++, item);
        }

        
        gui.setItem(26, createActionButton(Material.ARROW, ChatColor.YELLOW + "返回", "back_to_pet", petUUID, null));

        player.openInventory(gui);
    }

    private ItemStack createNavigationButton(Material material, String name, String action, int targetPage, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && !lore.isEmpty()) meta.setLore(lore);
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, targetPage);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFriendlyNavButton(Material material, String name, String action, UUID petUUID, int targetPage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(PET_UUID_KEY, PersistentDataType.STRING, petUUID.toString());
        meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, targetPage);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createActionButton(Material material, String name, String action, UUID petUUID, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && !lore.isEmpty()) meta.setLore(lore);
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        if (petUUID != null) {
            meta.getPersistentDataContainer().set(PET_UUID_KEY, PersistentDataType.STRING, petUUID.toString());
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public void openBatchConfirmRemoveDeadMenu(Player player, EntityType petType, int deadPetCount) {
        String typeName = petType.name().toLowerCase().replace('_', ' ');
        String title = ChatColor.DARK_RED + "确认移除：死亡 " + typeName + "类";
        Inventory gui = Bukkit.createInventory(player, 27, title);

        ItemStack info = createItem(
                Material.SKELETON_SKULL,
                ChatColor.YELLOW + "移除 " + deadPetCount + " Dead Pet Record(s)",
                Arrays.asList(
                        ChatColor.GRAY + "宠物类型: " + ChatColor.WHITE + typeName,
                        "",
                        ChatColor.DARK_RED + "⚠ 警告 ⚠",
                        ChatColor.RED + "该操作不可逆。"
                )
        );
        gui.setItem(13, info);

        ItemStack cancel = new ItemStack(Material.LIME_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.GREEN + "取消");
        cancelMeta.setLore(Collections.singletonList(ChatColor.GRAY + "保留宠物记录."));
        cancelMeta.getPersistentDataContainer().set(BatchActionsGUI.BATCH_ACTION_KEY, PersistentDataType.STRING, "open_pet_select");
        cancelMeta.getPersistentDataContainer().set(BatchActionsGUI.PET_TYPE_KEY, PersistentDataType.STRING, petType.name());
        cancel.setItemMeta(cancelMeta);
        gui.setItem(15, cancel);

        ItemStack confirm = new ItemStack(Material.RED_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.DARK_RED + "确认移除");
        confirmMeta.setLore(Collections.singletonList(ChatColor.RED + "点击永久移除这些记录。"));
        confirmMeta.getPersistentDataContainer().set(BatchActionsGUI.BATCH_ACTION_KEY, PersistentDataType.STRING, "batch_confirm_remove_dead");
        confirmMeta.getPersistentDataContainer().set(BatchActionsGUI.PET_TYPE_KEY, PersistentDataType.STRING, petType.name());
        confirm.setItemMeta(confirmMeta);
        gui.setItem(11, confirm);

        player.openInventory(gui);
    }

    public ItemStack createPetItem(PetData petData) {
        Entity petEntity = Bukkit.getEntity(petData.getPetUUID());
        if (petData.isDead()) {
            ItemStack skull = new ItemStack(Material.SKELETON_SKULL);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "[DEAD] " + petData.getDisplayName());
            meta.setLore(List.of(
                    ChatColor.DARK_RED + "这只宠物已经死亡!",
                    ChatColor.GRAY + "点击浏览复活或移除选项。"
            ));
            meta.getPersistentDataContainer().set(PET_UUID_KEY, PersistentDataType.STRING, petData.getPetUUID().toString());
            skull.setItemMeta(meta);
            return skull;
        }

        Material mat = getDisplayMaterialForPet(petData);
        ChatColor nameColor = getNameColor(petData);

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String displayName = (petData.isFavorite() ? ChatColor.GOLD + "★ " : "") + nameColor + petData.getDisplayName();
        meta.setDisplayName(displayName);
        List<String> lore = new java.util.ArrayList<>();
        lore.add(ChatColor.GRAY + "类型: " + ChatColor.WHITE + petData.getEntityType().name());
        lore.add(ChatColor.GRAY + "模式: " + ChatColor.WHITE + petData.getMode().name());

        if (petEntity instanceof LivingEntity livingEntity && petEntity.isValid()) {
            double health = livingEntity.getHealth();
            double maxHealth = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            lore.add(ChatColor.RED + "生命值: " + ChatColor.WHITE + String.format("%.1f", health) + " / " + String.format("%.1f", maxHealth));
        } else {
            lore.add(ChatColor.GRAY + "生命值: 未知 (未加载)");
        }

        lore.add(ChatColor.GRAY + "保护: " + (petData.isProtectedFromPlayers() ? ChatColor.GREEN + "已启用" : ChatColor.RED + "未启用"));
        int friendlyCount = petData.getFriendlyPlayers().size();
        if (friendlyCount > 0) {
            lore.add("" + ChatColor.GREEN + friendlyCount + " 友好玩家数量" + (friendlyCount == 1 ? "" : "个"));
        }
        if (petEntity instanceof Ageable ageable && !ageable.isAdult()) {
            lore.add(ChatColor.LIGHT_PURPLE + "幼宠");
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "点击管理这只宠物");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(PET_UUID_KEY, PersistentDataType.STRING, petData.getPetUUID().toString());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createModeButton(Material material, String name, BehaviorMode mode, PetData currentPetData) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean isActive = currentPetData.getMode() == mode;
            meta.setDisplayName((isActive ? "" + ChatColor.GREEN + ChatColor.BOLD : currentPetData.getMode() == BehaviorMode.BATCH ? ChatColor.AQUA : ChatColor.YELLOW) + name);

            List<String> lore = new ArrayList<>();
            switch (mode) {
                case PASSIVE -> lore.add(ChatColor.GRAY + "宠物不会攻击任何目标");
                case NEUTRAL -> {
                    lore.add(ChatColor.GRAY + "宠物将保护主人并会攻击");
                    lore.add(ChatColor.GRAY + "主人的目标 (原版).");
                }
                case AGGRESSIVE -> {
                    lore.add(ChatColor.GRAY + "宠物攻击邻近的敌对生物");
                    lore.add(ChatColor.GRAY + "主动执行（如条件允许）。");
                }
                default -> {
                }
            }

            if (isActive) {
                lore.add("");
                lore.add(ChatColor.DARK_GREEN + "▶ " + ChatColor.GREEN + "当前活跃状态");
            } else if (currentPetData.getMode() == BehaviorMode.BATCH) {
                lore.add("");
                lore.add(ChatColor.AQUA + "当前选择包含混合模式。");
                lore.add(ChatColor.YELLOW + "点击所有为这个模式。");
            } else {
                lore.add("");
                lore.add(ChatColor.YELLOW + "点击激活这个模式。");
            }

            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            String action;
            if (currentPetData.getPetUUID() == null) {
                action = "batch_set_mode_" + mode.name();
            } else {
                action = "set_mode_" + mode.name();
            }
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);

            if (currentPetData.getPetUUID() != null) {
                meta.getPersistentDataContainer().set(PET_UUID_KEY, PersistentDataType.STRING, currentPetData.getPetUUID().toString());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPlayerHead(UUID playerUUID, String name, String action, UUID petContextUUID, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerUUID);
            meta.setOwningPlayer(targetPlayer);
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) meta.setLore(lore);
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
            if (petContextUUID != null) {
                meta.getPersistentDataContainer().set(PET_UUID_KEY, PersistentDataType.STRING, petContextUUID.toString());
            }
            meta.getPersistentDataContainer().set(TARGET_PLAYER_UUID_KEY, PersistentDataType.STRING, playerUUID.toString());
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createItem(Material material, String name, List<String> lore) {
        return this.createItem(material, name, lore, null);
    }

    public ItemStack createItem(Material material, String name, List<String> lore, UUID petUUIDContext) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) meta.setLore(lore);
            if (petUUIDContext != null) {
                meta.getPersistentDataContainer().set(PET_UUID_KEY, PersistentDataType.STRING, petUUIDContext.toString());
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    
    private ChatColor getNameColor(PetData data) {
        String c = data.getDisplayColor();
        if (c == null || c.isEmpty()) return ChatColor.AQUA;
        try {
            return ChatColor.valueOf(c);
        } catch (IllegalArgumentException ex) {
            return ChatColor.AQUA;
        }
    }

    
    private Material getDisplayMaterialForPet(PetData data) {
        if (data.getCustomIconMaterial() != null) {
            try {
                return Material.valueOf(data.getCustomIconMaterial());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return getPetMaterial(data.getEntityType());
    }

    public Material getPetMaterial(EntityType type) {
        return switch (type) {
            case WOLF -> Material.WOLF_SPAWN_EGG;
            case CAT -> Material.CAT_SPAWN_EGG;
            case PARROT -> Material.PARROT_SPAWN_EGG;
            
            case HORSE -> Material.HORSE_SPAWN_EGG;
            case DONKEY -> Material.DONKEY_SPAWN_EGG;
            case MULE -> Material.MULE_SPAWN_EGG;
            case LLAMA -> Material.LLAMA_SPAWN_EGG;
            case TRADER_LLAMA -> Material.TRADER_LLAMA_SPAWN_EGG;
            case SKELETON_HORSE -> Material.SKELETON_HORSE_SPAWN_EGG;
            case ZOMBIE_HORSE -> Material.ZOMBIE_HORSE_SPAWN_EGG;
            default -> Material.NAME_TAG;
        };
    }

    public void openCustomizationMenu(Player player, UUID petUUID) {
        PetData petData = petManager.getPetData(petUUID);
        if (petData == null) {
            player.sendMessage(ChatColor.RED + "错误：无法找到该宠物。");
            openMainMenu(player);
            return;
        }
        String title = ChatColor.DARK_AQUA + "自定义: " + petData.getDisplayName();
        if (title.length() > 32) title = ChatColor.DARK_AQUA + "自定义宠物";

        Inventory gui = Bukkit.createInventory(player, 45, title);

        
        gui.setItem(4, createItem(
                getDisplayMaterialForPet(petData),
                getNameColor(petData) + petData.getDisplayName(),
                Collections.singletonList(ChatColor.GRAY + "选择以上的选项。")
        ));

        
        gui.setItem(20, createActionButton(
                Material.ITEM_FRAME,
                ChatColor.AQUA + "设置展示图标",
                "set_display_icon",
                petUUID,
                Arrays.asList(
				        ChatColor.GRAY + "左键：使用你手上的物品",
                        ChatColor.GRAY + "作为这只宠物的图标",
                        ChatColor.GRAY + "Shift+点击: 重设为默认。"
                )
        ));

        
        gui.setItem(24, createActionButton(
                Material.WHITE_DYE,
                ChatColor.AQUA + "编辑昵称颜色",
                "set_display_color",
                petUUID,
                Arrays.asList(
                        ChatColor.GRAY + "左键：选择一个颜色",
                        ChatColor.GRAY + "Shift+点击: 重设为默认名字。"
                )
        ));

        
        gui.setItem(44, createActionButton(Material.ARROW, ChatColor.YELLOW + "返回宠物管理", "back_to_pet", petUUID, null));

        player.openInventory(gui);
    }

}


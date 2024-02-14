package org.s1queence;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.s1queence.plugin.libs.YamlDocument;

import java.util.HashMap;

import static org.s1queence.api.S1Booleans.isLuck;
import static org.s1queence.api.S1Booleans.isNotAllowableInteraction;
import static org.s1queence.api.S1TextUtils.getConvertedTextFromConfig;
import static org.s1queence.api.S1Utils.sendActionBarMsg;
import static org.s1queence.api.S1Utils.toCenterLocation;

public class ChopListener implements Listener {

    private final TreeChopChopper plugin;
    public ChopListener(TreeChopChopper plugin) {
        this.plugin = plugin;
    }

    private final HashMap<Block, Integer> treeChopProgress = new HashMap<>();
    private boolean isLog(Block block) {
        String stringedBlockType = block.getType().toString();
        return stringedBlockType.contains("LOG") || stringedBlockType.contains("WOOD");
    }

    private boolean isDirtLikeBlock(Block block) {
        Material type = block.getType();
        String stringedType = block.getType().toString();
        return type.equals(Material.GRASS_BLOCK) || type.equals(Material.PODZOL) || type.equals(Material.GRAVEL) || type.equals(Material.SAND) || stringedType.contains("CONCRETE_POWDER") || stringedType.contains("DIRT");
    }

    private boolean isLeaves(Block block) {
        return block.getType().toString().contains("LEAVES");
    }

    private boolean isNotAllowableBlock(Block block) {
        return !block.getType().equals(Material.AIR) && !isLeaves(block) && !block.isPassable() && !isDirtLikeBlock(block);
    }

    private int getTreeHeight(Block block) {
        Location blockLocation = block.getLocation().clone();
        int treeHeight = 0;
        int matches = 0;
        while (isLog(blockLocation.getBlock())) {
            Block northBlock = blockLocation.getBlock().getRelative(BlockFace.NORTH);
            Block eastBlock = blockLocation.getBlock().getRelative(BlockFace.EAST);
            Block southBlock = blockLocation.getBlock().getRelative(BlockFace.SOUTH);
            Block westBlock = blockLocation.getBlock().getRelative(BlockFace.WEST);

            if (isNotAllowableBlock(northBlock) || isNotAllowableBlock(eastBlock) || isNotAllowableBlock(southBlock) || isNotAllowableBlock(westBlock)) {
                matches++;
                if (matches >= 3) {
                    treeHeight = -1;
                    break;
                }
            }

            treeHeight++;
            blockLocation.setY(blockLocation.getY() + 1.0d);
        }
        return treeHeight;
    }

    private String getWoodType(Material mat) {
        String stringedType = mat.toString();
        return stringedType.contains("ACACIA") ? "ACACIA" :
                stringedType.contains("DARK_OAK") ? "DARK_OAK" :
                        stringedType.contains("SPRUCE") ? "SPRUCE" :
                                stringedType.contains("JUNGLE") ? "JUNGLE" :
                                        stringedType.contains("BIRCH") ? "BIRCH" :
                                                 (stringedType.contains("OAK") && !stringedType.contains("DARK")) ? "OAK" : "NONE";
    }

    private boolean isSameWoodType(Material woodenMaterial, Block block) {
        return getWoodType(block.getType()).equals(getWoodType(woodenMaterial));
    }

    private boolean isSneakDestroyBypass(Player player) {
        return player.isSneaking() && plugin.getPluginConfig().getBoolean("sneak_destroy_bypass");
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getHand() == null) return;
        if (!e.getHand().equals(EquipmentSlot.HAND)) return;
        if (!e.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;
        Player player = e.getPlayer();
        if (isSneakDestroyBypass(player)) return;
        if (!player.getGameMode().equals(GameMode.SURVIVAL)) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        Material blockType = block.getType();
        if (!isLog(block)) return;
        ItemStack mainItem = player.getInventory().getItemInMainHand();
        boolean isAxe = mainItem.getType().toString().contains("AXE");
        if (!isAxe) return;
        Location blockLocation = block.getLocation();
        World world = block.getWorld();

        YamlDocument config = plugin.getPluginConfig();
        String pluginName = plugin.getName();

        if (isNotAllowableInteraction(player, blockLocation)) {
            e.setCancelled(true);
            return;
        }

        int treeHeight = getTreeHeight(block);

        if (treeHeight < 4) return;

        if (player.getAttackCooldown() != 1.0f) {
            sendActionBarMsg(player, getConvertedTextFromConfig(config, "attack_coolDown_is_not_expired", pluginName));
            return;
        }

        if (!treeChopProgress.containsKey(block)) treeChopProgress.put(block, treeHeight);
        int treeHits = treeChopProgress.get(block) - 1;
        if (config.getBoolean("sweep_attack_particle")) world.spawnParticle(Particle.SWEEP_ATTACK, toCenterLocation(blockLocation), 1);
        world.playSound(blockLocation, config.getString("chop_sound"), 5.0f, 1.0f);
        treeChopProgress.put(block, treeHits);

        if (treeHits != 0) {
            sendActionBarMsg(player, getConvertedTextFromConfig(config, "hits_left", pluginName).replace("%hits_left%", treeHits + ""));
            return;
        }

        treeChopProgress.remove(block);

        Location startLocationToDestroyStem = block.getLocation().clone();
        while (isLog(startLocationToDestroyStem.getBlock())) {
            world.getBlockAt(startLocationToDestroyStem).breakNaturally();
            startLocationToDestroyStem.setY(startLocationToDestroyStem.getY() + 1.0d);
        }

        int blockZ = blockLocation.getBlockZ() - 3;
        int blockX = blockLocation.getBlockX() - 3;
        for (int i = 0; i <= treeHeight + 3; i++) {
            for (int j = 0; j <= 6; j++) {
                for (int k = 0; k <= 6; k++) {
                    Block toChop = world.getBlockAt(blockX + j, blockLocation.getBlockY(), blockZ + k);
                    Location toChopLocation = toChop.getLocation();
                    if (!isLeaves(toChop)) continue;
                    if (!isSameWoodType(blockType, toChop)) continue;
                    if (isLuck(config.getInt("leaves_fall_chance"))) world.spawnFallingBlock(toChopLocation, toChop.getBlockData());
                    world.getBlockAt(toChopLocation).breakNaturally();
                }
            }
            blockLocation.setY(blockLocation.getY() + 1.0d);
        }

        ItemMeta im = mainItem.getItemMeta();
        if (im == null) return;
        Damageable dIM = (Damageable) im;
        int damage = dIM.getDamage() + treeHeight * 2;
        if (mainItem.getType().getMaxDurability() - damage <= 0) {
            player.getInventory().remove(mainItem);
            world.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 5.0f, 1.0f);
            world.spawnParticle(Particle.ITEM_CRACK, player.getLocation(), 10, 0.3, 0.5, 0.3, 0, mainItem);
            return;
        }
        dIM.setDamage(damage);
        mainItem.setItemMeta(dIM);
    }

    @EventHandler
    private void onPlayerDestroyBLock(BlockBreakEvent e) {
        Player player = e.getPlayer();
        if (isSneakDestroyBypass(player)) return;
        Block block = e.getBlock();
        treeChopProgress.remove(block);
        if (!isLog(block)) return;
        if (!player.getGameMode().equals(GameMode.SURVIVAL)) return;

        if (getTreeHeight(block) < 4) return;

        e.setCancelled(true);
        sendActionBarMsg(player, getConvertedTextFromConfig(plugin.getPluginConfig(), "default_destroy_block_of_tree_stem", plugin.getName()));
    }
}


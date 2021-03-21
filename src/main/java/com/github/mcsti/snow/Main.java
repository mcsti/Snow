package com.github.mcsti.snow;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public final class Main extends JavaPlugin implements Listener {
    
    private static final int CONFIG_VERSION = 0;
    private              int range          = 7;
    private              int maxHeight      = 356;
    
    @Override
    public void onEnable() {
        if (!getConfig().contains("version") || !getConfig().isInt("version") || getConfig()
                .getInt("version") != CONFIG_VERSION) {
            getConfig().set("version", CONFIG_VERSION);
            getConfig().set("maxHeight", maxHeight);
            getConfig().set("range", range);
            saveConfig();
        }
        maxHeight = getConfig().getInt("maxHeight");
        range = getConfig().getInt("range");
        
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getWorlds().stream().filter(World::hasStorm)
                                           .flatMap(world -> world.getPlayers().stream())
                                           .collect(Collectors.toList())) {
                            Chunk chunk = player.getLocation().getChunk();
                    List<Block> surface = getHighestBlocksInChunk(getChunksAroundPlayer(player))
                                    .stream()
                                    .filter(block -> block.getTemperature() < 0.15)
                                    .filter(block -> block.getLightFromBlocks() <= 10)
                                    // .filter(block -> !Arrays
                                    //         .asList(Material.AIR, Material.WATER, Material.LAVA, Material.ICE)
                                    //         .contains(block.getType()))
                                    .filter(block -> (!block.getType().isAir() && block.getType().isSolid()) || block.getType() == Material.SNOW)
                                    .filter(Main.this::checkHeight)
                                    .collect(Collectors.toList());
                            for (int i = 0; i < player.getWorld().getGameRuleValue(GameRule.RANDOM_TICK_SPEED); i++) {
                                if (surface.isEmpty()) {
                                    break;
                                }
                                Block block = surface.get((int) (surface.size() * Math.random()));
                                if (block.getType() == Material.SNOW && ((Snow) block.getBlockData())
                                        .getLayers() != 8) {
                                    Snow snow = (Snow) block.getBlockData();
                                    snow.setLayers(snow.getLayers() + 1);
                                    block.setBlockData(snow);
                                } else {
                                    block = block.getRelative(0, 1, 0);
                                    block.setType(Material.SNOW);
                                }
                                surface.remove(block);
                            }
                        }
            }
        }.runTaskTimer(this, 0L, 1L);
    }
    
    private boolean checkHeight(Block originalBlock) {
        int   actualHeight = 0;
        Block block        = originalBlock;
        
        while ((block = block.getRelative(0, -1, 0)).getType() == Material.SNOW) {
            actualHeight += 100;
        }
        
        if (originalBlock.getType() == Material.SNOW) {
            Snow snow = (Snow) originalBlock.getBlockData();
            actualHeight += ((double) snow.getLayers() / (double) snow.getMaximumLayers()) * 100;
        }
        
        return actualHeight < maxHeight;
    }
    
    private Chunk[] getChunksAroundPlayer(Player player) {
        Chunk[] chunks = new Chunk[(int) Math.pow(range*2+1, 2)];
        int i = 0;
        for (int x = -range; x < range; x++) {
            for (int z = -range; z < range; z++) {
                Chunk chunk = player.getLocation().getChunk();
                chunks[i] = player.getWorld().getChunkAt(chunk.getX()+x, chunk.getZ()+z);
                i++;
            }
        }
        return chunks;
    }
    
    private List<Block> getHighestBlocksInChunk(Chunk... chunks) {
        List<Block> blocks = new ArrayList<>();
        for (Chunk chunk : chunks) {
            int         x      = 0;
            int         z      = 0;
            for (int y = 255; y > 0; y--) {
                if (z == 16 || chunk == null) {
                    continue;
                }
                Block block = chunk.getBlock(x, y, z);
                if (!block.isEmpty() && block.getType() != Material.AIR) {
                    blocks.add(block);
                    y = 255;
                    if (x < 15) {
                        x++;
                    } else {
                        z++;
                        x = 0;
                    }
                }
            }
        }
        return blocks;
    }
}

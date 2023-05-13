package net.quintia.mods;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import javax.annotation.Nullable;

public class Overlay implements IGuiOverlay {
    private static final int RANGE = 9;
    private static final int SIZE = RANGE * 2;
    private static final int PIXEL = 3;
    private static final int HALF_PIXEL = 1;
    private static final int FRAME = 3;
    private static final int MARGIN = 10;

    private static final int FRAME_COLOR = 0x60FFFFFF;
    private static final int AXIS_COLOR = 0x40FFFFFF;

    private static final int UNDEFINED_BLOCK_COLOR = 0xFF888888;
    private static final int WATER_COLOR = 0xFF3d60ee;

    public Overlay() {
    }

    private static String fDouble(double q) {
        return String.format("%.2f", q);
    }

    private static String fVec(Vec3 v) {
        return fDouble(v.x) + " " + fDouble(v.y) + " " + fDouble(v.z);
    }

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTick, int width, int height) {
        Minecraft mc = gui.getMinecraft();
        if (mc.player == null || mc.level == null) {
            return;
        }

        Vec3 eye = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getLookAngle();
        Block targeted = trace(mc, eye, lookVec, 5);
        if (targeted != null) {
            GuiComponent.drawCenteredString(poseStack, gui.getFont(),
                    targeted.getDescriptionId().substring(16),  // length of "minecraft.blocks." - 1
                    width - MARGIN - FRAME - PIXEL * RANGE,  // x
                    MARGIN + FRAME * 2 + PIXEL * SIZE,  // y
                    0x80FFFFFF);
        }

        // top frame
        GuiComponent.fill(poseStack,
                width - MARGIN - PIXEL * SIZE - FRAME * 2,
                MARGIN,
                width - MARGIN - FRAME,  // shorten right
                MARGIN + FRAME,
                FRAME_COLOR);
        // left frame
        GuiComponent.fill(poseStack,
                width - MARGIN - PIXEL * SIZE - FRAME * 2,
                MARGIN + FRAME,  // shorten top
                width - MARGIN - PIXEL * SIZE - FRAME,
                MARGIN + PIXEL * SIZE + FRAME * 2,
                FRAME_COLOR);
        // bottom frame
        GuiComponent.fill(poseStack,
                width - MARGIN - PIXEL * SIZE - FRAME,  //shorten left
                MARGIN + PIXEL * SIZE + FRAME,
                width - MARGIN,
                MARGIN + PIXEL * SIZE + FRAME * 2,
                FRAME_COLOR);
        // right frame
        GuiComponent.fill(poseStack,
                width - MARGIN - FRAME,
                MARGIN,
                width - MARGIN,
                MARGIN + PIXEL * SIZE + FRAME,  //shorten bottom
                FRAME_COLOR);

        // color gradation change
        double tick = (double) (System.currentTimeMillis() % 2000);  // 0 <= tick < 2000
        int triangleWaveIntensity = (int) (tick / 2000.0 * 32.0);  // 0x00 <= triangleWaveIntensity < 0x20
        if (0x10 < triangleWaveIntensity) {
            triangleWaveIntensity = 0x20 - triangleWaveIntensity;  //  0x10 > triangleWaveIntensity > 0x00
        }
        int triangleWaveColorCode = 0x010101 * triangleWaveIntensity;

        // map
        Vec3 xzPlainLookVec = new Vec3(lookVec.x, 0.0d, lookVec.z).normalize();
        double directionX = xzPlainLookVec.x;
        double directionZ = xzPlainLookVec.z;

        if (directionZ != 0.0 && directionX != 0.0) {
            Vec3 player = new Vec3(eye.x, eye.y - mc.player.getEyeHeight(), eye.z);

            for (int front = 0; front < SIZE; front++) {
                Block above = null;
                for (int y = 0; y < SIZE; y++) {
                    Block block = mc.level.getBlockState(
                            new BlockPos(
                                    player.x + directionX * (front - RANGE + 1),
                                    player.y - y + RANGE,
                                    player.z + directionZ * (front - RANGE + 1))).getBlock();

                    if (block == Blocks.AIR || block == Blocks.VOID_AIR) {
                        above = block;
                        continue;
                    }

                    Material material = block.defaultBlockState().getMaterial();
                    int pixelColor = getPixelColor(block, material);
                    if (pixelColor == 0x000000) {
                        // no drawing
                        above = block;
                        continue;
                    }
                    if (pixelColor == 0xff000000) {
                        // undefined
                        GuiComponent.fill(
                                poseStack,
                                width - MARGIN - FRAME - PIXEL * SIZE + PIXEL * front,
                                MARGIN + PIXEL * (y + 1),
                                width - MARGIN - FRAME - PIXEL * SIZE + PIXEL * (front + 1),
                                MARGIN + PIXEL * (y + 2),
                                UNDEFINED_BLOCK_COLOR);
                        GuiComponent.fill(
                                poseStack,
                                width - MARGIN - FRAME - PIXEL * SIZE + PIXEL * front + 1,
                                MARGIN + PIXEL * (y + 1) + 1,
                                width - MARGIN - FRAME - PIXEL * SIZE + PIXEL * (front + 1) - 1,
                                MARGIN + PIXEL * (y + 2) - 1,
                                AXIS_COLOR);
                        above = block;
                        continue;
                    }

                    // color gradation change
                    if (pixelColor == UNDEFINED_BLOCK_COLOR
                            || block == Blocks.WATER
                            || block == Blocks.LAVA
                            || block == Blocks.MAGMA_BLOCK
                            || block == Blocks.TORCH
                            || block == Blocks.WALL_TORCH
                            || block == Blocks.JACK_O_LANTERN
                            || block == Blocks.FURNACE
                            || block == Blocks.REDSTONE_TORCH
                            || block == Blocks.REDSTONE_WALL_TORCH) {
                        pixelColor += triangleWaveColorCode;
                    }

                    if (block == Blocks.SNOW
                            || block == Blocks.HAY_BLOCK
                            || block == Blocks.KELP
                            || block == Blocks.SCULK_VEIN
                            || block == Blocks.NETHER_SPROUTS
                            || block instanceof SlabBlock
                            || block instanceof WoolCarpetBlock
                            || block instanceof PressurePlateBlock
                            || block instanceof BaseRailBlock
                            || block instanceof TrapDoorBlock
                            || (material == Material.REPLACEABLE_PLANT && above != block)
                            || (material == Material.REPLACEABLE_WATER_PLANT && above != block)
                    ) {
                        if (above == Blocks.WATER) {
                            GuiComponent.fill(
                                    poseStack,
                                    width - MARGIN - FRAME - PIXEL * SIZE + PIXEL * front,
                                    MARGIN + PIXEL * (y + 1),
                                    width - MARGIN - FRAME - PIXEL * SIZE + PIXEL * (front + 1),
                                    MARGIN + PIXEL * (y + 2) - HALF_PIXEL,
                                    WATER_COLOR + triangleWaveColorCode);
                        }

                        // draw half tall
                        GuiComponent.fill(
                                poseStack,
                                width - MARGIN - FRAME - PIXEL * SIZE + PIXEL * front,
                                MARGIN + PIXEL * (y + 2) - HALF_PIXEL,
                                width - MARGIN - FRAME - PIXEL * SIZE + PIXEL * (front + 1),
                                MARGIN + PIXEL * (y + 2),
                                pixelColor);
                    } else {
                        // draw
                        GuiComponent.fill(
                                poseStack,
                                width - MARGIN - FRAME - PIXEL * SIZE + PIXEL * front,
                                MARGIN + PIXEL * (y + 1),
                                width - MARGIN - FRAME - PIXEL * SIZE + PIXEL * (front + 1),
                                MARGIN + PIXEL * (y + 2),
                                pixelColor);
                    }

                    above = block;
                }
            }
        }

        // cross line
        GuiComponent.fill(
                poseStack,
                width - MARGIN - PIXEL * SIZE - FRAME,  // start x
                MARGIN + FRAME + PIXEL * (RANGE + 1),  // y
                width - MARGIN - FRAME - 1,  // end x
                MARGIN + FRAME + PIXEL * (RANGE + 1) + 1,  // y
                AXIS_COLOR);
        GuiComponent.fill(
                poseStack,
                width - MARGIN - FRAME - PIXEL * RANGE - 1,  // x
                MARGIN + FRAME - 1,  // start y
                width - MARGIN - FRAME - PIXEL * RANGE,  // x
                MARGIN + FRAME + PIXEL * SIZE,  //end y
                AXIS_COLOR);
    }

    private int getPixelColor(Block block, Material material) {
        // default color
        int color;

        if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
            color = 0x745844;
        } else if (block == Blocks.WATER) {
            color = WATER_COLOR;
        } else if (block == Blocks.SAND) {
            color = 0xffe6b3;
        } else if (block == Blocks.SANDSTONE) {
            color = 0xf7d899;
        } else if (block == Blocks.GRAVEL) {
            color = 0x727272;
        } else if (block == Blocks.COAL_ORE
                || block == Blocks.COAL_BLOCK
                || block == Blocks.DEEPSLATE_COAL_ORE
        ) {
            color = 0x6f0607;
        } else if (block == Blocks.IRON_ORE
                || block == Blocks.IRON_BLOCK
                || block == Blocks.DEEPSLATE_IRON_ORE
                || block == Blocks.IRON_BARS) {
            color = 0xffffff;
        } else if (block == Blocks.COPPER_ORE
                || block == Blocks.COPPER_BLOCK
                || block == Blocks.DEEPSLATE_COPPER_ORE) {
            color = 0xbf5e49;
        } else if (block == Blocks.DIAMOND_ORE
                || block == Blocks.DIAMOND_BLOCK
                || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            color = 0x00ffff;
        } else if (block == Blocks.GOLD_ORE
                || block == Blocks.GOLD_BLOCK
                || block == Blocks.NETHER_GOLD_ORE
                || block == Blocks.DEEPSLATE_GOLD_ORE) {
            color = 0xffff00;
        } else if (block == Blocks.EMERALD_ORE
                || block == Blocks.EMERALD_BLOCK
                || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            color = 0x00ff51;
        } else if (block == Blocks.LAPIS_ORE
                || block == Blocks.LAPIS_BLOCK
                || block == Blocks.DEEPSLATE_LAPIS_ORE) {
            color = 0x000080;
        } else if (block == Blocks.REDSTONE_BLOCK
                || block == Blocks.REDSTONE_ORE
                || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            color = 0xff0000;
        } else if (block == Blocks.NETHER_QUARTZ_ORE) {
            color = 0xe0e0e0;
        } else if (block == Blocks.MAGMA_BLOCK
                || block == Blocks.LAVA) {
            color = 0xdb4d06;
        } else if (block == Blocks.CAMPFIRE) {
            color = 0xff4000;
        } else if (block == Blocks.TNT) {
            color = 0xff4000;
        } else if (block == Blocks.CACTUS) {
            color = 0x00b300;
        } else if (block instanceof BaseRailBlock) {
            color = 0xc00000;
        } else if (block == Blocks.ICE
                || block == Blocks.PACKED_ICE
                || block == Blocks.SNOW) {
            color = 0xa6e1ff;
        } else if (block == Blocks.STONE_BRICKS
                || block == Blocks.STONE_BRICK_SLAB
                || block == Blocks.STONE_BRICK_STAIRS) {
            color = 0x686868;
        } else if (block == Blocks.TORCH
                || block == Blocks.WALL_TORCH
                || block == Blocks.JACK_O_LANTERN
                || block instanceof CandleBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof AnvilBlock
                || block instanceof RodBlock) {
            color = 0xd08000;
        } else if (block == Blocks.REDSTONE_TORCH
                || block == Blocks.REDSTONE_WALL_TORCH) {
            color = 0x800000;
        } else if (material == Material.WATER_PLANT
                || material == Material.PLANT
                || material == Material.REPLACEABLE_PLANT
                || material == Material.REPLACEABLE_WATER_PLANT
                || block instanceof SaplingBlock) {
            color = 0x00b300;
        } else if (block instanceof LeavesBlock) {
            color = 0x006700;
        } else if (block instanceof AmethystBlock) {
            color = 0x5e43b0;
        } else if (block instanceof FlowerPotBlock) {
            color = 0x803839;
        } else if (block instanceof ButtonBlock) {
            return 0x000000;
        } else if (material == Material.NETHER_WOOD
                || block == Blocks.NETHER_SPROUTS
                || material == Material.SCULK) {
            // need before WOOD
            color = 0x337675;
        } else if (material == Material.WOOD
                || block instanceof RotatedPillarBlock
                || block instanceof RootsBlock) {
            color = 0x654636;
        } else if (material == Material.DIRT) {
            color = 0x745844;
        } else if (material == Material.STONE
                || block instanceof ConcretePowderBlock) {
            color = 0x686868;
        } else if (material == Material.WOOL) {
            color = 0xe0e0e0;
        } else {
            color = 0;
        }

        return color | 0xFF000000;
    }

    @Nullable
    private Block trace(Minecraft mc, Vec3 start, Vec3 direction, int distance) {
        if (mc.level == null) {
            return null;
        }
        int divisor = 7;
        Vec3 normalized = direction.normalize();
        Vec3 quotient = new Vec3(normalized.x / divisor, normalized.y / divisor, normalized.z / divisor);
        Vec3 pos = start;

        for (int i = 0; i < distance * divisor; i++) {
            Block block = mc.level.getBlockState(new BlockPos(pos)).getBlock();
            if (block != Blocks.AIR && block != Blocks.WATER) {
                return block;
            }
            pos = pos.add(quotient);
        }
        return null;
    }
}

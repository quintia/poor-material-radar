package net.quintia.mods;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import javax.annotation.Nullable;
import java.util.*;

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
        if (mc.player == null) {
            return;
        }

        // color gradation change
        double tick = (double)(System.currentTimeMillis() % 2000);  // 0 <= tick < 2000
        int triangleWaveIntensity = (int)(tick / 2000.0 * 32.0);  // 0x00 <= triangleWaveIntensity < 0x20
        if (0x10 < triangleWaveIntensity) {
            triangleWaveIntensity = 0x20 - triangleWaveIntensity;  //  0x10 > triangleWaveIntensity > 0x00
        }
        int triangleWaveColorCode = 0x010101 * triangleWaveIntensity;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getLookAngle();

        Block targeted = trace(mc, eye, lookVec, 5);
        if (targeted != null) {
            GuiComponent.drawCenteredString(poseStack, gui.getFont(),
                    targeted.getDescriptionId().substring(16),  // length of "minecaft.blocks."
                    width - MARGIN - FRAME - PIXEL * RANGE,  // x
                    MARGIN + FRAME * 2 + PIXEL * SIZE,  // y
                    0x80FFFFFF);
        } else {
            GuiComponent.drawCenteredString(poseStack, gui.getFont(),
                    fVec(eye),
                    width - MARGIN - FRAME - PIXEL * RANGE,  // x
                    MARGIN + FRAME * 2 + PIXEL * SIZE,  // y
                    0x80FFFFFF);
        }

        // frame
        GuiComponent.fill(poseStack,
                width - MARGIN - PIXEL * SIZE - FRAME * 2,
                MARGIN,
                width - MARGIN - FRAME,  // shorten right
                MARGIN + FRAME,
                FRAME_COLOR);
        GuiComponent.fill(poseStack,
                width - MARGIN - PIXEL * SIZE - FRAME * 2,
                MARGIN + FRAME,  // shorten top
                width - MARGIN - PIXEL * SIZE - FRAME,
                MARGIN + PIXEL * SIZE + FRAME * 2,
                FRAME_COLOR);
        //bottom frame
        GuiComponent.fill(poseStack,
                width - MARGIN - PIXEL * SIZE - FRAME,  //shorten left
                MARGIN + PIXEL * SIZE + FRAME,
                width - MARGIN,
                MARGIN + PIXEL * SIZE + FRAME * 2,
                FRAME_COLOR);
        //right frame
        GuiComponent.fill(poseStack,
                width - MARGIN - FRAME,
                MARGIN,
                width - MARGIN,
                MARGIN + PIXEL * SIZE + FRAME,  //shorten bottom
                FRAME_COLOR);

        // map
        Vec3 xzPlainLookVec = new Vec3(lookVec.x, 0.0d, lookVec.z).normalize();

        double directionX = xzPlainLookVec.x;
        double directionZ = xzPlainLookVec.z;
        if (directionZ != 0.0 && directionX != 0.0) {
            for (int front = 0; front < SIZE; front++) {
                Vec3 player = new Vec3(eye.x, eye.y - mc.player.getEyeHeight(), eye.z);
                Block above = null;
                for (int y = 0; y < SIZE; y++) {
                    Block block = Objects.requireNonNull(mc.level).getBlockState(
                            new BlockPos(
                                    player.x + directionX * (front - RANGE + 1),
                                    player.y - y + RANGE,
                                    player.z + directionZ * (front - RANGE + 1))).getBlock();
                    Material material = block.defaultBlockState().getMaterial();
                    int pixelColor = getPixelColor(mc, block, material);

                    // drawing expect AIR
                    if (pixelColor != 0x00FFFFFF) {
                        if (block == Blocks.WATER || block == Blocks.LAVA || pixelColor == UNDEFINED_BLOCK_COLOR) {
                            pixelColor += triangleWaveColorCode;
                        }

                        if (block == Blocks.SNOW
                                || block == Blocks.HAY_BLOCK
                                || block == Blocks.KELP
                                || block instanceof SlabBlock
                                || block instanceof WoolCarpetBlock
                                || block instanceof PressurePlateBlock
                                || (material == Material.REPLACEABLE_PLANT && above != block)
                                || (material == Material.REPLACEABLE_WATER_PLANT && above != block)
                        ) {
                            if (above == Blocks.WATER) {
                                GuiComponent.fill(
                                        poseStack,
                                        width - MARGIN - FRAME - PIXEL * SIZE + PIXEL * front,
                                        MARGIN + PIXEL * (y + 1) ,
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

    private int getPixelColor(Minecraft mc, Block block, Material material) {
        if (block == Blocks.AIR) {
            return 0x00FFFFFF;
        }

        // not transparent flag
        int color = 0xFF000000;

        if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
            color |= 0x745844;
        } else if (block == Blocks.WATER) {
            color = WATER_COLOR;
        } else if (block == Blocks.SAND) {
            color |= 0xffe6b3;
        } else if (block == Blocks.SANDSTONE) {
            color |= 0xf7d899;
        } else if (block == Blocks.GRAVEL) {
            color |= 0x727272;
        } else if (block == Blocks.COAL_ORE || block == Blocks.COAL_BLOCK) {
            color |= 0x6f0607;
        } else if (block == Blocks.IRON_ORE || block == Blocks.IRON_BLOCK) {
            color |= 0xffffff;
        } else if (block == Blocks.COPPER_ORE || block == Blocks.COPPER_BLOCK) {
            color |= 0xbf5e49;
        } else if (block == Blocks.CACTUS) {
            color |= 0x00b300;
        } else if (block == Blocks.DIAMOND_ORE || block == Blocks.DIAMOND_BLOCK) {
            color |= 0x00ffff;
        } else if (block == Blocks.GOLD_ORE || block == Blocks.GOLD_BLOCK) {
            color |= 0xffff00;
        } else if (block == Blocks.EMERALD_ORE || block == Blocks.EMERALD_BLOCK) {
            color |= 0x00ff51;
        } else if (block == Blocks.LAPIS_ORE || block == Blocks.LAPIS_BLOCK) {
            color |= 0x000080;
        } else if (block == Blocks.REDSTONE_BLOCK || block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE) {
            color |= 0xFF0000;
        } else if (block == Blocks.MAGMA_BLOCK || block == Blocks.LAVA) {
            color |= 0xdb4d06;
        } else if (block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.SNOW) {
            color |= 0xa6e1ff;
        } else if (block == Blocks.STONE_BRICKS || block == Blocks.STONE_BRICK_SLAB || block == Blocks.STONE_BRICK_STAIRS) {
            color |= 0x686868;
        } else if (block == Blocks.TORCH
                || block == Blocks.WALL_TORCH
                || block == Blocks.JACK_O_LANTERN
                || block == Blocks.FURNACE /*炉*/) {
            // Transparent blocks
            color = 0xA0ff8000;
        } else if (block == Blocks.REDSTONE_TORCH
                || block == Blocks.REDSTONE_WALL_TORCH) {
            // Transparent blocks
            color = 0xA0800000;
        } else if (material == Material.WATER_PLANT
                || material == Material.PLANT
                || material == Material.REPLACEABLE_PLANT
                || material == Material.REPLACEABLE_WATER_PLANT) {
            color |= 0x00b300;
        } else if (block instanceof LeavesBlock) {
            color |= 0x006700;
        } else if (material == Material.WOOD) {
            color |= 0x654636;
        } else if (material == Material.STONE) {
            color |= 0x686868;
        } else if (material == Material.WOOL) {
            color |= 0xFFFFFF;
        } else {
            // default color
            color = UNDEFINED_BLOCK_COLOR;
        }

        return color;
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

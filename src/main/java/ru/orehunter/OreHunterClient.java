package ru.orehunter;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public final class OreHunterClient implements ClientModInitializer {
    private static final int SEARCH_RADIUS = 16;
    private static final int SEARCH_INTERVAL = 10;
    private static final int PATH_INTERVAL = 12;
    private static final double MINE_DISTANCE = 4.0;
    private static KeyMapping toggleKey;
    private static boolean enabled;
    private static BlockPos target;
    private static BlockPos miningTarget;
    private static List<BlockPos> route = List.of();
    private static List<BlockPos> oreHighlights = List.of();
    private static int scanCooldown, pathCooldown, eatCooldown, attackCooldown, statusCooldown;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.orehunter.toggle", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J, KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(OreHunterClient::tick);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(OreHunterClient::renderOreHighlights);
    }

    private static void tick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            enabled = !enabled;
            target = null;
            miningTarget = null;
            route = List.of();
            oreHighlights = List.of();
            releaseMovement(client);
            scanCooldown = pathCooldown = 0;
            if (client.player != null) client.player.displayClientMessage(
                    Component.literal(enabled ? "OreHunter: ON" : "OreHunter: OFF"), true);
        }
        if (!enabled || client.player == null || client.level == null || client.gameMode == null) {
            if (!enabled) releaseMovement(client);
            return;
        }

        if (manualMovementPressed()) {
            releaseMovement(client);
            return;
        }

        if (eatCooldown > 0) eatCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (statusCooldown-- <= 0) {
            client.player.displayClientMessage(Component.literal(target == null ? "OreHunter: ищу руду..." :
                    "OreHunter: цель " + target.getX() + " " + target.getY() + " " + target.getZ()), true);
            statusCooldown = 30;
        }

        if (client.player.getFoodData().getFoodLevel() <= 14 && eatCooldown == 0) {
            int foodSlot = findFoodSlot(client);
            if (foodSlot >= 0) {
                releaseMovement(client);
                client.player.getInventory().setSelectedSlot(foodSlot);
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                eatCooldown = 35;
                return;
            }
        }

        Mob threat = findThreat(client);
        if (threat != null) {
            route = List.of();
            face(client, threat.position().add(0, threat.getBbHeight() * 0.65, 0));
            double distance = client.player.position().distanceTo(threat.position());
            if (distance > 3.1) {
                moveToward(client, threat.position());
            } else {
                releaseMovement(client);
                int weapon = findSwordSlot(client);
                if (weapon >= 0) client.player.getInventory().setSelectedSlot(weapon);
                if (attackCooldown == 0) {
                    client.gameMode.attack(client.player, threat);
                    client.player.swing(InteractionHand.MAIN_HAND);
                    attackCooldown = 10;
                }
            }
            return;
        }

        ItemEntity drop = findNearbyDrop(client);
        if (drop != null) {
            if (client.player.position().distanceTo(drop.position()) > 1.15) {
                moveToward(client, drop.position());
            } else {
                releaseMovement(client);
            }
            return;
        }

        if (scanCooldown-- <= 0 || !isOre(client, target)) {
            BlockPos previous = target;
            oreHighlights = findNearbyOres(client);
            target = oreHighlights.isEmpty() ? null : oreHighlights.get(0);
            scanCooldown = SEARCH_INTERVAL;
            if (!Objects.equals(previous, target)) {
                route = List.of();
                pathCooldown = 0;
                miningTarget = null;
            }
        }

        if (target == null) {
            moveToward(client, client.player.position().add(0, 0, 1));
            return;
        }

        if (isAdjacentToOre(client.player.blockPosition(), target)) {
            route = List.of();
            releaseMovement(client);
            face(client, Vec3.atCenterOf(target));
            if (client.player.position().distanceTo(Vec3.atCenterOf(target)) <= MINE_DISTANCE) {
                selectBestTool(client, client.level.getBlockState(target));
                if (client.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(target)) {
                    if (!target.equals(miningTarget)) {
                        miningTarget = target.immutable();
                        client.gameMode.startDestroyBlock(target, hit.getDirection());
                    }
                    client.gameMode.continueDestroyBlock(target, hit.getDirection());
                    client.player.swing(InteractionHand.MAIN_HAND);
                } else {
                    miningTarget = null;
                }
            }
            return;
        }

        if (pathCooldown-- <= 0 || route.isEmpty()) {
            route = findPathToOre(client, target);
            pathCooldown = PATH_INTERVAL;
        }
        followRoute(client);
    }

    private static boolean manualMovementPressed() {
        long window = GLFW.glfwGetCurrentContext();
        return window != 0 && (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS);
    }

    private static void releaseMovement(Minecraft client) {
        if (client == null) return;
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
    }

    // Move in world-space without rotating the camera (used for navigation and item pickup).
    private static void moveToward(Minecraft client, Vec3 point) {
        Vec3 delta = point.subtract(client.player.position());
        double length = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (length < 0.15) {
            releaseMovement(client);
            return;
        }
        double dx = delta.x / length, dz = delta.z / length;
        double yaw = Math.toRadians(client.player.getYRot());
        double forwardX = -Math.sin(yaw), forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw), rightZ = Math.sin(yaw);
        double forward = dx * forwardX + dz * forwardZ;
        double right = dx * rightX + dz * rightZ;
        client.options.keyUp.setDown(forward > 0.18);
        client.options.keyDown.setDown(forward < -0.18);
        client.options.keyRight.setDown(right > 0.18);
        client.options.keyLeft.setDown(right < -0.18);
    }

    private static Mob findThreat(Minecraft client) {
        return client.level.getEntitiesOfClass(Mob.class, client.player.getBoundingBox().inflate(6.0),
                mob -> mob.isAlive() && mob.getType().getCategory() == MobCategory.MONSTER)
                .stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(client.player))).orElse(null);
    }

    private static ItemEntity findNearbyDrop(Minecraft client) {
        return client.level.getEntitiesOfClass(ItemEntity.class, client.player.getBoundingBox().inflate(7.0),
                ItemEntity::isAlive).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(client.player))).orElse(null);
    }

    private static int findFoodSlot(Minecraft client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.get(DataComponents.FOOD) != null) return i;
        }
        return -1;
    }

    private static int findSwordSlot(Minecraft client) {
        int best = -1;
        float bestDamage = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.is(ItemTags.SWORDS)
                    || (stack.getMaxDamage() > 0 && stack.getDamageValue() >= stack.getMaxDamage() - 1)) continue;
            // Prefer the first durable sword; the exact damage bonus API differs
            // between game versions, so do not rely on a hand-specific bonus here.
            float score = stack.getMaxDamage() == 0 ? 1.0F :
                    1.0F - (float) stack.getDamageValue() / stack.getMaxDamage();
            if (score > bestDamage) { bestDamage = score; best = i; }
        }
        return best;
    }

    private static void selectBestTool(Minecraft client, BlockState state) {
        int bestSlot = client.player.getInventory().getSelectedSlot();
        float bestSpeed = -1.0F;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty() || (stack.getMaxDamage() > 0 && stack.getDamageValue() >= stack.getMaxDamage() - 1)) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) { bestSpeed = speed; bestSlot = i; }
        }
        client.player.getInventory().setSelectedSlot(bestSlot);
    }

    private static void face(Minecraft client, Vec3 point) {
        Vec3 delta = point.subtract(client.player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float desiredYaw = (float)(Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
        float desiredPitch = (float)(-(Mth.atan2(delta.y, horizontal) * (180.0 / Math.PI)));
        float yaw = Mth.rotLerp(0.22F, client.player.getYRot(), desiredYaw);
        float pitch = Mth.lerp(0.22F, client.player.getXRot(), desiredPitch);
        client.player.setYRot(yaw);
        client.player.setYHeadRot(yaw);
        client.player.setXRot(pitch);
    }

    private static boolean isAdjacentToOre(BlockPos feet, BlockPos ore) {
        int dx = Math.abs(feet.getX() - ore.getX());
        int dy = Math.abs(feet.getY() - ore.getY());
        int dz = Math.abs(feet.getZ() - ore.getZ());
        return dx <= 1 && dz <= 1 && dy <= 1 && (dx + dz > 0 || dy > 0);
    }

    private static List<BlockPos> findPathToOre(Minecraft client, BlockPos ore) {
        BlockPos start = client.player.blockPosition();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);
        BlockPos goal = null;
        int examined = 0;
        while (!queue.isEmpty() && examined++ < 3500) {
            BlockPos current = queue.removeFirst();
            if (isAdjacentToOre(current, ore)) { goal = current; break; }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos next = current.relative(dir).above(dy);
                    if (Math.abs(next.getX() - start.getX()) > 20
                            || Math.abs(next.getZ() - start.getZ()) > 20
                            || Math.abs(next.getY() - start.getY()) > 5
                            || !visited.add(next) || !canStandAt(client, next)) continue;
                    parent.put(next, current);
                    queue.addLast(next);
                }
            }
        }
        if (goal == null) return List.of();
        ArrayList<BlockPos> result = new ArrayList<>();
        for (BlockPos p = goal; p != null; p = parent.get(p)) result.add(p);
        Collections.reverse(result);
        return result;
    }

    private static boolean canStandAt(Minecraft client, BlockPos feet) {
        // Never plan to dig the floor directly under the player's feet.
        // A route node is valid only when it has safe solid support and a
        // two-block-high body space that can be cleared before entering.
        BlockPos floor = feet.below();
        BlockState floorState = client.level.getBlockState(floor);
        if (!floorState.getFluidState().isEmpty()
                || !floorState.isFaceSturdy(client.level, floor, Direction.UP)
                || hasNearbyLava(client, floor)) return false;
        return canClear(client, feet) && canClear(client, feet.above());
    }

    private static boolean canClear(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir()) return true;
        if (!state.getFluidState().isEmpty() || state.is(Blocks.BEDROCK)
                || state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.END_PORTAL_FRAME) || state.is(Blocks.BARRIER)
                || client.level.getBlockEntity(pos) != null) return false;
        if (hasNearbyLava(client, pos)) return false;
        return state.getDestroySpeed(client.level, pos) > 0.0F;
    }

    private static boolean hasNearbyLava(Minecraft client, BlockPos pos) {
        if (client.level.getBlockState(pos).getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) return true;
        for (Direction direction : Direction.values())
            if (client.level.getBlockState(pos.relative(direction)).getFluidState()
                    .is(net.minecraft.tags.FluidTags.LAVA)) return true;
        return false;
    }

    private static void followRoute(Minecraft client) {
        if (route.isEmpty()) {
            releaseMovement(client);
            return;
        }
        BlockPos here = client.player.blockPosition();
        while (route.size() > 1 && (route.get(1).equals(here)
                || route.get(1).distToCenterSqr(client.player.position()) < 0.55)) {
            route = new ArrayList<>(route.subList(1, route.size()));
        }
        if (route.size() <= 1) {
            releaseMovement(client);
            return;
        }
        BlockPos next = route.get(1);
        // Clear the lower block first, then the block above it. This produces
        // a one-block-wide, two-block-high tunnel without breaking the floor.
        BlockPos obstruction = null;
        if (!client.level.getBlockState(next).isAir()) obstruction = next;
        else if (!client.level.getBlockState(next.above()).isAir()) obstruction = next.above();

        if (obstruction != null) {
            if (!canClear(client, obstruction)) {
                route = List.of();
                releaseMovement(client);
                pathCooldown = 0;
                return;
            }
            releaseMovement(client);
            face(client, Vec3.atCenterOf(obstruction));
            selectBestTool(client, client.level.getBlockState(obstruction));
            if (client.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(obstruction)) {
                if (!obstruction.equals(miningTarget)) {
                    miningTarget = obstruction.immutable();
                    client.gameMode.startDestroyBlock(obstruction, hit.getDirection());
                }
                client.gameMode.continueDestroyBlock(obstruction, hit.getDirection());
                client.player.swing(InteractionHand.MAIN_HAND);
            }
            return;
        }
        miningTarget = null;
        // Re-evaluate after falling/stepping so the bot never assumes it is
        // still standing at the old Y coordinate.
        if (next.getY() < here.getY() && client.player.getY() > next.getY() + 0.35) {
            releaseMovement(client);
            return;
        }
        moveToward(client, Vec3.atCenterOf(next));
    }

    private static List<BlockPos> findNearbyOres(Minecraft client) {
        BlockPos origin = client.player.blockPosition();
        ArrayList<BlockPos> ores = new ArrayList<>();
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++)
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++)
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (isOre(client, pos)) ores.add(pos.immutable());
                }
        ores.sort(Comparator.comparingDouble(p ->
                client.player.position().distanceToSqr(Vec3.atCenterOf(p))));
        return ores;
    }

    private static void renderOreHighlights(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled || client.level == null || oreHighlights.isEmpty()) return;
        Vec3 camera = client.gameRenderer.getMainCamera().getPosition();
        context.matrices().pushPose();
        try {
            var lines = context.consumers().getBuffer(RenderTypes.lines());
            int count = Math.min(oreHighlights.size(), 32);
            for (int i = 0; i < count; i++) {
                BlockPos pos = oreHighlights.get(i);
                double x = pos.getX() - camera.x;
                double y = pos.getY() - camera.y;
                double z = pos.getZ() - camera.z;
                LevelRenderer.renderLineBox(context.matrices(), lines,
                        new AABB(x + 0.002, y + 0.002, z + 0.002,
                                x + 0.998, y + 0.998, z + 0.998),
                        0.15F, 1.0F, 0.25F, 0.95F);
            }
        } finally {
            context.matrices().popPose();
        }
    }

    private static boolean isOre(Minecraft client, BlockPos pos) {
        if (pos == null || client.level == null) return false;
        BlockState state = client.level.getBlockState(pos);
        return state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.ANCIENT_DEBRIS);
    }
}

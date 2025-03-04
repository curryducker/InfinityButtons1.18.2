package net.larsmans.infinitybuttons.block.custom.emergencybutton;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.larsmans.infinitybuttons.InfinityButtonsConfig;
import net.larsmans.infinitybuttons.sounds.InfinityButtonsSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class SafeEmergencyButton extends WallMountedBlock {
    InfinityButtonsConfig config = AutoConfig.getConfigHolder(InfinityButtonsConfig.class).getConfig();

    public static final EnumProperty<SEBStateEnum> STATE = EnumProperty.of("state", SEBStateEnum.class);

    private static final VoxelShape STONE_DOWN = Block.createCuboidShape(3, 0, 3, 13, 1, 13);
    private static final VoxelShape STONE_UP = Block.createCuboidShape(3, 15, 3, 13, 16, 13);
    private static final VoxelShape STONE_NORTH = Block.createCuboidShape(3, 3, 15, 13, 13, 16);
    private static final VoxelShape STONE_EAST = Block.createCuboidShape(0, 3, 3, 1, 13, 13);
    private static final VoxelShape STONE_SOUTH = Block.createCuboidShape(3, 3, 0, 13, 13, 1);
    private static final VoxelShape STONE_WEST = Block.createCuboidShape(15, 3, 3, 16, 13, 13);

    private static final VoxelShape FLOOR_CLOSED_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(4, 1, 4, 12, 8, 12), STONE_DOWN);
    private static final VoxelShape FLOOR_OPEN_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(5, 1, 5, 11, 5, 11), STONE_DOWN);
    private static final VoxelShape FLOOR_PRESSED_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(5, 1, 5, 11, 3, 11), STONE_DOWN);
    private static final VoxelShape CEILING_CLOSED_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(4, 8, 4, 12, 15, 12), STONE_UP);
    private static final VoxelShape CEILING_OPEN_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(5, 11, 5, 11, 15, 11), STONE_UP);
    private static final VoxelShape CEILING_PRESSED_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(5, 13, 5, 11, 15, 11), STONE_UP);
    private static final VoxelShape NORTH_CLOSED_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(4, 4, 8, 12, 12, 15), STONE_NORTH);
    private static final VoxelShape NORTH_OPEN_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(5, 5, 11, 11, 11, 15), STONE_NORTH);
    private static final VoxelShape NORTH_PRESSED_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(5, 5, 13, 11, 11, 15), STONE_NORTH);
    private static final VoxelShape EAST_CLOSED_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(1, 4, 4, 8, 12, 12), STONE_EAST);
    private static final VoxelShape EAST_OPEN_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(1, 5, 5, 5, 11, 11), STONE_EAST);
    private static final VoxelShape EAST_PRESSED_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(1, 5, 5, 3, 11, 11), STONE_EAST);
    private static final VoxelShape SOUTH_CLOSED_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(4, 4, 1, 12, 12, 8), STONE_SOUTH);
    private static final VoxelShape SOUTH_OPEN_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(5, 5, 1, 11, 11, 5), STONE_SOUTH);
    private static final VoxelShape SOUTH_PRESSED_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(5, 5, 1, 11, 11, 3), STONE_SOUTH);
    private static final VoxelShape WEST_CLOSED_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(8, 4, 4, 15, 12, 12), STONE_WEST);
    private static final VoxelShape WEST_OPEN_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(11, 5, 5, 15, 11, 11), STONE_WEST);
    private static final VoxelShape WEST_PRESSED_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(13, 5, 5, 15, 11, 11), STONE_WEST);

    public SafeEmergencyButton(FabricBlockSettings settings) {
        super(settings);
        this.setDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(FACING, Direction.NORTH)).with(STATE, SEBStateEnum.CLOSED)).with(FACE, WallMountLocation.FLOOR));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(STATE, FACING, FACE);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = state.get(FACING);
        switch (state.get(FACE)) {
            case FLOOR -> {
                switch (state.get(STATE)) {
                    case CLOSED -> {
                        return FLOOR_CLOSED_SHAPE;
                    }
                    case OPEN -> {
                        return FLOOR_OPEN_SHAPE;
                    }
                    case PRESSED -> {
                        return FLOOR_PRESSED_SHAPE;
                    }
                }
            }
            case CEILING -> {
                switch (state.get(STATE)) {
                    case CLOSED -> {
                        return CEILING_CLOSED_SHAPE;
                    }
                    case OPEN -> {
                        return CEILING_OPEN_SHAPE;
                    }
                    case PRESSED -> {
                        return CEILING_PRESSED_SHAPE;
                    }
                }
            }
            case WALL -> {
                switch (direction) {
                    case NORTH -> {
                        switch (state.get(STATE)) {
                            case CLOSED -> {
                                return NORTH_CLOSED_SHAPE;
                            }
                            case OPEN -> {
                                return NORTH_OPEN_SHAPE;
                            }
                            case PRESSED -> {
                                return NORTH_PRESSED_SHAPE;
                            }
                        }
                    }
                    case EAST -> {

                        switch (state.get(STATE)) {
                            case CLOSED -> {
                                return EAST_CLOSED_SHAPE;
                            }
                            case OPEN -> {
                                return EAST_OPEN_SHAPE;
                            }
                            case PRESSED -> {
                                return EAST_PRESSED_SHAPE;
                            }
                        }
                    }
                    case SOUTH -> {
                        switch (state.get(STATE)) {
                            case CLOSED -> {
                                return SOUTH_CLOSED_SHAPE;
                            }
                            case OPEN -> {
                                return SOUTH_OPEN_SHAPE;
                            }
                            case PRESSED -> {
                                return SOUTH_PRESSED_SHAPE;
                            }
                        }
                    }
                    case WEST -> {
                        switch (state.get(STATE)) {
                            case CLOSED -> {
                                return WEST_CLOSED_SHAPE;
                            }
                            case OPEN -> {
                                return WEST_OPEN_SHAPE;
                            }
                            case PRESSED -> {
                                return WEST_PRESSED_SHAPE;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        switch (state.get(STATE)) {
            case PRESSED -> {
                return ActionResult.CONSUME;
            }
            case OPEN -> {
                if (player.isSneaking()) {
                    this.closeCase(state, world, pos);
                    this.playToggleSound(player, world, pos, false);
                } else {
                    this.powerOn(state, world, pos);
                    this.playClickSound(player, world, pos, true);
                    if (config.alarmSound) {
                        world.playSound(player, pos, InfinityButtonsSounds.ALARM, SoundCategory.BLOCKS, 2f, 0.6f);
                    }
                    world.emitGameEvent((Entity) player, GameEvent.BLOCK_PRESS, pos);
                }
            }
            case CLOSED -> {
                if (player.isSneaking()) {
                    this.openCase(state, world, pos);
                    this.playToggleSound(player, world, pos, true);
                } else {
                    player.sendMessage(new TranslatableText("infinitybuttons.actionbar.closed_safety_button"), true);
                    return ActionResult.CONSUME;
                }
            }
        }
        return ActionResult.success(world.isClient);
    }

    public void openCase(BlockState state, World world, BlockPos pos) {
        world.setBlockState(pos, state.with(STATE, SEBStateEnum.OPEN), Block.NOTIFY_ALL);
        this.updateNeighbors(state, world, pos);
    }

    public void closeCase(BlockState state, World world, BlockPos pos) {
        world.setBlockState(pos, state.with(STATE, SEBStateEnum.CLOSED), Block.NOTIFY_ALL);
        this.updateNeighbors(state, world, pos);
    }

    public void powerOn(BlockState state, World world, BlockPos pos) {
        world.setBlockState(pos, state.with(STATE, SEBStateEnum.PRESSED), Block.NOTIFY_ALL);
        this.updateNeighbors(state, world, pos);
        world.createAndScheduleBlockTick(pos, this, 10);
    }

    protected void playClickSound(@Nullable PlayerEntity player, WorldAccess world, BlockPos pos, boolean pressed) {
        world.playSound(pressed ? player : null, pos, SoundEvents.BLOCK_BONE_BLOCK_BREAK, SoundCategory.BLOCKS, 0.75f, pressed ? 0.6f : 0.5f);
    }

    protected void playToggleSound(@Nullable PlayerEntity player, WorldAccess world, BlockPos pos, boolean pressed) {
        world.playSound(pressed ? player : null, pos, pressed ? SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN : SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 1f, 1);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (moved || state.isOf(newState.getBlock())) {
            return;
        }
        if (state.get(STATE) == SEBStateEnum.PRESSED) {
            this.updateNeighbors(state, world, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }


    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(STATE) == SEBStateEnum.PRESSED ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        if (state.get(STATE) == SEBStateEnum.PRESSED && EmergencyButton.getDirection(state) == direction) {
            return 15;
        }
        return 0;
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if(state.get(STATE) == SEBStateEnum.PRESSED) {
            world.setBlockState(pos, state.with(STATE, SEBStateEnum.OPEN), Block.NOTIFY_ALL);
            this.updateNeighbors(state, world, pos);
            this.playClickSound(null, world, pos, false);
            world.emitGameEvent(null, GameEvent.BLOCK_UNPRESS, pos);
        }
    }
    
    private void updateNeighbors(BlockState state, World world, BlockPos pos) {
        world.updateNeighborsAlways(pos, this);
        world.updateNeighborsAlways(pos.offset(EmergencyButton.getDirection(state).getOpposite()), this);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        if (config.tooltips) {
            if (Screen.hasShiftDown()) {
                tooltip.add(new TranslatableText("infinitybuttons.tooltip.safe_emergency_button").formatted(Formatting.GRAY));
            } else {
                tooltip.add(new TranslatableText("infinitybuttons.tooltip.hold_shift").formatted(Formatting.GRAY));
            }
        }
    }
}

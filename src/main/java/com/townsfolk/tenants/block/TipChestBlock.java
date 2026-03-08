package com.townsfolk.tenants.block;

import com.townsfolk.signs.world.RoomData;
import com.townsfolk.signs.world.RoomManager;
import com.townsfolk.tenants.block.entity.ModBlockEntities;
import com.townsfolk.tenants.block.entity.TipChestBlockEntity;
import com.townsfolk.tenants.entity.TenantEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TipChestBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final MapCodec<TipChestBlock> CODEC = createCodec(TipChestBlock::new);
    
    public TipChestBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }
    
    @Override
    public MapCodec<TipChestBlock> getCodec() {
        return CODEC;
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TipChestBlockEntity(pos, state);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        
        if (player.isSneaking()) {
            showHotelOverview(world, pos, player);
            return ActionResult.CONSUME;
        }
        
        NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);
        if (screenHandlerFactory != null) {
            player.openHandledScreen(screenHandlerFactory);
        }
        
        return ActionResult.CONSUME;
    }
    
    private void showHotelOverview(World world, BlockPos pos, PlayerEntity player) {
        RoomManager manager = RoomManager.getServerState((ServerWorld) world);
        List<RoomData> hotelRooms = manager.getRoomsByType("hotel");
        
        int nearbyRooms = 0;
        int occupiedRooms = 0;
        int totalDailyIncome = 0;
        int totalQuality = 0;
        int validRooms = 0;
        
        player.sendMessage(Text.literal("=== Hotel Overview ===").formatted(Formatting.GOLD), false);
        
        for (RoomData room : hotelRooms) {
            BlockPos signPos = room.getSignPos();
            double distance = Math.sqrt(pos.getSquaredDistance(signPos));
            
            if (distance <= 64) {
                nearbyRooms++;
                
                if (room.isValid()) {
                    validRooms++;
                    totalQuality += room.getQuality();
                    if (room.hasTenant()) {
                        occupiedRooms++;
                        totalDailyIncome += TenantEntity.getDailyPayment(room.getQuality());
                    }
                }
                
                String status = room.isValid() ? 
                    (room.hasTenant() ? "Occupied" : "Vacant") : "Invalid";
                Formatting color = room.isValid() ? 
                    (room.hasTenant() ? Formatting.GREEN : Formatting.YELLOW) : Formatting.RED;
                
                String tenantInfo = room.hasTenant() ? 
                    " - " + room.getTenantName() : "";
                
                player.sendMessage(Text.literal(String.format(
                    "#%d [%s] Q:%d $%d %s%s", 
                    room.getRoomNumber(), 
                    TenantEntity.getTierName(room.getQuality()).substring(0, Math.min(3, TenantEntity.getTierName(room.getQuality()).length())),
                    room.getQuality(),
                    TenantEntity.getDailyPayment(room.getQuality()),
                    status,
                    tenantInfo
                )).formatted(color), false);
            }
        }
        
        if (nearbyRooms == 0) {
            player.sendMessage(Text.literal("No rooms registered. Place wall signs on rooms with beds.").formatted(Formatting.YELLOW), false);
            return;
        }
        
        player.sendMessage(Text.literal("---").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal(String.format(
            "Rooms: %d | Occupied: %d/%d (%.0f%%)", 
            nearbyRooms, occupiedRooms, validRooms,
            validRooms > 0 ? (occupiedRooms * 100.0 / validRooms) : 0
        )).formatted(Formatting.AQUA), false);
        player.sendMessage(Text.literal(String.format(
            "Daily Income: %d emeralds | Avg Quality: %d", 
            totalDailyIncome,
            validRooms > 0 ? totalQuality / validRooms : 0
        )).formatted(Formatting.GREEN), false);
    }
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof TipChestBlockEntity chest) {
                chest.dropContents(world, pos);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}

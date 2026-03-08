package com.townsfolk.tenants.item;

import com.townsfolk.signs.world.RoomData;
import com.townsfolk.signs.world.RoomManager;
import com.townsfolk.tenants.entity.TenantEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.OpenWrittenBookS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class GuestLedgerItem extends Item {
    
    public GuestLedgerItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            openLedgerBook((ServerWorld) world, serverPlayer, hand, stack);
        }
        
        return TypedActionResult.success(stack, world.isClient);
    }
    
    private void openLedgerBook(ServerWorld world, ServerPlayerEntity player, Hand hand, ItemStack stack) {
        RoomManager manager = RoomManager.getServerState(world);
        List<RoomData> hotelRooms = manager.getRoomsByType("hotel");
        
        List<RawFilteredPair<Text>> pages = new ArrayList<>();
        
        if (hotelRooms.isEmpty()) {
            Text emptyPage = Text.empty()
                .append(Text.literal("Guest Ledger\n\n").formatted(Formatting.DARK_BLUE, Formatting.BOLD))
                .append(Text.literal("No hotel rooms registered.\n\n").formatted(Formatting.GRAY))
                .append(Text.literal("Place wall signs on rooms with beds to create hotel rooms.").formatted(Formatting.DARK_GRAY));
            pages.add(RawFilteredPair.of(emptyPage));
        } else {
            int totalRooms = hotelRooms.size();
            int validRooms = 0;
            int occupiedRooms = 0;
            int totalIncome = 0;
            
            for (RoomData room : hotelRooms) {
                if (room.isValid()) {
                    validRooms++;
                    if (room.hasTenant()) {
                        occupiedRooms++;
                        totalIncome += TenantEntity.getDailyPayment(room.getQuality());
                    }
                }
            }
            
            Text summaryPage = Text.empty()
                .append(Text.literal("Guest Ledger\n").formatted(Formatting.DARK_BLUE, Formatting.BOLD))
                .append(Text.literal("------------\n\n").formatted(Formatting.DARK_GRAY))
                .append(Text.literal("Summary\n").formatted(Formatting.DARK_GREEN, Formatting.BOLD))
                .append(Text.literal("Total Rooms: ").formatted(Formatting.BLACK))
                .append(Text.literal(totalRooms + "\n").formatted(Formatting.DARK_BLUE))
                .append(Text.literal("Valid: ").formatted(Formatting.BLACK))
                .append(Text.literal(validRooms + "\n").formatted(Formatting.DARK_GREEN))
                .append(Text.literal("Occupied: ").formatted(Formatting.BLACK))
                .append(Text.literal(occupiedRooms + "/" + validRooms + "\n\n").formatted(Formatting.GOLD))
                .append(Text.literal("Daily Income\n").formatted(Formatting.DARK_GREEN, Formatting.BOLD))
                .append(Text.literal(totalIncome + " emeralds").formatted(Formatting.DARK_AQUA));
            pages.add(RawFilteredPair.of(summaryPage));
            
            StringBuilder roomListBuilder = new StringBuilder();
            int roomsOnPage = 0;
            
            for (RoomData room : hotelRooms) {
                String status;
                String statusSymbol;
                if (!room.isValid()) {
                    status = "Invalid";
                    statusSymbol = "[X]";
                } else if (room.hasTenant()) {
                    status = room.getTenantName();
                    statusSymbol = "[*]";
                } else {
                    status = "Vacant";
                    statusSymbol = "[ ]";
                }
                
                String roomLine = String.format("%s #%d [Q%d]\n  %s\n  $%d/day\n\n",
                    statusSymbol,
                    room.getRoomNumber(),
                    room.getQuality(),
                    status,
                    TenantEntity.getDailyPayment(room.getQuality())
                );
                
                if (roomsOnPage >= 3) {
                    Text roomPage = Text.empty()
                        .append(Text.literal("Room Details\n").formatted(Formatting.DARK_BLUE, Formatting.BOLD))
                        .append(Text.literal("------------\n").formatted(Formatting.DARK_GRAY))
                        .append(Text.literal(roomListBuilder.toString()).formatted(Formatting.BLACK));
                    pages.add(RawFilteredPair.of(roomPage));
                    roomListBuilder = new StringBuilder();
                    roomsOnPage = 0;
                }
                
                roomListBuilder.append(roomLine);
                roomsOnPage++;
            }
            
            if (roomsOnPage > 0) {
                Text roomPage = Text.empty()
                    .append(Text.literal("Room Details\n").formatted(Formatting.DARK_BLUE, Formatting.BOLD))
                    .append(Text.literal("------------\n").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(roomListBuilder.toString()).formatted(Formatting.BLACK));
                pages.add(RawFilteredPair.of(roomPage));
            }
        }
        
        WrittenBookContentComponent bookContent = new WrittenBookContentComponent(
            RawFilteredPair.of("Guest Ledger"),
            "Hotel Manager",
            0,
            pages,
            true
        );
        
        stack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, bookContent);
        
        player.currentScreenHandler.sendContentUpdates();
        
        player.networkHandler.sendPacket(new OpenWrittenBookS2CPacket(hand));
    }
}

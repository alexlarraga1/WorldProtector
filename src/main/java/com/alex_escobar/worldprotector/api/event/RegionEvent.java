package com.alex_escobar.worldprotector.api.event;

import com.alex_escobar.worldprotector.core.IRegion;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public abstract class RegionEvent extends Event {

    private final IRegion region;
    private final Player player;

    public RegionEvent(IRegion region, Player player) {
        this.region = region;
        this.player = player;
    }

    public IRegion getRegion() {
        return region;
    }

    public Player getPlayer() {
        return player;
    }

    public static class CreateRegionEvent extends RegionEvent {

        public CreateRegionEvent(IRegion region, Player player) {
            super(region, player);
        }
    }

    public static class RemoveRegionEvent extends RegionEvent {

        public RemoveRegionEvent(IRegion region, Player player) {
            super(region, player);
        }
    }

    public static class UpdateRegionEvent extends RegionEvent {

        public UpdateRegionEvent(IRegion region, Player player) {
            super(region, player);
        }
    }
}

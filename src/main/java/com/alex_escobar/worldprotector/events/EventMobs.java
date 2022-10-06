package com.alex_escobar.worldprotector.events;

import com.alex_escobar.worldprotector.WorldProtector;
import com.alex_escobar.worldprotector.core.IRegion;
import com.alex_escobar.worldprotector.core.RegionFlag;
import com.alex_escobar.worldprotector.utils.MessageUtils;
import com.alex_escobar.worldprotector.utils.RegionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = WorldProtector.MODID)
public class EventMobs {

	private EventMobs(){}

	@SubscribeEvent
	public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
		Entity eventEntity = event.getEntity();
		List<IRegion> affectedRegions = RegionUtils.getHandlingRegionsFor(new BlockPos(event.getEntity().position()), event.getWorld());
		for (IRegion region : affectedRegions) {
			if (region.containsFlag(RegionFlag.SPAWNING_ALL) && eventEntity instanceof Mob) {
				event.setCanceled(true);
				return;
			}
			if (region.containsFlag(RegionFlag.SPAWNING_ANIMAL) && isAnimal(eventEntity)) {
				event.setCanceled(true);
				return;
			}
			if (region.containsFlag(RegionFlag.SPAWNING_GOLEM) && eventEntity instanceof IronGolem) {
				event.setCanceled(true);
				return;
			}
			if (region.containsFlag(RegionFlag.SPAWNING_MONSTERS) && isMonster(eventEntity)) {
				event.setCanceled(true);
				return;
			}
			if (region.containsFlag(RegionFlag.SPAWNING_XP) && eventEntity instanceof ExperienceOrb) {
				event.setCanceled(true);
				return;
			}
		}
	}

	// TODO: Test on Villagers and add extra flag
	@SubscribeEvent
	public static void onBreedingAttempt(BabyEntitySpawnEvent event) {
		Player player = event.getCausedByPlayer();
		if (player != null && !player.level.isClientSide) {
			List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(event.getParentB().position()), event.getParentB().level);
			for (IRegion region : regions) {
				if (region.containsFlag(RegionFlag.ANIMAL_BREEDING) && region.forbids(player)) {
					if (!region.isMuted()) {
						MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.mobs.breed_animals");
					}
					event.setCanceled(true);
					return;
				}
			}
		}
		// TODO: test if this is fired when animals are bred without player interaction
	}

	@SubscribeEvent
	public static void onAnimalTameAttempt(AnimalTameEvent event) {
		Animal animal = event.getAnimal();
		Player player = event.getTamer();
		if (!player.level.isClientSide) {
			List<IRegion> regions = RegionUtils.getHandlingRegionsFor(new BlockPos(animal.position()), event.getAnimal().level);
			for (IRegion region : regions) {
				if (region.containsFlag(RegionFlag.ANIMAL_TAMING) && region.forbids(player)) {
					event.setCanceled(true);
					if (!region.isMuted()) {
						MessageUtils.sendStatusMessage((ServerPlayer) player, "message.event.mobs.tame_animal");
					}
					return;
				}
			}
		}
	}

	public static boolean isAnimal(Entity entity){
		return entity instanceof Animal || entity instanceof WaterAnimal;
	}

	public static boolean isMonster(Entity entity){
		return entity instanceof Monster
				|| entity instanceof Slime
				|| entity instanceof FlyingMob
				|| entity instanceof EnderDragon
				|| entity instanceof Shulker;
	}

	private static boolean regionContainsEntity(IRegion region, Entity entity){
		return region.getArea().contains(entity.position());
	}

	@SubscribeEvent
	public static void onAttackEntityAnimal(AttackEntityEvent event) {
		Player player = event.getPlayer();
		Entity eventEntity = event.getTarget();
		List<IRegion> affectedRegions = RegionUtils.getHandlingRegionsFor(new BlockPos(event.getTarget().position()), event.getTarget().level);
		if (!event.getTarget().level.isClientSide) {
			if (isAnimal(eventEntity)) {
				for (IRegion region : affectedRegions) {
					boolean flagDamageAnimals = region.containsFlag(RegionFlag.ATTACK_ANIMALS.toString());
					if (flagDamageAnimals && regionContainsEntity(region, eventEntity) && region.forbids(player)) {
						if (!region.isMuted()) {
							MessageUtils.sendStatusMessage((ServerPlayer) player, new TranslatableComponent("message.event.mobs.hurt_animal"));
						}
						event.setCanceled(true);
					}
				}
			}

			if (isMonster(eventEntity)) {
				for (IRegion region : affectedRegions) {
					boolean flagDamageMonsters = region.containsFlag(RegionFlag.ATTACK_MONSTERS.toString());
					if (flagDamageMonsters && regionContainsEntity(region, eventEntity) && region.forbids(player)) {
						if (!region.isMuted()) {
							MessageUtils.sendStatusMessage((ServerPlayer) player, new TranslatableComponent("message.event.mobs.hurt_monster"));
						}
						event.setCanceled(true);
					}
				}
			}

			if (event.getTarget() instanceof Villager) { // exclude pesky wandering trader >:-)
				Villager villager = (Villager) event.getTarget();
				for (IRegion region : affectedRegions) {
					boolean flagDamageMonsters = region.containsFlag(RegionFlag.ATTACK_VILLAGERS.toString());
					if (flagDamageMonsters && regionContainsEntity(region, villager) && region.forbids(player)) {
						if (!region.isMuted()) {
							MessageUtils.sendStatusMessage((ServerPlayer) player, new TranslatableComponent("message.event.mobs.hurt_villager"));
						}
						event.setCanceled(true);
					}
				}
			}
		}
	}

}

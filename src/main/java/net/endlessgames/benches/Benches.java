package net.endlessgames.benches;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Stairs;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Benches
 * <br>
 * Allows the player to sit down on stairs
 * 
 * @author minnymin3
 */
public class Benches extends JavaPlugin implements Listener {

	private Set<Arrow> mounted = new HashSet<Arrow>();
	private boolean signs;
	private boolean orient;
	
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		
		saveDefaultConfig();
		this.signs = getConfig().getBoolean("signs");
		this.orient = getConfig().getBoolean("rotate");
		
		new Cleanup(this);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getClickedBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
				return;
			}
			if (checkChair(event.getClickedBlock())) {
				Player player = event.getPlayer();
				Arrow arrow = dropSeat(event.getClickedBlock(), player);
				List<Arrow> arrows = new ArrayList<Arrow>();
				for (Entity en : arrow.getNearbyEntities(0.3, 0.3, 0.3)) {
					if (en instanceof Arrow && !en.equals(arrow)) {
						arrows.add((Arrow) en);
					}
				}
				if (!arrows.isEmpty()) {
					arrow.remove();
					return;
				}
				Stairs stairs = (Stairs) event.getClickedBlock().getState().getData();
				if (this.orient) {
					Location loc = player.getLocation();
					switch (stairs.getDescendingDirection()) {
					case NORTH:
						loc.setYaw(180);
						break;
					case SOUTH:
						loc.setYaw(0);
						break;
					case EAST:
						loc.setYaw(270);
						break;
					case WEST:
						loc.setYaw(90);
						break;
					default:
						break;
					}
					player.teleport(loc);
				}
				arrow.setPassenger(player);
				mounted.add(arrow);
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getState().getData() instanceof Stairs) {
			Arrow arrow = dropSeat(event.getBlock(), event.getPlayer());
			for (Entity en : arrow.getNearbyEntities(0.3, 0.3, 0.3)) {
				if (en instanceof Arrow && en.getPassenger() != null) {
					en.remove();
				}
			}
			arrow.remove();
		}
	}

	private boolean checkChair(Block block) {
		if (block.getState().getData() instanceof Stairs) {
			Stairs stairs = (Stairs) block.getState().getData();
			if (this.signs) {
				boolean sign1 = false;
				boolean sign2 = false;
	
				if (stairs.getDescendingDirection() == BlockFace.NORTH
						|| stairs.getDescendingDirection() == BlockFace.SOUTH) {
					sign1 = checkSign(block, BlockFace.EAST);
					sign2 = checkSign(block, BlockFace.WEST);
				} else if (stairs.getDescendingDirection() == BlockFace.EAST
						|| stairs.getDescendingDirection() == BlockFace.WEST) {
					sign1 = checkSign(block, BlockFace.NORTH);
					sign2 = checkSign(block, BlockFace.SOUTH);
				}
				return sign1 && sign2;
			}
			return true;
		}
		return false;
	}

	private boolean checkSign(Block block, BlockFace face) {
		for (int i = 1; true; i++) {
			Block relative = block.getRelative(face, i);
			if (!(relative.getState().getData() instanceof Stairs)
					|| ((Stairs) relative.getState().getData())
							.getDescendingDirection() != ((Stairs) block
							.getState().getData()).getDescendingDirection()) {
				if (relative.getType() == Material.SIGN
						|| relative.getType() == Material.WALL_SIGN
						|| relative.getType() == Material.SIGN_POST) {
					return true;
				}
				return false;
			}
		}
	}

	private Arrow dropSeat(Block chair, Player player) {
		Location location = chair.getLocation().add(0.5, 0.1, 0.5);
		Arrow drop = (Arrow) location.getWorld().spawnEntity(location, EntityType.ARROW);
		drop.setVelocity(new Vector(0, 0, 0));
		drop.teleport(location);
		return drop;
	}
	
	private class Cleanup extends BukkitRunnable {
		
		public Cleanup(Plugin plugin) {
			this.runTaskTimer(plugin, 10, 10);
		}
		
		@Override
		public void run() {
			for (Arrow arrow : mounted) {
				if (arrow.getPassenger() == null) {
					arrow.remove();
				}
			}
		}
		
	}

}

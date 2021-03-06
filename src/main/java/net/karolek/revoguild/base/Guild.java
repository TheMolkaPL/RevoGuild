package net.karolek.revoguild.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import lombok.Data;
import net.karolek.revoguild.GuildPlugin;
import net.karolek.revoguild.data.Config;
import net.karolek.revoguild.store.Entry;
import net.karolek.revoguild.utils.TimeUtil;
import net.karolek.revoguild.utils.Util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@Data
public class Guild implements Entry {

	private final String		tag;
	private final String		name;
	private UUID				owner;
	private UUID				leader;
	private final Cuboid		cuboid;
	private final Treasure	treasure;
	private Location			home;
	private final long		createTime;
	private long				expireTime;
	private long				lastExplodeTime;
	private long				lastTakenLifeTime;
	private int					lives;
	private boolean			pvp;
	private boolean			preDeleted;
	private final Set<UUID>	members			= new HashSet<UUID>();
	private final Set<UUID>	treasureUsers	= new HashSet<UUID>();
	private final Set<UUID>	invites			= new HashSet<UUID>();

	public Guild(String tag, String name, Player owner) {
		this.tag = tag;
		this.name = name;
		this.owner = owner.getUniqueId();
		this.leader = owner.getUniqueId();
		this.cuboid = new Cuboid(owner.getWorld().getName(), owner.getLocation().getBlockX(), owner.getLocation().getBlockZ(), 24);
		this.treasure = new Treasure(this);
		this.home = owner.getLocation();
		this.createTime = System.currentTimeMillis();
		this.expireTime = System.currentTimeMillis() + TimeUtil.WEEK.getTime(Config.TIME_START);
		this.lastExplodeTime = System.currentTimeMillis() - TimeUtil.SECOND.getTime(Config.TNT_CANTBUILD_TIME);
		this.lastTakenLifeTime = System.currentTimeMillis();
		this.lives = Config.UPTAKE_LIVES_AMOUNT;
		this.pvp = false;
		this.preDeleted = false;
	}

	public Guild(ResultSet rs) throws SQLException {
		this.tag = rs.getString("tag");
		this.name = rs.getString("name");
		this.owner = Util.getUUID(rs.getString("owner"));
		this.leader = Util.getUUID(rs.getString("leader"));
		this.cuboid = new Cuboid(rs.getString("cuboidWorld"), rs.getInt("cuboidX"), rs.getInt("cuboidZ"), rs.getInt("cuboidSize"));
		this.treasure = new Treasure(this, GuildPlugin.getStore().query("SELECT * FROM `{P}treasures` WHERE `tag` = '" + this.tag + "'"));
		this.home = new Location(Bukkit.getWorld(rs.getString("homeWorld")), rs.getInt("homeX"), rs.getInt("homeY"), rs.getInt("homeZ"));
		this.createTime = rs.getLong("createTime");
		this.expireTime = rs.getLong("expireTime");
		this.lastExplodeTime = System.currentTimeMillis() - TimeUtil.SECOND.getTime(Config.TNT_CANTBUILD_TIME);
		this.lastTakenLifeTime = rs.getLong("lastTakenLifeTime");
		this.lives = rs.getInt("lives");
		this.pvp = (rs.getInt("pvp") == 1);
		this.preDeleted = false;

		ResultSet r = GuildPlugin.getStore().query("SELECT * FROM `{P}members` WHERE `tag` = '" + this.tag + "'");
		while (r.next())
			this.members.add(Util.getUUID(r.getString("uuid")));

		ResultSet r1 = GuildPlugin.getStore().query("SELECT * FROM `{P}treasure_users` WHERE `tag` = '" + this.tag + "'");
		while (r1.next())
			this.treasureUsers.add(Util.getUUID(r1.getString("uuid")));

	}

	public void openTreasure(Player p) {
		Treasure bp = this.treasure;
		Inventory inv = bp.getInv();
		inv.clear();
		inv.setContents(bp.getItems());
		p.openInventory(inv);
		p.playSound(p.getLocation(), Sound.CHEST_OPEN, 1.0F, 1.0F);
	}

	public void closeTreasure(Player p, Inventory i) {
		Treasure bp = this.treasure;
		bp.setItems(i.getContents());
		bp.update(false);
		p.playSound(p.getLocation(), Sound.CHEST_CLOSE, 1.0F, 1.0F);
	}

	public Set<Player> getOnlineMembers() {
		Set<Player> online = new HashSet<Player>();
		for (UUID u : this.members) {
			Player p = Bukkit.getPlayer(u);
			if (p != null)
				online.add(p);
		}
		return online;
	}

	public void setOwner(UUID u) {
		this.owner = u;
		GuildPlugin.getStore().update(false, "UPDATE `{P}guilds` SET `owner` = '" + u + "' WHERE `tag` = '" + this.tag + "'");
	}

	public void setLeader(UUID u) {
		this.leader = u;
		GuildPlugin.getStore().update(false, "UPDATE `{P}guilds` SET `leader` = '" + u + "' WHERE `tag` = '" + this.tag + "'");
	}

	public boolean isOwner(UUID u) {
		return this.owner.equals(u);
	}

	public boolean isLeader(UUID u) {
		if (isOwner(u))
			return true;
		return this.leader.equals(u);
	}

	public boolean isMember(UUID u) {
		return this.members.contains(u);
	}

	public boolean isTreasureUser(UUID u) {
		return this.treasureUsers.contains(u);
	}

	public void addTreasureUser(UUID u) {
		GuildPlugin.getStore().update(false, "INSERT INTO `{P}treasure_users` (`id`,`uuid`,`tag`) VALUES(NULL, '" + u + "', '" + this.tag + "')");
		this.treasureUsers.add(u);
	}

	public void removeTreasureUser(UUID u) {
		GuildPlugin.getStore().update(false, "DELETE FROM `{P}treasure_users` WHERE `uuid` = '" + u + "' AND `tag` = '" + this.tag + "'");
		this.treasureUsers.remove(u);
	}

	public boolean hasInvite(UUID u) {
		return this.invites.contains(u);
	}

	public boolean addInvite(UUID u) {
		if (hasInvite(u))
			return false;
		return this.invites.add(u);
	}

	public boolean removeInvite(UUID u) {
		return this.invites.remove(u);
	}

	public boolean addMember(UUID u) {
		if (!hasInvite(u))
			return false;
		if (isMember(u))
			return false;
		removeInvite(u);
		this.members.add(u);
		GuildPlugin.getStore().update(false, "INSERT INTO `{P}members` (`id`,`uuid`,`tag`) VALUES(NULL, '" + u + "', '" + this.tag + "')");
		return true;
	}

	public boolean removeMember(UUID u) {
		if (!isMember(u))
			return false;
		this.members.remove(u);
		removeTreasureUser(u);
		GuildPlugin.getStore().update(false, "DELETE FROM `{P}members` WHERE `uuid` = '" + u + "' AND `tag` = '" + this.tag + "'");

		return true;
	}

	@Override
	public void insert() {
		String u = "INSERT INTO `{P}guilds` (`id`,`tag`,`name`,`owner`,`leader`,`cuboidWorld`,`cuboidX`,`cuboidZ`,`cuboidSize`,`homeWorld`,`homeX`,`homeY`,`homeZ`,`lives`,`createTime`,`expireTime`,`lastTakenLifeTime`,`pvp`) VALUES(NULL, '" + this.tag + "','" + this.name + "','" + this.owner + "','" + this.leader + "','" + this.cuboid.getWorld().getName() + "','" + this.cuboid.getCenterX() + "','" + this.cuboid.getCenterZ() + "','" + this.cuboid.getSize() + "','" + this.home.getWorld().getName() + "','" + this.home.getBlockX() + "','" + this.home.getBlockY() + "','" + this.home.getBlockZ() + "','" + this.lives + "','" + this.createTime + "','" + this.expireTime + "','" + this.lastTakenLifeTime + "','" + (this.pvp ? 1 : 0) + "')";
		// GuildPlugin.getStore().update(false,
		// "INSERT INTO `{P}guilds` SET `tag` = '" + this.tag + "',  `name` = '" +
		// this.name + "',  `owner` = '" + this.owner + "',  `leader` = '" +
		// this.leader + "',  `cuboidWorld` = '" +
		// this.cuboid.getWorld().getName() + "',  `cuboidX` = '" +
		// this.cuboid.getCenterX() + "',  `cuboidZ` = '" +
		// this.cuboid.getCenterZ() + "',  `cuboidSize` = '" +
		// this.cuboid.getSize() + "',  `homeWorld` = '" +
		// this.home.getWorld().getName() + "',  `homeX` = '" +
		// this.home.getBlockX() + "',  `homeY` = '" + this.home.getBlockY() +
		// "',  `homeZ` = '" + this.home.getBlockZ() + "',  `lives` = '" +
		// this.lives + "', `createTime` = '" + this.createTime +
		// "',  `expireTime` = '" + this.expireTime +
		// "',  `lastTakenLifeTime` = '" + this.lastTakenLifeTime + "', `pvp` = '"
		// + (this.pvp ? 1 : 0) + "'");
		GuildPlugin.getStore().update(false, u);
	}

	@Override
	public void update(boolean now) {
		String update = "UPDATE `{P}guilds` SET `owner`='" + this.owner + "', `leader`='" + this.leader + "', `cuboidWorld`='" + this.cuboid.getWorld().getName() + "', `cuboidX`='" + this.cuboid.getCenterX() + "', `cuboidZ`='" + this.cuboid.getCenterZ() + "', `cuboidSize`='" + this.cuboid.getSize() + "', `homeWorld`='" + this.home.getWorld().getName() + "', `homeX`='" + this.home.getBlockX() + "', `homeY`='" + this.home.getBlockY() + "', `homeZ`='" + this.home.getBlockZ() + "', `createTime`='" + this.createTime + "', `expireTime`='" + this.expireTime + "', `lastTakenLifeTime` = '" + this.lastTakenLifeTime + "', `lives` = '" + this.lives + "', `pvp`='" + (this.pvp ? 1 : 0) + "' WHERE `tag`='" + this.tag + "'";
		if (now)
			GuildPlugin.getStore().update(now, update);

	}

	@Override
	public void delete() {
		GuildPlugin.getStore().update(true, "DELETE FROM `{P}guilds` WHERE `tag` = '" + this.tag + "'");
		GuildPlugin.getStore().update(true, "DELETE FROM `{P}members` WHERE `tag` = '" + this.tag + "'");
		GuildPlugin.getStore().update(true, "DELETE FROM `{P}treasures` WHERE `tag` = '" + this.tag + "'");
		GuildPlugin.getStore().update(true, "DELETE FROM `{P}treasure_users` WHERE `tag` = '" + this.tag + "'");
	}

}

package net.karolek.revoguild.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import lombok.Data;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.karolek.revoguild.GuildPlugin;
import net.karolek.revoguild.data.Config;
import net.karolek.revoguild.store.Entry;
import net.karolek.revoguild.utils.ItemSerializer;
import net.karolek.revoguild.utils.Util;

@Data
public class Treasure implements Entry {

	private Guild			owner;
	private Inventory		inv	= Bukkit.createInventory(null, 9 * Config.TREASURE_ROWS, Util.fixColor(Config.TREASURE_TITLE));
	private ItemStack[]	items	= new ItemStack[9 * Config.TREASURE_ROWS];

	public Treasure(Guild g) {
		this.owner = g;
		insert();
	}

	public Treasure(Guild g, ResultSet rs) throws SQLException {
		rs.next();
		this.owner = g;
		ItemStack[] items = ItemSerializer.stringToItems(rs.getString("content"));
		if (items.length > this.items.length) {
			this.items = Arrays.copyOfRange(items, 0, this.items.length);
			for (ItemStack item : Arrays.copyOfRange(items, this.items.length + 1, items.length))
				if (item != null)
					owner.getHome().getWorld().dropItem(owner.getHome(), item);
		}
	}

	public String getContent() {
		return ItemSerializer.itemsToString(this.items);
	}

	@Override
	public void insert() {
		GuildPlugin.getStore().update(true, "INSERT INTO `{P}treasures` (`id`,`tag`,`content`) VALUES(NULL, '" + this.owner.getTag() + "', '" + getContent() + "')");
	}

	@Override
	public void update(boolean now) {
		String query = "UPDATE `{P}treasures` SET `content` ='" + getContent() + "' WHERE `tag` = '" + this.owner.getTag() + "'";

		GuildPlugin.getStore().update(now, query);

	}

	@Override
	public void delete() {
		GuildPlugin.getStore().update(true, "DELETE FROM `{P}treasures` WHERE `tag` = '" + this.owner.getTag() + "'");
	}

}

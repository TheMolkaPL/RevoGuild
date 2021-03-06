package net.karolek.revoguild.commands.user;

import java.util.List;

import net.karolek.revoguild.base.Guild;
import net.karolek.revoguild.commands.SubCommand;
import net.karolek.revoguild.data.Config;
import net.karolek.revoguild.data.Lang;
import net.karolek.revoguild.manager.Manager;
import net.karolek.revoguild.utils.ItemUtil;
import net.karolek.revoguild.utils.Util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ResizeCommand extends SubCommand {

	public ResizeCommand() {
		super("powieksz", "powiekszanie terenu gildii", "", "rg.cmd.user.resize", "resize");
	}

	@Override
	public boolean onCommand(Player p, String[] args) {
		Guild g = Manager.GUILD.getGuild(p);

		if (g == null)
			return Util.sendMsg(p, Lang.ERROR_DONT_HAVE_GUILD);

		if (!g.isOwner(p.getUniqueId()))
			return Util.sendMsg(p, Lang.ERROR_NOT_OWNER);

		int modifier = (g.getCuboid().getSize() - Config.SIZE_START) / 5 + 1;
		List<ItemStack> items = ItemUtil.getItems(Config.COST_ADDSIZE, modifier);
		
		if (!ItemUtil.checkItems(items, p))
			return Util.sendMsg(p, Lang.ERROR_DONT_HAVE_ITEMS);

		if (!g.getCuboid().addSize())
			return Util.sendMsg(p, Lang.ERROR_MAX_SIZE);

		ItemUtil.removeItems(items, p);
		g.update(false);
		
		return Util.sendMsg(p, Lang.INFO_RESIZED);
	}
}

package net.karolek.revoguild.commands.user;


import net.karolek.revoguild.base.Guild;
import net.karolek.revoguild.commands.SubCommand;
import net.karolek.revoguild.data.Lang;
import net.karolek.revoguild.manager.Manager;
import net.karolek.revoguild.utils.Util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class InviteCommand extends SubCommand {

	public InviteCommand() {
		super("zapros", "zapraszanie graczy do gildii", "<gracz>", "sguilds.cmd.user.invite", "dodaj", "invite");
	}

	@Override
	public boolean onCommand(Player p, String[] args) {

		if (args.length != 1)
			return Util.sendMsg(p, Lang.parse(Lang.CMD_CORRECT_USAGE, this));

		Guild g = Manager.GUILD.getGuild(p);
		
		if (g == null)
			return Util.sendMsg(p, Lang.ERROR_DONT_HAVE_GUILD);

		if (!g.isLeader(p.getUniqueId()))
			return Util.sendMsg(p, Lang.ERROR_NOT_LEADER);

		@SuppressWarnings("deprecation")
		Player o = Bukkit.getPlayer(args[0]);

		if (o == null)
			return Util.sendMsg(p, Lang.ERROR_CANT_FIND_PLAYER);

		if (g.isMember(o.getUniqueId()))
			return Util.sendMsg(p, Lang.ERROR_PLAYER_IS_MEMBER);

		if (!g.addInvite(o.getUniqueId())) {
			g.removeInvite(o.getUniqueId());
			Util.sendMsg(p, Lang.parse(Lang.INFO_INVITE_BACK, o));
			return Util.sendMsg(o, Lang.parse(Lang.INFO_INVITE_CANCEL, g));
		}
		
		Util.sendMsg(p, Lang.parse(Lang.INFO_INVITE_SEND, o));
		return Util.sendMsg(o, Lang.parse(Lang.INFO_INVITE_NEW, g));


	}

}

/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.admincommandhandlers;


import org.l2j.gameserver.Config;
import org.l2j.gameserver.handler.IAdminCommandHandler;
import org.l2j.gameserver.model.L2Object;
import org.l2j.gameserver.model.L2World;
import org.l2j.gameserver.model.actor.L2Character;
import org.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.l2j.gameserver.network.SystemMessageId;
import org.l2j.gameserver.util.BuilderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles following admin commands: - heal = restores HP/MP/CP on target, name or radius
 * @version $Revision: 1.2.4.5 $ $Date: 2005/04/11 10:06:06 $ Small typo fix by Zoey76 24/02/2011
 */
public class AdminHeal implements IAdminCommandHandler
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AdminRes.class);
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_heal"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		
		if (command.equals("admin_heal"))
		{
			handleHeal(activeChar);
		}
		else if (command.startsWith("admin_heal"))
		{
			try
			{
				final String healTarget = command.substring(11);
				handleHeal(activeChar, healTarget);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				if (Config.DEVELOPER)
				{
					LOGGER.warn("Heal error: " + e);
				}
				BuilderUtil.sendSysMessage(activeChar, "Incorrect target/radius specified.");
			}
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private void handleHeal(L2PcInstance activeChar)
	{
		handleHeal(activeChar, null);
	}
	
	private void handleHeal(L2PcInstance activeChar, String player)
	{
		
		L2Object obj = activeChar.getTarget();
		if (player != null)
		{
			final L2PcInstance plyr = L2World.getInstance().getPlayer(player);
			
			if (plyr != null)
			{
				obj = plyr;
			}
			else
			{
				try
				{
					final int radius = Integer.parseInt(player);
					L2World.getInstance().forEachVisibleObject(activeChar, L2Character.class, character ->
					{
						character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());
						if (character.isPlayer())
						{
							character.setCurrentCp(character.getMaxCp());
						}
					});
					
					BuilderUtil.sendSysMessage(activeChar, "Healed within " + radius + " unit radius.");
					return;
				}
				catch (NumberFormatException nbe)
				{
				}
			}
		}
		if (obj == null)
		{
			obj = activeChar;
		}
		if (obj.isCharacter())
		{
			final L2Character target = (L2Character) obj;
			target.setCurrentHpMp(target.getMaxHp(), target.getMaxMp());
			if (target.isPlayer())
			{
				target.setCurrentCp(target.getMaxCp());
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
		}
	}
}

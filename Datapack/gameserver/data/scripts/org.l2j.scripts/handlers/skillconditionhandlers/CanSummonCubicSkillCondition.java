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
package handlers.skillconditionhandlers;

import org.l2j.gameserver.model.L2Object;
import org.l2j.gameserver.model.StatsSet;
import org.l2j.gameserver.model.actor.L2Character;
import org.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.l2j.gameserver.model.skills.ISkillCondition;
import org.l2j.gameserver.model.skills.Skill;

/**
 * @author UnAfraid
 */
public class CanSummonCubicSkillCondition implements ISkillCondition
{
	public CanSummonCubicSkillCondition(StatsSet params)
	{
	}
	
	@Override
	public boolean canUse(L2Character caster, Skill skill, L2Object target)
	{
		if (!caster.isPlayer() || caster.isAlikeDead() || caster.getActingPlayer().inObserverMode())
		{
			return false;
		}
		
		final L2PcInstance player = caster.getActingPlayer();
		if (player.inObserverMode() || player.isMounted() || player.isSpawnProtected() || player.isTeleportProtected())
		{
			return false;
		}
		
		return true;
	}
}

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

import org.l2j.gameserver.mobius.gameserver.enums.SkillConditionAffectType;
import org.l2j.gameserver.mobius.gameserver.enums.SkillConditionPercentType;
import org.l2j.gameserver.mobius.gameserver.model.L2Object;
import org.l2j.gameserver.mobius.gameserver.model.StatsSet;
import org.l2j.gameserver.mobius.gameserver.model.actor.L2Character;
import org.l2j.gameserver.mobius.gameserver.model.skills.ISkillCondition;
import org.l2j.gameserver.mobius.gameserver.model.skills.Skill;

/**
 * @author UnAfraid
 */
public class RemainHpPerSkillCondition implements ISkillCondition
{
	private final int _amount;
	private final SkillConditionPercentType _percentType;
	private final SkillConditionAffectType _affectType;
	
	public RemainHpPerSkillCondition(StatsSet params)
	{
		_amount = params.getInt("amount");
		_percentType = params.getEnum("percentType", SkillConditionPercentType.class);
		_affectType = params.getEnum("affectType", SkillConditionAffectType.class);
	}
	
	@Override
	public boolean canUse(L2Character caster, Skill skill, L2Object target)
	{
		switch (_affectType)
		{
			case CASTER:
			{
				return _percentType.test(caster.getCurrentHpPercent(), _amount);
			}
			case TARGET:
			{
				if ((target != null) && target.isCharacter())
				{
					return _percentType.test(((L2Character) target).getCurrentHpPercent(), _amount);
				}
				break;
			}
		}
		return false;
	}
}
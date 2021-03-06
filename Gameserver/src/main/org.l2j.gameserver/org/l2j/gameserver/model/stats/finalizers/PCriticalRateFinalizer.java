package org.l2j.gameserver.model.stats.finalizers;

import org.l2j.gameserver.Config;
import org.l2j.gameserver.model.actor.L2Character;
import org.l2j.gameserver.model.items.L2Item;
import org.l2j.gameserver.model.stats.BaseStats;
import org.l2j.gameserver.model.stats.IStatsFunction;
import org.l2j.gameserver.model.stats.Stats;

import java.util.Optional;

/**
 * @author UnAfraid
 */
public class PCriticalRateFinalizer implements IStatsFunction {
    @Override
    public double calc(L2Character creature, Optional<Double> base, Stats stat) {
        throwIfPresent(base);

        double baseValue = calcWeaponBaseValue(creature, stat);
        if (creature.isPlayer()) {
            // Enchanted legs bonus
            baseValue += calcEnchantBodyPart(creature, L2Item.SLOT_LEGS);
        }
        final double dexBonus = creature.getDEX() > 0 ? BaseStats.DEX.calcBonus(creature) : 1.;
        return validateValue(creature, Stats.defaultValue(creature, stat, baseValue * dexBonus * 10), 0, Config.MAX_PCRIT_RATE);
    }

    @Override
    public double calcEnchantBodyPartBonus(int enchantLevel, boolean isBlessed) {
        if (isBlessed) {
            return (0.5 * Math.max(enchantLevel - 3, 0)) + (0.5 * Math.max(enchantLevel - 6, 0));
        }

        return (0.34 * Math.max(enchantLevel - 3, 0)) + (0.34 * Math.max(enchantLevel - 6, 0));
    }
}

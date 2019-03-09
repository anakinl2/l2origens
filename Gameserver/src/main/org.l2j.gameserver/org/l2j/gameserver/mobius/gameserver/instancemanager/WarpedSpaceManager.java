package org.l2j.gameserver.mobius.gameserver.instancemanager;

import org.l2j.gameserver.mobius.gameserver.model.Location;
import org.l2j.gameserver.mobius.gameserver.model.actor.L2Character;
import org.l2j.gameserver.mobius.gameserver.model.holders.WarpedSpaceHolder;
import org.l2j.gameserver.mobius.gameserver.model.instancezone.Instance;
import org.l2j.gameserver.mobius.gameserver.util.Util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Sdw
 */
public class WarpedSpaceManager
{
    private volatile ConcurrentHashMap<L2Character, WarpedSpaceHolder> _warpedSpace = null;

    public void addWarpedSpace(L2Character creature, int radius)
    {
        if (_warpedSpace == null)
        {
            synchronized (this)
            {
                if (_warpedSpace == null)
                {
                    _warpedSpace = new ConcurrentHashMap<>();
                }
            }
        }
        _warpedSpace.put(creature, new WarpedSpaceHolder(creature, radius));
    }

    public void removeWarpedSpace(L2Character creature)
    {
        _warpedSpace.remove(creature);
    }

    public boolean checkForWarpedSpace(Location origin, Location destination, Instance instance)
    {
        if (_warpedSpace != null)
        {
            for (WarpedSpaceHolder holder : _warpedSpace.values())
            {
                final L2Character creature = holder.getCreature();
                if (creature.getInstanceWorld() != instance)
                {
                    continue;
                }
                final int radius = creature.getTemplate().getCollisionRadius();
                final boolean originInRange = Util.calculateDistance(creature, origin, false, false) <= (holder.getRange() + radius);
                final boolean destinationInRange = Util.calculateDistance(creature, destination, false, false) <= (holder.getRange() + radius);
                return destinationInRange ? !originInRange : originInRange;
            }
        }
        return false;
    }

    public static WarpedSpaceManager getInstance()
    {
        return SingletonHolder._instance;
    }

    private static class SingletonHolder
    {
        protected static final WarpedSpaceManager _instance = new WarpedSpaceManager();
    }
}
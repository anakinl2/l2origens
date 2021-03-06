package org.l2j.gameserver.model;

import org.l2j.commons.threading.ThreadPoolManager;
import org.l2j.gameserver.model.actor.L2Character;
import org.l2j.gameserver.model.events.EventDispatcher;
import org.l2j.gameserver.model.events.impl.character.OnCreatureSee;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Predicate;

/**
 * @author UnAfraid
 */
public class CreatureContainer {
    private final Set<Integer> _seen = ConcurrentHashMap.newKeySet();
    private final L2Character _owner;
    private final int _range;
    private ScheduledFuture<?> _task;
    private Predicate<L2Character> _condition = null;

    public CreatureContainer(L2Character owner, int range, Predicate<L2Character> condition) {
        _owner = owner;
        _range = range;
        _condition = condition;
        start();
    }

    public L2Character getOwner() {
        return _owner;
    }

    public int getRange() {
        return _range;
    }

    /**
     * Starts the task that scans for new creatures
     */
    public void start() {
        if ((_task == null) || _task.isDone()) {
            _task = ThreadPoolManager.getInstance().scheduleAtFixedRate(this::update, 1000, 1000);
        }
    }

    /**
     * @return {@code false} if the task could not be cancelled, typically because it has already completed normally; {@code true} otherwise
     */
    public boolean stop() {
        return (_task != null) && !_task.isDone() && _task.cancel(false);
    }

    /**
     * Resets the creatures container, all previously seen creature will be discarded and next time update method is called will notify for each creature that owner sees!
     */
    public void reset() {
        _seen.clear();
    }

    /**
     * Scans around the npc and notifies about new creature owner seen
     */
    private void update() {
        final Set<Integer> verified = new HashSet<>();
        L2World.getInstance().forEachVisibleObjectInRange(_owner, L2Character.class, _range, creature ->
        {
            if ((_condition == null) || _condition.test(creature)) {
                if (_seen.add(creature.getObjectId())) {
                    EventDispatcher.getInstance().notifyEventAsync(new OnCreatureSee(_owner, creature), _owner);
                }
                verified.add(creature.getObjectId());
            }
        });

        _seen.retainAll(verified);
    }
}

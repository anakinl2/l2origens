package org.l2j.gameserver.util;

import org.l2j.commons.util.Rnd;
import org.l2j.gameserver.Config;
import org.l2j.commons.threading.ThreadPoolManager;
import org.l2j.gameserver.data.xml.impl.NpcData;
import org.l2j.gameserver.model.Location;
import org.l2j.gameserver.model.actor.L2Character;
import org.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import org.l2j.gameserver.model.actor.templates.L2NpcTemplate;
import org.l2j.gameserver.model.holders.MinionHolder;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author luisantonioa, DS, Mobius
 */
public class MinionList {
    protected final L2MonsterInstance _master;
    private final List<L2MonsterInstance> _spawnedMinions = new CopyOnWriteArrayList<>();

    public MinionList(L2MonsterInstance pMaster) {
        if (pMaster == null) {
            throw new NullPointerException("MinionList: master is null");
        }
        _master = pMaster;
    }

    /**
     * Init a Minion and add it in the world as a visible object.<BR>
     * <BR>
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Get the template of the Minion to spawn</li>
     * <li>Create and Init the Minion and generate its Identifier</li>
     * <li>Set the Minion HP, MP and Heading</li>
     * <li>Set the Minion leader to this RaidBoss</li>
     * <li>Init the position of the Minion and add it in the world as a visible object</li><BR>
     * <BR>
     *
     * @param master   L2MonsterInstance used as master for this minion
     * @param minionId The L2NpcTemplate Identifier of the Minion to spawn
     * @return
     */
    public static L2MonsterInstance spawnMinion(L2MonsterInstance master, int minionId) {
        // Get the template of the Minion to spawn
        final L2NpcTemplate minionTemplate = NpcData.getInstance().getTemplate(minionId);
        if (minionTemplate == null) {
            return null;
        }

        return initializeNpcInstance(master, new L2MonsterInstance(minionTemplate));
    }

    protected static L2MonsterInstance initializeNpcInstance(L2MonsterInstance master, L2MonsterInstance minion) {
        minion.stopAllEffects();
        minion.setIsDead(false);
        minion.setDecayed(false);

        // Set the Minion HP, MP and Heading
        minion.setCurrentHpMp(minion.getMaxHp(), minion.getMaxMp());
        minion.setHeading(master.getHeading());

        // Set the Minion leader to this RaidBoss
        minion.setLeader(master);

        // move monster to masters instance
        minion.setInstance(master.getInstanceWorld());

        // Init the position of the Minion and add it in the world as a visible object
        final int offset = 200;
        final int minRadius = (int) master.getCollisionRadius() + 30;

        int newX = Rnd.get(minRadius * 2, offset * 2); // x
        int newY = Rnd.get(newX, offset * 2); // distance
        newY = (int) Math.sqrt((newY * newY) - (newX * newX)); // y
        if (newX > (offset + minRadius)) {
            newX = (master.getX() + newX) - offset;
        } else {
            newX = (master.getX() - newX) + minRadius;
        }
        if (newY > (offset + minRadius)) {
            newY = (master.getY() + newY) - offset;
        } else {
            newY = (master.getY() - newY) + minRadius;
        }

        minion.spawnMe(newX, newY, master.getZ());

        return minion;
    }

    /**
     * @return list of the spawned (alive) minions.
     */
    public List<L2MonsterInstance> getSpawnedMinions() {
        return _spawnedMinions;
    }

    /**
     * Manage the spawn of Minions.<BR>
     * <BR>
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Get the Minion data of all Minions that must be spawn</li>
     * <li>For each Minion type, spawn the amount of Minion needed</li><BR>
     * <BR>
     *
     * @param minions
     */
    public final void spawnMinions(List<MinionHolder> minions) {
        if (_master.isAlikeDead() || (minions == null)) {
            return;
        }

        int minionCount;
        int minionId;
        int minionsToSpawn;
        for (MinionHolder minion : minions) {
            minionCount = minion.getCount();
            minionId = minion.getId();

            minionsToSpawn = minionCount - countSpawnedMinionsById(minionId);
            if (minionsToSpawn > 0) {
                for (int i = 0; i < minionsToSpawn; i++) {
                    spawnMinion(minionId);
                }
            }
        }
    }

    /**
     * Called on the minion spawn and added them in the list of the spawned minions.
     *
     * @param minion
     */
    public void onMinionSpawn(L2MonsterInstance minion) {
        _spawnedMinions.add(minion);
    }

    /**
     * Called on the master death/delete.
     *
     * @param force - When true, force delete of the spawned minions. By default minions are deleted only for raidbosses.
     */
    public void onMasterDie(boolean force) {
        if (_master.isRaid() || force || Config.FORCE_DELETE_MINIONS) {
            if (!_spawnedMinions.isEmpty()) {
                for (L2MonsterInstance minion : _spawnedMinions) {
                    if (minion != null) {
                        minion.setLeader(null);
                        minion.deleteMe();
                    }
                }
                _spawnedMinions.clear();
            }
        }
    }

    /**
     * Called on the minion death/delete. Removed minion from the list of the spawned minions and reuse if possible.
     *
     * @param minion
     * @param respawnTime (ms) enable respawning of this minion while master is alive. -1 - use default value: 0 (disable) for mobs and config value for raids.
     */
    public void onMinionDie(L2MonsterInstance minion, int respawnTime) {
        minion.setLeader(null); // prevent memory leaks
        _spawnedMinions.remove(minion);

        final int time = respawnTime < 0 ? _master.isRaid() ? (int) Config.RAID_MINION_RESPAWN_TIMER : 0 : respawnTime;
        if ((time > 0) && !_master.isAlikeDead()) {
            ThreadPoolManager.getInstance().schedule(new MinionRespawnTask(minion), time);
        }
    }

    /**
     * Called if master/minion was attacked. Master and all free minions receive aggro against attacker.
     *
     * @param caller
     * @param attacker
     */
    public void onAssist(L2Character caller, L2Character attacker) {
        if (attacker == null) {
            return;
        }

        if (!_master.isAlikeDead() && !_master.isInCombat()) {
            _master.addDamageHate(attacker, 0, 1);
        }

        final boolean callerIsMaster = caller == _master;
        int aggro = callerIsMaster ? 10 : 1;
        if (_master.isRaid()) {
            aggro *= 10;
        }

        for (L2MonsterInstance minion : _spawnedMinions) {
            if ((minion != null) && !minion.isDead() && (callerIsMaster || !minion.isInCombat())) {
                minion.addDamageHate(attacker, 0, aggro);
            }
        }
    }

    /**
     * Called from onTeleported() of the master Alive and able to move minions teleported to master.
     */
    public void onMasterTeleported() {
        final int offset = 200;
        final int minRadius = (int) _master.getCollisionRadius() + 30;

        for (L2MonsterInstance minion : _spawnedMinions) {
            if ((minion != null) && !minion.isDead() && !minion.isMovementDisabled()) {
                int newX = Rnd.get(minRadius * 2, offset * 2); // x
                int newY = Rnd.get(newX, offset * 2); // distance
                newY = (int) Math.sqrt((newY * newY) - (newX * newX)); // y
                if (newX > (offset + minRadius)) {
                    newX = (_master.getX() + newX) - offset;
                } else {
                    newX = (_master.getX() - newX) + minRadius;
                }
                if (newY > (offset + minRadius)) {
                    newY = (_master.getY() + newY) - offset;
                } else {
                    newY = (_master.getY() - newY) + minRadius;
                }

                minion.teleToLocation(new Location(newX, newY, _master.getZ()));
            }
        }
    }

    private final void spawnMinion(int minionId) {
        if (minionId == 0) {
            return;
        }
        spawnMinion(_master, minionId);
    }

    private final int countSpawnedMinionsById(int minionId) {
        int count = 0;
        for (L2MonsterInstance minion : _spawnedMinions) {
            if ((minion != null) && (minion.getId() == minionId)) {
                count++;
            }
        }
        return count;
    }

    // Statistics part

    public final int countSpawnedMinions() {
        return _spawnedMinions.size();
    }

    public final long lazyCountSpawnedMinionsGroups() {
        return _spawnedMinions.stream().distinct().count();
    }

    private final class MinionRespawnTask implements Runnable {
        private final L2MonsterInstance _minion;

        public MinionRespawnTask(L2MonsterInstance minion) {
            _minion = minion;
        }

        @Override
        public void run() {
            if (!_master.isAlikeDead() && _master.isSpawned()) {
                // minion can be already spawned or deleted
                if (!_minion.isSpawned()) {
                    _minion.refreshID();
                    initializeNpcInstance(_master, _minion);
                }
            }
        }
    }
}

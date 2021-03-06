package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.model.actor.instance.L2AirShipInstance;
import org.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.l2j.gameserver.network.L2GameClient;
import org.l2j.gameserver.network.ServerPacketId;

public class ExAirShipStopMove extends ServerPacket {
    private final int _playerId;
    private final int _airShipId;
    private final int _x;
    private final int _y;
    private final int _z;

    public ExAirShipStopMove(L2PcInstance player, L2AirShipInstance ship, int x, int y, int z) {
        _playerId = player.getObjectId();
        _airShipId = ship.getObjectId();
        _x = x;
        _y = y;
        _z = z;
    }

    @Override
    public void writeImpl(L2GameClient client) {
        writeId(ServerPacketId.EX_MOVE_TO_LOCATION_AIR_SHIP);

        writeInt(_airShipId);
        writeInt(_playerId);
        writeInt(_x);
        writeInt(_y);
        writeInt(_z);
    }

}
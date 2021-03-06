package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.model.actor.instance.L2DoorInstance;
import org.l2j.gameserver.network.L2GameClient;
import org.l2j.gameserver.network.ServerPacketId;

public final class DoorInfo extends ServerPacket {
    private final L2DoorInstance _door;

    public DoorInfo(L2DoorInstance door) {
        _door = door;
    }

    @Override
    public void writeImpl(L2GameClient client) {
        writeId(ServerPacketId.DOOR_INFO);

        writeInt(_door.getObjectId());
        writeInt(_door.getId());
    }

}

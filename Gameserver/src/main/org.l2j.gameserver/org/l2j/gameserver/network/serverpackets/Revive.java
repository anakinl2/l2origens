package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.model.L2Object;
import org.l2j.gameserver.network.L2GameClient;
import org.l2j.gameserver.network.ServerPacketId;

public final class Revive extends ServerPacket {
    private final int _objectId;

    public Revive(L2Object obj) {
        _objectId = obj.getObjectId();
    }

    @Override
    public void writeImpl(L2GameClient client) {
        writeId(ServerPacketId.REVIVE);

        writeInt(_objectId);
    }

}

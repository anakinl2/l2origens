package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.l2j.gameserver.network.L2GameClient;
import org.l2j.gameserver.network.ServerPacketId;

public class PrivateStoreMsgBuy extends ServerPacket {
    private final int _objId;
    private String _storeMsg;

    public PrivateStoreMsgBuy(L2PcInstance player) {
        _objId = player.getObjectId();
        if (player.getBuyList() != null) {
            _storeMsg = player.getBuyList().getTitle();
        }
    }

    @Override
    public void writeImpl(L2GameClient client) {
        writeId(ServerPacketId.PRIVATE_STORE_BUY_MSG);

        writeInt(_objId);
        writeString(_storeMsg);
    }

}

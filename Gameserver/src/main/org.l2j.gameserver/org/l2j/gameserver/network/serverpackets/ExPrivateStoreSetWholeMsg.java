package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.l2j.gameserver.network.L2GameClient;
import org.l2j.gameserver.network.ServerPacketId;

/**
 * @author KenM
 */
public class ExPrivateStoreSetWholeMsg extends ServerPacket {
    private final int _objectId;
    private final String _msg;

    public ExPrivateStoreSetWholeMsg(L2PcInstance player, String msg) {
        _objectId = player.getObjectId();
        _msg = msg;
    }

    public ExPrivateStoreSetWholeMsg(L2PcInstance player) {
        this(player, player.getSellList().getTitle());
    }

    @Override
    public void writeImpl(L2GameClient client) {
        writeId(ServerPacketId.EX_PRIVATE_STORE_WHOLE_MSG);

        writeInt(_objectId);
        writeString(_msg);
    }

}

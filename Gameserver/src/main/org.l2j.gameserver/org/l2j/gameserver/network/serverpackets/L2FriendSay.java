package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.network.L2GameClient;
import org.l2j.gameserver.network.ServerPacketId;

/**
 * Send Private (Friend) Message
 *
 * @author Tempy
 */
public class L2FriendSay extends ServerPacket {
    private final String _sender;
    private final String _receiver;
    private final String _message;

    public L2FriendSay(String sender, String reciever, String message) {
        _sender = sender;
        _receiver = reciever;
        _message = message;
    }

    @Override
    public void writeImpl(L2GameClient client) {
        writeId(ServerPacketId.L2_FRIEND_SAY);

        writeInt(0); // ??
        writeString(_receiver);
        writeString(_sender);
        writeString(_message);
    }

}

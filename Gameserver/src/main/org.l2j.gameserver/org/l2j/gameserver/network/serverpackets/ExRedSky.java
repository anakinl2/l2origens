package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.network.L2GameClient;
import org.l2j.gameserver.network.ServerPacketId;

/**
 * @author KenM
 */
public class ExRedSky extends ServerPacket {
    private final int _duration;

    public ExRedSky(int duration) {
        _duration = duration;
    }

    @Override
    public void writeImpl(L2GameClient client) {
        writeId(ServerPacketId.EX_RED_SKY);

        writeInt(_duration);
    }

}

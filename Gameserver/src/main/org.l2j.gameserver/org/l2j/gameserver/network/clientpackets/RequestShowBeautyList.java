package org.l2j.gameserver.network.clientpackets;

import org.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.l2j.gameserver.network.serverpackets.ExResponseBeautyList;

/**
 * @author Sdw
 */
public class RequestShowBeautyList extends ClientPacket {
    private int _type;

    @Override
    public void readImpl() {
        _type = readInt();
    }

    @Override
    public void runImpl() {
        final L2PcInstance activeChar = client.getActiveChar();
        if (activeChar == null) {
            return;
        }

        client.sendPacket(new ExResponseBeautyList(activeChar, _type));
    }
}

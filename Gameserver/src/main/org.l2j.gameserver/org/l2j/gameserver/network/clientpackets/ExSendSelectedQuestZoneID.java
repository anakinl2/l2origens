package org.l2j.gameserver.network.clientpackets;

import org.l2j.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author St3eT
 */
public final class ExSendSelectedQuestZoneID extends ClientPacket {
    private int _questZoneId;

    @Override
    public void readImpl() {
        _questZoneId = readInt();
    }

    @Override
    public void runImpl() {
        final L2PcInstance activeChar = client.getActiveChar();
        if (activeChar == null) {
            return;
        }

        activeChar.setQuestZoneId(_questZoneId);
    }
}

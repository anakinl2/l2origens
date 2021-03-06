package org.l2j.gameserver.network.serverpackets.compound;

import io.github.joealisson.mmocore.StaticPacket;
import org.l2j.gameserver.network.L2GameClient;
import org.l2j.gameserver.network.ServerPacketId;
import org.l2j.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Sdw
 */
@StaticPacket
public class ExEnchantRetryToPutItemOk extends ServerPacket {
    public static final ExEnchantRetryToPutItemOk STATIC_PACKET = new ExEnchantRetryToPutItemOk();

    private ExEnchantRetryToPutItemOk() {
    }

    @Override
    public void writeImpl(L2GameClient client) {
        writeId(ServerPacketId.EX_ENCHANT_RETRY_TO_PUT_ITEM_OK);
    }

}
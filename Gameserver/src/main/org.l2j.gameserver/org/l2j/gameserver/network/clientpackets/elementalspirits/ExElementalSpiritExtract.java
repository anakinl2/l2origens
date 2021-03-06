package org.l2j.gameserver.network.clientpackets.elementalspirits;

import org.l2j.gameserver.data.elemental.ElementalSpirit;
import org.l2j.gameserver.data.elemental.ElementalSpiritManager;
import org.l2j.gameserver.data.elemental.ElementalType;
import org.l2j.gameserver.enums.PrivateStoreType;
import org.l2j.gameserver.enums.UserInfoType;
import org.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.l2j.gameserver.network.clientpackets.ClientPacket;
import org.l2j.gameserver.network.serverpackets.SystemMessage;
import org.l2j.gameserver.network.serverpackets.UserInfo;
import org.l2j.gameserver.network.serverpackets.elementalspirits.ElementalSpiritExtract;

import static java.util.Objects.isNull;
import static org.l2j.gameserver.network.SystemMessageId.*;

public class ExElementalSpiritExtract extends ClientPacket {

    private byte type;

    @Override
    protected void readImpl() throws Exception {
        type = readByte();
    }

    @Override
    protected void runImpl() {
        var player = client.getActiveChar();
        var spirit = player.getElementalSpirit(ElementalType.of(type));

        if(isNull(spirit)) {
            client.sendPacket(NO_SPIRITS_ARE_AVAILABLE);
            return;
        }

        var canExtract = checkConditions(player, spirit);

        if(canExtract) {
            var amount = spirit.getExtractAmount();
            client.sendPacket(SystemMessage.getSystemMessage(EXTRACTED_S1_S2_SUCCESSFULLY).addItemName(spirit.getExtractItem()).addInt(amount));
            spirit.resetStage();
            player.addItem("Extract", spirit.getExtractItem(), amount, player, true);

            var userInfo = new UserInfo(player);
            userInfo.addComponentType(UserInfoType.ATT_SPIRITS);
            client.sendPacket(userInfo);
        }

        client.sendPacket(new ElementalSpiritExtract(type, canExtract));
    }

    private boolean checkConditions(L2PcInstance player, ElementalSpirit spirit) {
        var noMeetConditions = false;

        if(noMeetConditions = spirit.getExtractAmount() < 1) {
            client.sendPacket(NOT_ENOUGH_ATTRIBUTE_XP_FOR_EXTRACTION);
        } else if(noMeetConditions = !player.getInventory().validateCapacity(1)) {
            client.sendPacket(UNABLE_TO_EXTRACT_BECAUSE_INVENTORY_IS_FULL);
        } else if(noMeetConditions = player.getPrivateStoreType() != PrivateStoreType.NONE) {
            client.sendPacket(CANNOT_EVOLVE_ABSORB_EXTRACT_WHILE_USING_THE_PRIVATE_STORE_WORKSHOP);
        } else if(noMeetConditions = player.isInBattle()) {
            client.sendPacket(UNABLE_TO_EVOLVE_DURING_BATTLE);
        } else if(noMeetConditions = !player.reduceAdena("Extract", ElementalSpiritManager.EXTRACT_FEE,  player, true)) {
            client.sendPacket(NOT_ENOUGH_INGREDIENTS_TO_EXTRACT);
        }
        return !noMeetConditions;
    }
}

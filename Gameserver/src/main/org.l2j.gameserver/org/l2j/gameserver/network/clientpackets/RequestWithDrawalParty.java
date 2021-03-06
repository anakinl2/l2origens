package org.l2j.gameserver.network.clientpackets;

import org.l2j.gameserver.model.L2Party;
import org.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.l2j.gameserver.model.matching.MatchingRoom;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestWithDrawalParty extends ClientPacket {
    @Override
    public void readImpl() {

    }

    @Override
    public void runImpl() {
        final L2PcInstance player = client.getActiveChar();
        if (player == null) {
            return;
        }

        final L2Party party = player.getParty();
        if (party != null) {
            party.removePartyMember(player, L2Party.MessageType.LEFT);

            final MatchingRoom room = player.getMatchingRoom();
            if (room != null) {
                room.deleteMember(player, false);
            }
        }
    }
}

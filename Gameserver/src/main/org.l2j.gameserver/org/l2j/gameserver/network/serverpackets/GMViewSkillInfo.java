package org.l2j.gameserver.network.serverpackets;

import org.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.l2j.gameserver.model.skills.Skill;
import org.l2j.gameserver.network.L2GameClient;
import org.l2j.gameserver.network.ServerPacketId;

import java.util.Collection;

public class GMViewSkillInfo extends ServerPacket {
    private final L2PcInstance _activeChar;
    private final Collection<Skill> _skills;

    public GMViewSkillInfo(L2PcInstance cha) {
        _activeChar = cha;
        _skills = _activeChar.getSkillList();
    }

    @Override
    public void writeImpl(L2GameClient client) {
        writeId(ServerPacketId.GM_VIEW_SKILL_INFO);

        writeString(_activeChar.getName());
        writeInt(_skills.size());

        final boolean isDisabled = (_activeChar.getClan() != null) && (_activeChar.getClan().getReputationScore() < 0);

        for (Skill skill : _skills) {
            writeInt(skill.isPassive() ? 1 : 0);
            writeShort((short) skill.getDisplayLevel());
            writeShort((short) skill.getSubLevel());
            writeInt(skill.getDisplayId());
            writeInt(0x00);
            writeByte((byte) (isDisabled && skill.isClanSkill() ? 1 : 0));
            writeByte((byte)(skill.isEnchantable() ? 1 : 0));
        }
    }

}
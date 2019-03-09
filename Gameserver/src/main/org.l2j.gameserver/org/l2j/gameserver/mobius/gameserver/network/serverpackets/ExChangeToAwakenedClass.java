/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2j.gameserver.mobius.gameserver.network.serverpackets;

import org.l2j.commons.network.PacketWriter;
import org.l2j.gameserver.mobius.gameserver.network.OutgoingPackets;

/**
 * @author Sdw
 */
public class ExChangeToAwakenedClass implements IClientOutgoingPacket
{
	private final int _classId;
	
	public ExChangeToAwakenedClass(int classId)
	{
		_classId = classId;
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_CHANGE_TO_AWAKENED_CLASS.writeId(packet);
		
		packet.writeD(_classId);
		return true;
	}
}
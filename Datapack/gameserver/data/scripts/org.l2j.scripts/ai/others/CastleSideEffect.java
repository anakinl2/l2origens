package ai.others;

import ai.AbstractNpcAI;
import org.l2j.gameserver.instancemanager.CastleManager;
import org.l2j.gameserver.model.actor.L2Character;
import org.l2j.gameserver.model.entity.Castle;
import org.l2j.gameserver.model.zone.L2ZoneType;
import org.l2j.gameserver.network.serverpackets.ExCastleState;

/**
 * Shows castle side effect in cities.
 * @author Gigi
 *
 */
public class CastleSideEffect extends AbstractNpcAI
{
	private static final int[] ZONE_ID =
	{
		11020, // Giran
		11027, // Gludio
		11028, // Dion
		11029, // Oren
		11031, // aden
		11032, // Goddard
		11033, // Rune
		11034, // Heine
		11035, // Shuttgard
	};
	
	public CastleSideEffect()
	{
		addEnterZoneId(ZONE_ID);
	}
	
	@Override
	public String onEnterZone(L2Character character, L2ZoneType zone)
	{
		if (character.isPlayer())
		{
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				character.sendPacket(new ExCastleState(castle));
			}
		}
		return super.onEnterZone(character, zone);
	}
	
	public static void main(String[] args)
	{
		new CastleSideEffect();
	}
}
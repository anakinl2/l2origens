package org.l2j.gameserver.network.clientpackets;

import org.l2j.gameserver.enums.ItemLocation;
import org.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.l2j.gameserver.model.itemcontainer.Inventory;
import org.l2j.gameserver.model.items.instance.L2ItemInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Format:(ch) d[dd]
 *
 * @author -Wooden-
 */
public final class RequestSaveInventoryOrder extends ClientPacket {
    /**
     * client limit
     */
    private static final int LIMIT = 125;
    private List<InventoryOrder> _order;

    @Override
    public void readImpl() {
        int sz = readInt();
        sz = Math.min(sz, LIMIT);
        _order = new ArrayList<>(sz);
        for (int i = 0; i < sz; i++) {
            final int objectId = readInt();
            final int order = readInt();
            _order.add(new InventoryOrder(objectId, order));
        }
    }

    @Override
    public void runImpl() {
        final L2PcInstance player = client.getActiveChar();
        if (player != null) {
            final Inventory inventory = player.getInventory();
            for (InventoryOrder order : _order) {
                final L2ItemInstance item = inventory.getItemByObjectId(order.objectID);
                if ((item != null) && (item.getItemLocation() == ItemLocation.INVENTORY)) {
                    item.setItemLocation(ItemLocation.INVENTORY, order.order);
                }
            }
        }
    }

    private static class InventoryOrder {
        int order;

        int objectID;

        public InventoryOrder(int id, int ord) {
            objectID = id;
            order = ord;
        }
    }
}

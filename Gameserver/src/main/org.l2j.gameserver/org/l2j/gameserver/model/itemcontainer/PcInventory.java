package org.l2j.gameserver.model.itemcontainer;

import org.l2j.commons.database.DatabaseFactory;
import org.l2j.gameserver.Config;
import org.l2j.gameserver.world.WorldTimeController;
import org.l2j.gameserver.datatables.ItemTable;
import org.l2j.gameserver.enums.InventoryBlockType;
import org.l2j.gameserver.enums.ItemLocation;
import org.l2j.gameserver.model.TradeItem;
import org.l2j.gameserver.model.TradeList;
import org.l2j.gameserver.model.actor.instance.Player;
import org.l2j.gameserver.model.events.EventDispatcher;
import org.l2j.gameserver.model.events.impl.character.player.OnPlayerItemAdd;
import org.l2j.gameserver.model.events.impl.character.player.OnPlayerItemDestroy;
import org.l2j.gameserver.model.events.impl.character.player.OnPlayerItemDrop;
import org.l2j.gameserver.model.events.impl.character.player.OnPlayerItemTransfer;
import org.l2j.gameserver.model.items.CommonItem;
import org.l2j.gameserver.model.items.ItemTemplate;
import org.l2j.gameserver.model.items.instance.Item;
import org.l2j.gameserver.model.items.type.EtcItemType;
import org.l2j.gameserver.network.SystemMessageId;
import org.l2j.gameserver.network.serverpackets.InventoryUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class PcInventory extends Inventory {
    private static final Logger LOGGER = LoggerFactory.getLogger(PcInventory.class);

    private final Player _owner;
    private Item _adena;
    private Item _ancientAdena;
    private Item _beautyTickets;
    private Item silverCoin;
    private Item rustyCoin;

    private Collection<Integer> _blockItems = null;

    private InventoryBlockType _blockMode = InventoryBlockType.NONE;

    public PcInventory(Player owner) {
        _owner = owner;
    }

    public static int[][] restoreVisibleInventory(int objectId) {
        final int[][] paperdoll = new int[Inventory.PAPERDOLL_TOTALSLOTS][3];
        try (Connection con = DatabaseFactory.getInstance().getConnection();
             PreparedStatement statement2 = con.prepareStatement("SELECT object_id,item_id,loc_data,enchant_level FROM items WHERE owner_id=? AND loc='PAPERDOLL'")) {
            statement2.setInt(1, objectId);
            try (ResultSet invdata = statement2.executeQuery()) {
                while (invdata.next()) {
                    final int slot = invdata.getInt("loc_data");
                    paperdoll[slot][0] = invdata.getInt("object_id");
                    paperdoll[slot][1] = invdata.getInt("item_id");
                    paperdoll[slot][2] = invdata.getInt("enchant_level");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not restore inventory: " + e.getMessage(), e);
        }
        return paperdoll;
    }

    @Override
    public Player getOwner() {
        return _owner;
    }

    @Override
    protected ItemLocation getBaseLocation() {
        return ItemLocation.INVENTORY;
    }

    @Override
    protected ItemLocation getEquipLocation() {
        return ItemLocation.PAPERDOLL;
    }

    public Item getAdenaInstance() {
        return _adena;
    }

    @Override
    public long getAdena() {
        return _adena != null ? _adena.getCount() : 0;
    }

    public Item getAncientAdenaInstance() {
        return _ancientAdena;
    }

    public long getAncientAdena() {
        return (_ancientAdena != null) ? _ancientAdena.getCount() : 0;
    }

    public Item getBeautyTicketsInstance() {
        return _beautyTickets;
    }

    @Override
    public long getBeautyTickets() {
        return _beautyTickets != null ? _beautyTickets.getCount() : 0;
    }

    /**
     * Returns the list of items in inventory available for transaction
     *
     * @param allowAdena
     * @param allowAncientAdena
     * @return Item : items in inventory
     */
    public Collection<Item> getUniqueItems(boolean allowAdena, boolean allowAncientAdena) {
        return getUniqueItems(allowAdena, allowAncientAdena, true);
    }

    public Collection<Item> getUniqueItems(boolean allowAdena, boolean allowAncientAdena, boolean onlyAvailable) {
        final Collection<Item> list = new LinkedList<>();
        for (Item item : _items.values()) {
            if (!allowAdena && (item.getId() == CommonItem.ADENA)) {
                continue;
            }
            if (!allowAncientAdena && (item.getId() == CommonItem.ANCIENT_ADENA)) {
                continue;
            }
            boolean isDuplicate = false;
            for (Item litem : list) {
                if (litem.getId() == item.getId()) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate && (!onlyAvailable || (item.isSellable() && item.isAvailable(_owner, false, false)))) {
                list.add(item);
            }
        }

        return list;
    }

    /**
     * @param itemId
     * @return
     */
    public Collection<Item> getAllItemsByItemId(int itemId) {
        return getAllItemsByItemId(itemId, true);
    }

    /**
     * Returns the list of all items in inventory that have a given item id.
     *
     * @param itemId          : ID of item
     * @param includeEquipped : include equipped items
     * @return Item[] : matching items from inventory
     */
    public Collection<Item> getAllItemsByItemId(int itemId, boolean includeEquipped) {
        return getItems(i -> (i.getId() == itemId) && (includeEquipped || !i.isEquipped()));
    }

    /**
     * @param itemId
     * @param enchantment
     * @return
     */
    public Collection<Item> getAllItemsByItemId(int itemId, int enchantment) {
        return getAllItemsByItemId(itemId, enchantment, true);
    }

    /**
     * Returns the list of all items in inventory that have a given item id AND a given enchantment level.
     *
     * @param itemId          : ID of item
     * @param enchantment     : enchant level of item
     * @param includeEquipped : include equipped items
     * @return Item[] : matching items from inventory
     */
    public Collection<Item> getAllItemsByItemId(int itemId, int enchantment, boolean includeEquipped) {
        return getItems(i -> (i.getId() == itemId) && (i.getEnchantLevel() == enchantment) && (includeEquipped || !i.isEquipped()));
    }

    /**
     * @param allowAdena
     * @param allowNonTradeable
     * @param feightable
     * @return the list of items in inventory available for transaction
     */
    public Collection<Item> getAvailableItems(boolean allowAdena, boolean allowNonTradeable, boolean feightable) {
        return getItems(i ->
        {
            if (!i.isAvailable(_owner, allowAdena, allowNonTradeable) || !canManipulateWithItemId(i.getId())) {
                return false;
            } else if (feightable) {
                return (i.getItemLocation() == ItemLocation.INVENTORY) && i.isFreightable();
            }
            return true;
        });
    }

    /**
     * Returns the list of items in inventory available for transaction adjusted by tradeList
     *
     * @param tradeList
     * @return Item : items in inventory
     */
    public Collection<TradeItem> getAvailableItems(TradeList tradeList) {
        //@formatter:off
        return _items.values().stream()
                .filter(i -> i.isAvailable(_owner, false, false))
                .map(tradeList::adjustAvailableItem)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedList::new));
        //@formatter:on
    }

    /**
     * Adjust TradeItem according his status in inventory
     *
     * @param item : Item to be adjusted
     */
    public void adjustAvailableItem(TradeItem item) {
        boolean notAllEquipped = false;
        for (Item adjItem : getItemsByItemId(item.getItem().getId())) {
            if (adjItem.isEquipable()) {
                if (!adjItem.isEquipped()) {
                    notAllEquipped |= true;
                }
            } else {
                notAllEquipped |= true;
                break;
            }
        }
        if (notAllEquipped) {
            final Item adjItem = getItemByItemId(item.getItem().getId());
            item.setObjectId(adjItem.getObjectId());
            item.setEnchant(adjItem.getEnchantLevel());

            if (adjItem.getCount() < item.getCount()) {
                item.setCount(adjItem.getCount());
            }

            return;
        }

        item.setCount(0);
    }

    /**
     * Adds adena to PcInventory
     *
     * @param process   : String Identifier of process triggering this action
     * @param count     : int Quantity of adena to be added
     * @param actor     : Player Player requesting the item add
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     */
    public void addAdena(String process, long count, Player actor, Object reference) {
        if (count > 0) {
            addItem(process, CommonItem.ADENA, count, actor, reference);
        }
    }

    /**
     * Adds Beauty Tickets to PcInventory
     *
     * @param process   : String Identifier of process triggering this action
     * @param count     : int Quantity of Beauty Tickets to be added
     * @param actor     : Player Player requesting the item add
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     */
    public void addBeautyTickets(String process, long count, Player actor, Object reference) {
        if (count > 0) {
            addItem(process, BEAUTY_TICKET_ID, count, actor, reference);
        }
    }

    /**
     * Removes adena to PcInventory
     *
     * @param process   : String Identifier of process triggering this action
     * @param count     : int Quantity of adena to be removed
     * @param actor     : Player Player requesting the item add
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @return boolean : true if adena was reduced
     */
    public boolean reduceAdena(String process, long count, Player actor, Object reference) {
        if (count > 0) {
            return destroyItemByItemId(process, CommonItem.ADENA, count, actor, reference) != null;
        }
        return false;
    }

    /**
     * Removes Beauty Tickets to PcInventory
     *
     * @param process   : String Identifier of process triggering this action
     * @param count     : int Quantity of Beauty Tickets to be removed
     * @param actor     : Player Player requesting the item add
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @return boolean : true if adena was reduced
     */
    public boolean reduceBeautyTickets(String process, long count, Player actor, Object reference) {
        if (count > 0) {
            return destroyItemByItemId(process, BEAUTY_TICKET_ID, count, actor, reference) != null;
        }
        return false;
    }

    /**
     * Adds specified amount of ancient adena to player inventory.
     *
     * @param process   : String Identifier of process triggering this action
     * @param count     : int Quantity of adena to be added
     * @param actor     : Player Player requesting the item add
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     */
    public void addAncientAdena(String process, long count, Player actor, Object reference) {
        if (count > 0) {
            addItem(process,CommonItem.ANCIENT_ADENA, count, actor, reference);
        }
    }

    /**
     * Removes specified amount of ancient adena from player inventory.
     *
     * @param process   : String Identifier of process triggering this action
     * @param count     : int Quantity of adena to be removed
     * @param actor     : Player Player requesting the item add
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @return boolean : true if adena was reduced
     */
    public boolean reduceAncientAdena(String process, long count, Player actor, Object reference) {
        if (count > 0) {
            return destroyItemByItemId(process, CommonItem.ANCIENT_ADENA, count, actor, reference) != null;
        }
        return false;
    }

    /**
     * Adds item in inventory and checks _adena and _ancientAdena
     *
     * @param process   : String Identifier of process triggering this action
     * @param item      : Item to be added
     * @param actor     : Player Player requesting the item add
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @return Item corresponding to the new item or the updated item in inventory
     */
    @Override
    public Item addItem(String process, Item item, Player actor, Object reference) {
        item = super.addItem(process, item, actor, reference);

        if (item != null) {
            if ((item.getId() == CommonItem.ADENA) && !item.equals(_adena)) {
                _adena = item;
            } else if ((item.getId() == CommonItem.ANCIENT_ADENA) && !item.equals(_ancientAdena)) {
                _ancientAdena = item;
            } else if ((item.getId() == BEAUTY_TICKET_ID) && !item.equals(_beautyTickets)) {
                _beautyTickets = item;
            } else if( item.getId() == CommonItem.SILVER_COIN && !item.equals(silverCoin)) {
                silverCoin = item;
            } else if(item.getId() == CommonItem.RUSTY_COIN && !item.equals(rustyCoin)) {
                rustyCoin = item;
            }

            if (actor != null) {
                // Send inventory update packet
                if (!Config.FORCE_INVENTORY_UPDATE) {
                    final InventoryUpdate playerIU = new InventoryUpdate();
                    playerIU.addItem(item);
                    actor.sendInventoryUpdate(playerIU);
                } else {
                    actor.sendItemList();
                }

                // Notify to scripts
                EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemAdd(actor, item), actor, item.getItem());
            }
        }

        return item;
    }

    /**
     * Adds item in inventory and checks _adena and _ancientAdena
     *
     * @param process   : String Identifier of process triggering this action
     * @param itemId    : int Item Identifier of the item to be added
     * @param count     : int Quantity of items to be added
     * @param actor     : Player Player requesting the item creation
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @return Item corresponding to the new item or the updated item in inventory
     */
    @Override
    public Item addItem(String process, int itemId, long count, Player actor, Object reference) {
        return addItem(process, itemId, count, actor, reference, true);
    }

    /**
     * Adds item in inventory and checks _adena and _ancientAdena
     *
     * @param process   : String Identifier of process triggering this action
     * @param itemId    : int Item Identifier of the item to be added
     * @param count     : int Quantity of items to be added
     * @param actor     : Player Player requesting the item creation
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @param update    : Update inventory (not used by MultiSellChoose packet / it sends update after finish)
     * @return Item corresponding to the new item or the updated item in inventory
     */
    public Item addItem(String process, int itemId, long count, Player actor, Object reference, boolean update) {
        final Item item = super.addItem(process, itemId, count, actor, reference);
        if (item != null) {
            if ((item.getId() == CommonItem.ADENA) && !item.equals(_adena)) {
                _adena = item;
            } else if ((item.getId() == CommonItem.ANCIENT_ADENA) && !item.equals(_ancientAdena)) {
                _ancientAdena = item;
            } else if ((item.getId() == BEAUTY_TICKET_ID) && !item.equals(_beautyTickets)) {
                _beautyTickets = item;
            }
        }

        if ((item != null) && (actor != null)) {
            // Send inventory update packet
            if (update) {
                if (!Config.FORCE_INVENTORY_UPDATE) {
                    final InventoryUpdate playerIU = new InventoryUpdate();
                    playerIU.addItem(item);
                    actor.sendInventoryUpdate(playerIU);
                } else {
                    actor.sendItemList();
                }
            }

            // Notify to scripts
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemAdd(actor, item), actor, item.getItem());
        }
        return item;
    }

    /**
     * Transfers item to another inventory and checks _adena and _ancientAdena
     *
     * @param process   string Identifier of process triggering this action
     * @param objectId  Item Identifier of the item to be transfered
     * @param count     Quantity of items to be transfered
     * @param target    the item container for the item to be transfered.
     * @param actor     the player requesting the item transfer
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @return Item corresponding to the new item or the updated item in inventory
     */
    @Override
    public Item transferItem(String process, int objectId, long count, ItemContainer target, Player actor, Object reference) {
        final Item item = super.transferItem(process, objectId, count, target, actor, reference);

        if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId()))) {
            _adena = null;
        }

        if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId()))) {
            _ancientAdena = null;
        }

        // Notify to scripts
        EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemTransfer(actor, item, target), item.getItem());
        return item;
    }

    @Override
    public Item detachItem(String process, Item item, long count, ItemLocation newLocation, Player actor, Object reference) {
        item = super.detachItem(process, item, count, newLocation, actor, reference);

        if ((item != null) && (actor != null)) {
            actor.sendItemList();
        }
        return item;
    }

    /**
     * Destroy item from inventory and checks _adena and _ancientAdena
     *
     * @param process   : String Identifier of process triggering this action
     * @param item      : Item to be destroyed
     * @param actor     : Player Player requesting the item destroy
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @return Item corresponding to the destroyed item or the updated item in inventory
     */
    @Override
    public Item destroyItem(String process, Item item, Player actor, Object reference) {
        return destroyItem(process, item, item.getCount(), actor, reference);
    }

    /**
     * Destroy item from inventory and checks _adena and _ancientAdena
     *
     * @param process   : String Identifier of process triggering this action
     * @param item      : Item to be destroyed
     * @param actor     : Player Player requesting the item destroy
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @return Item corresponding to the destroyed item or the updated item in inventory
     */
    @Override
    public Item destroyItem(String process, Item item, long count, Player actor, Object reference) {
        item = super.destroyItem(process, item, count, actor, reference);

        if ((_adena != null) && (_adena.getCount() <= 0)) {
            _adena = null;
        }

        if ((_ancientAdena != null) && (_ancientAdena.getCount() <= 0)) {
            _ancientAdena = null;
        }

        // Notify to scripts
        if (item != null) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDestroy(actor, item), item.getItem());
        }
        return item;
    }

    /**
     * Destroys item from inventory and checks _adena and _ancientAdena
     *
     * @param process   : String Identifier of process triggering this action
     * @param objectId  : int Item Instance identifier of the item to be destroyed
     * @param count     : int Quantity of items to be destroyed
     * @param actor     : Player Player requesting the item destroy
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @return Item corresponding to the destroyed item or the updated item in inventory
     */
    @Override
    public Item destroyItem(String process, int objectId, long count, Player actor, Object reference) {
        final Item item = getItemByObjectId(objectId);
        if (item == null) {
            return null;
        }
        return destroyItem(process, item, count, actor, reference);
    }

    /**
     * Destroy item from inventory by using its <B>itemId</B> and checks _adena and _ancientAdena
     *
     * @param process   : String Identifier of process triggering this action
     * @param itemId    : int Item identifier of the item to be destroyed
     * @param count     : int Quantity of items to be destroyed
     * @param actor     : Player Player requesting the item destroy
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @return Item corresponding to the destroyed item or the updated item in inventory
     */
    @Override
    public Item destroyItemByItemId(String process, int itemId, long count, Player actor, Object reference) {
        final Item item = getItemByItemId(itemId);
        if (item == null) {
            return null;
        }
        return destroyItem(process, item, count, actor, reference);
    }

    /**
     * Drop item from inventory and checks _adena and _ancientAdena
     *
     * @param process   : String Identifier of process triggering this action
     * @param item      : Item to be dropped
     * @param actor     : Player Player requesting the item drop
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @return Item corresponding to the destroyed item or the updated item in inventory
     */
    @Override
    public Item dropItem(String process, Item item, Player actor, Object reference) {
        item = super.dropItem(process, item, actor, reference);

        if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId()))) {
            _adena = null;
        }

        if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId()))) {
            _ancientAdena = null;
        }

        // Notify to scripts
        if (item != null) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(actor, item, item.getLocation()), item.getItem());
        }
        return item;
    }

    /**
     * Drop item from inventory by using its <B>objectID</B> and checks _adena and _ancientAdena
     *
     * @param process   : String Identifier of process triggering this action
     * @param objectId  : int Item Instance identifier of the item to be dropped
     * @param count     : int Quantity of items to be dropped
     * @param actor     : Player Player requesting the item drop
     * @param reference : Object Object referencing current action like NPC selling item or previous item in transformation
     * @return Item corresponding to the destroyed item or the updated item in inventory
     */
    @Override
    public Item dropItem(String process, int objectId, long count, Player actor, Object reference) {
        final Item item = super.dropItem(process, objectId, count, actor, reference);

        if ((_adena != null) && ((_adena.getCount() <= 0) || (_adena.getOwnerId() != getOwnerId()))) {
            _adena = null;
        }

        if ((_ancientAdena != null) && ((_ancientAdena.getCount() <= 0) || (_ancientAdena.getOwnerId() != getOwnerId()))) {
            _ancientAdena = null;
        }

        // Notify to scripts
        if (item != null) {
            EventDispatcher.getInstance().notifyEventAsync(new OnPlayerItemDrop(actor, item, item.getLocation()), item.getItem());
        }
        return item;
    }

    /**
     * <b>Overloaded</b>, when removes item from inventory, remove also owner shortcuts.
     *
     * @param item : Item to be removed from inventory
     */
    @Override
    protected boolean removeItem(Item item) {
        // Removes any reference to the item from Shortcut bar
        _owner.removeItemFromShortCut(item.getObjectId());

        // Removes active Enchant Scroll
        if (_owner.isProcessingItem(item.getObjectId())) {
            _owner.removeRequestsThatProcessesItem(item.getObjectId());
        }

        if (item.getId() == CommonItem.ADENA) {
            _adena = null;
        } else if (item.getId() == CommonItem.ANCIENT_ADENA) {
            _ancientAdena = null;
        } else if (item.getId() == BEAUTY_TICKET_ID) {
            _beautyTickets = null;
        }

        return super.removeItem(item);
    }

    /**
     * Refresh the weight of equipment loaded
     */
    @Override
    public void refreshWeight() {
        super.refreshWeight();
        _owner.refreshOverloaded(true);
    }

    /**
     * Get back items in inventory from database
     */
    @Override
    public void restore() {
        super.restore();
        _adena = getItemByItemId(CommonItem.ADENA);
        _ancientAdena = getItemByItemId(CommonItem.ANCIENT_ADENA);
        _beautyTickets = getItemByItemId(BEAUTY_TICKET_ID);
    }

    /**
     * @param itemList         the items that needs to be validated.
     * @param sendMessage      if {@code true} will send a message of inventory full.
     * @param sendSkillMessage if {@code true} will send a message of skill not available.
     * @return {@code true} if the inventory isn't full after taking new items and items weight add to current load doesn't exceed max weight load.
     */
    public boolean checkInventorySlotsAndWeight(List<ItemTemplate> itemList, boolean sendMessage, boolean sendSkillMessage) {
        int lootWeight = 0;
        int requiredSlots = 0;
        if (itemList != null) {
            for (ItemTemplate item : itemList) {
                // If the item is not stackable or is stackable and not present in inventory, will need a slot.
                if (!item.isStackable() || (getInventoryItemCount(item.getId(), -1) <= 0)) {
                    requiredSlots++;
                }
                lootWeight += item.getWeight();
            }
        }

        final boolean inventoryStatusOK = validateCapacity(requiredSlots) && validateWeight(lootWeight);
        if (!inventoryStatusOK && sendMessage) {
            _owner.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
            if (sendSkillMessage) {
                _owner.sendPacket(SystemMessageId.WEIGHT_AND_VOLUME_LIMIT_HAVE_BEEN_EXCEEDED_THAT_SKILL_IS_CURRENTLY_UNAVAILABLE);
            }
        }
        return inventoryStatusOK;
    }

    /**
     * If the item is not stackable or is stackable and not present in inventory, will need a slot.
     *
     * @param item the item to validate.
     * @return {@code true} if there is enough room to add the item inventory.
     */
    public boolean validateCapacity(Item item) {
        int slots = 0;
        if (!item.isStackable() || ((getInventoryItemCount(item.getId(), -1) <= 0) && !item.getItem().hasExImmediateEffect())) {
            slots++;
        }
        return validateCapacity(slots, item.isQuestItem());
    }

    /**
     * If the item is not stackable or is stackable and not present in inventory, will need a slot.
     *
     * @param itemId the item Id for the item to validate.
     * @return {@code true} if there is enough room to add the item inventory.
     */
    public boolean validateCapacityByItemId(int itemId) {
        int slots = 0;
        final Item invItem = getItemByItemId(itemId);
        if ((invItem == null) || !invItem.isStackable()) {
            slots++;
        }
        return validateCapacity(slots, ItemTable.getInstance().getTemplate(itemId).isQuestItem());
    }

    @Override
    public boolean validateCapacity(long slots) {
        return validateCapacity(slots, false);
    }

    public boolean validateCapacity(long slots, boolean questItem) {
        return ((slots == 0) && !Config.AUTO_LOOT_SLOT_LIMIT) || questItem ? (getSize(item -> item.isQuestItem()) + slots) <= _owner.getQuestInventoryLimit() : (getSize(item -> !item.isQuestItem()) + slots) <= _owner.getInventoryLimit();
    }

    @Override
    public boolean validateWeight(long weight) {
        // Disable weight check for GMs.
        if (_owner.isGM() && _owner.getDietMode() && _owner.getAccessLevel().allowTransaction()) {
            return true;
        }
        return ((_totalWeight + weight) <= _owner.getMaxLoad());
    }

    /**
     * Set inventory block for specified IDs<br>
     * array reference is used for {@link PcInventory#_blockItems}
     *
     * @param items array of Ids to block/allow
     * @param mode  blocking mode {@link PcInventory#_blockMode}
     */
    public void setInventoryBlock(Collection<Integer> items, InventoryBlockType mode) {
        _blockMode = mode;
        _blockItems = items;

        _owner.sendItemList();
    }

    /**
     * Unblock blocked itemIds
     */
    public void unblock() {
        _blockMode = InventoryBlockType.NONE;
        _blockItems = null;

        _owner.sendItemList();
    }

    /**
     * Check if player inventory is in block mode.
     *
     * @return true if some itemIds blocked
     */
    public boolean hasInventoryBlock() {
        return ((_blockMode != InventoryBlockType.NONE) && (_blockItems != null) && !_blockItems.isEmpty());
    }

    /**
     * Block all items except adena
     */
    public void blockAllItems() {
        setInventoryBlock(Collections.singletonList(CommonItem.ADENA), InventoryBlockType.WHITELIST);
    }

    /**
     * Return block mode
     *
     * @return int {@link PcInventory#_blockMode}
     */
    public InventoryBlockType getBlockMode() {
        return _blockMode;
    }

    /**
     * Return Collection<Integer> with blocked item ids
     *
     * @return Collection<Integer>
     */
    public Collection<Integer> getBlockItems() {
        return _blockItems;
    }

    /**
     * Check if player can use item by itemid
     *
     * @param itemId int
     * @return true if can use
     */
    public boolean canManipulateWithItemId(int itemId) {
        final Collection<Integer> blockedItems = _blockItems;
        if (blockedItems != null) {
            return switch (_blockMode) {
                case NONE -> true;
                case WHITELIST -> blockedItems.stream().anyMatch(id -> id == itemId);
                case BLACKLIST -> blockedItems.stream().noneMatch(id -> id == itemId);
            };
        }
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + _owner + "]";
    }

    /**
     * Apply skills of inventory items
     */
    public void applyItemSkills() {
        for (Item item : _items.values()) {
            item.giveSkillsToOwner();
            item.applyEnchantStats();
            if (item.isEquipped()) {
                item.applySpecialAbilities();
            }
        }
    }

    /**
     * Reduce the number of arrows/bolts owned by the Player and send it Server->Client Packet InventoryUpdate or ItemList (to unequip if the last arrow was consumed).
     *
     * @param type
     */
    @Override
    public void reduceArrowCount(EtcItemType type) {
        if ((type != EtcItemType.ARROW) && (type != EtcItemType.BOLT)) {
            LOGGER.warn(type.toString(), " which is not arrow type passed to PlayerInstance.reduceArrowCount()");
            return;
        }

        final Item arrows = getPaperdollItem(Inventory.PAPERDOLL_LHAND);

        if ((arrows == null) || (arrows.getItemType() != type)) {
            return;
        }

        if (arrows.getEtcItem().isInfinite()) // Null-safe due to type checks above
        {
            return;
        }

        if ((WorldTimeController.getInstance().getGameTicks() % 10) == 0) {
            updateItemCount(null, arrows, -1, _owner, null);
        } else {
            updateItemCountNoDbUpdate(null, arrows, -1, _owner, null);
        }
    }

    /**
     * Reduces item count in the stack, destroys item when count reaches 0.
     *
     * @param process
     * @param item
     * @param countDelta Adds items to stack if positive, reduces if negative. If stack count reaches 0 item is destroyed.
     * @param creator
     * @param reference
     * @return Amount of items left.
     */
    public boolean updateItemCountNoDbUpdate(String process, Item item, long countDelta, Player creator, Object reference) {
        final InventoryUpdate iu = new InventoryUpdate();
        final long left = item.getCount() + countDelta;
        try {
            if (left > 0) {
                synchronized (item) {
                    if ((process != null) && (process.length() > 0)) {
                        item.changeCount(process, countDelta, creator, reference);
                    } else {
                        item.changeCountWithoutTrace(-1, creator, reference);
                    }
                    item.setLastChange(Item.MODIFIED);
                    refreshWeight();
                    iu.addModifiedItem(item);
                    return true;
                }
            } else if (left == 0) {
                iu.addRemovedItem(item);
                destroyItem(process, item, _owner, null);
                return true;
            } else {
                return false;
            }
        } finally {
            if (Config.FORCE_INVENTORY_UPDATE) {
                _owner.sendItemList();
            } else {
                _owner.sendInventoryUpdate(iu);
            }
        }
    }

    /**
     * Reduces item count in the stack, destroys item when count reaches 0.
     *
     * @param process
     * @param item
     * @param countDelta Adds items to stack if positive, reduces if negative. If stack count reaches 0 item is destroyed.
     * @param creator
     * @param reference
     * @return Amount of items left.
     */
    public boolean updateItemCount(String process, Item item, long countDelta, Player creator, Object reference) {
        if (item != null) {
            try {
                return updateItemCountNoDbUpdate(process, item, countDelta, creator, reference);
            } finally {
                if (item.getCount() > 0) {
                    item.updateDatabase();
                }
            }
        }
        return false;
    }

    public long getRustyCoin() {
        return nonNull(rustyCoin) ? rustyCoin.getCount() : 0;
    }

    public long getSilverCoin() {
        return nonNull(silverCoin) ? silverCoin.getCount() : 0;
    }
}

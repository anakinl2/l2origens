package org.l2j.gameserver.data.xml.impl;

import io.github.joealisson.primitive.maps.IntObjectMap;
import io.github.joealisson.primitive.maps.impl.CHashIntObjectMap;
import org.l2j.gameserver.data.database.data.DailyMissionPlayerData;
import org.l2j.gameserver.model.StatsSet;
import org.l2j.gameserver.model.actor.instance.L2PcInstance;
import org.l2j.gameserver.model.base.ClassId;
import org.l2j.gameserver.model.dailymission.DailyMissionDataHolder;
import org.l2j.gameserver.model.holders.ItemHolder;
import org.l2j.gameserver.settings.ServerSettings;
import org.l2j.gameserver.util.GameXmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.l2j.commons.configuration.Configurator.getSettings;
import static org.l2j.commons.util.Util.isNullOrEmpty;

/**
 * @author Sdw
 */
public class DailyMissionData extends GameXmlReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(DailyMissionData.class);

    private final IntObjectMap<IntObjectMap<DailyMissionPlayerData>> missionsData = new CHashIntObjectMap<>();
    private final Map<Integer, List<DailyMissionDataHolder>> dailyMissionRewards = new LinkedHashMap<>();

    private boolean available;

    private DailyMissionData() {
        load();
    }

    @Override
    protected Path getSchemaFilePath() {
        return getSettings(ServerSettings.class).dataPackDirectory().resolve("data/xsd/DailyMission.xsd");
    }

    @Override
    public void load() {
        dailyMissionRewards.clear();
        parseDatapackFile("data/DailyMission.xml");
        available = !dailyMissionRewards.isEmpty();
        LOGGER.info("Loaded {} one day rewards.",  dailyMissionRewards.size());
    }

    @Override
    public void parseDocument(Document doc, File f) {
        forEach(doc, "list", listNode -> forEach(listNode, "mission", missionNode -> {
            final StatsSet set = new StatsSet(parseAttributes(missionNode));

            final List<ItemHolder> items = new ArrayList<>(1);

            forEach(missionNode, "reward", itemNode -> {
                final int itemId = parseInteger(itemNode.getAttributes(), "id");
                final int itemCount = parseInteger(itemNode.getAttributes(), "count");
                items.add(new ItemHolder(itemId, itemCount));
            });

            set.set("rewards", items);

            final List<ClassId> classRestriction = new ArrayList<>(1);
            forEach(missionNode, "classes", classesNode -> {
                if(isNullOrEmpty(classesNode.getTextContent())) {
                    return;
                }
                classRestriction.addAll(Arrays.stream(classesNode.getTextContent().split(" ")).map(id -> ClassId.getClassId(Integer.parseInt(id))).collect(Collectors.toList()));
            });

            set.set("classRestriction", classRestriction);

            // Initial values in case handler doesn't exists
            set.set("handler", "");
            set.set("params", StatsSet.EMPTY_STATSET);

            // Parse handler and parameters
            forEach(missionNode, "handler", handlerNode -> {
                set.set("handler", parseString(handlerNode.getAttributes(), "name"));

                final StatsSet params = new StatsSet();
                set.set("params", params);
                forEach(handlerNode, "param", paramNode -> params.set(parseString(paramNode.getAttributes(), "name"), paramNode.getTextContent()));
            });

            final DailyMissionDataHolder holder = new DailyMissionDataHolder(set);
            dailyMissionRewards.computeIfAbsent(holder.getId(), k -> new ArrayList<>()).add(holder);
        }));
    }

    public Collection<DailyMissionDataHolder> getDailyMissions() {
        //@formatter:off
        return dailyMissionRewards.values()
                .stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
        //@formatter:on
    }

    public Collection<DailyMissionDataHolder> getDailyMissions(L2PcInstance player) {
        //@formatter:off
        return dailyMissionRewards.values()
                .stream()
                .flatMap(List::stream)
                .filter(o -> o.isDisplayable(player))
                .collect(Collectors.toList());
        //@formatter:on
    }

    public int getAvailableDailyMissionCount(L2PcInstance player) {
        return (int) dailyMissionRewards.values().stream().flatMap(List::stream).filter(mission -> mission.isAvailable(player)).count();
    }

    public Collection<DailyMissionDataHolder> getDailyMissions(int id) {
        return dailyMissionRewards.get(id);
    }

    public void clearMissionData(int id) {
        missionsData.values().forEach(map -> map.remove(id));
    }

    public void storeMissionData(int missionId, DailyMissionPlayerData data) {
        if(nonNull(data)) {
            missionsData.computeIfAbsent(data.getObjectId(), id -> new CHashIntObjectMap<>()).putIfAbsent(missionId, data);
        }
    }

    public IntObjectMap<DailyMissionPlayerData> getStoredDailyMissionData(L2PcInstance player) {
        return missionsData.computeIfAbsent(player.getObjectId(), id -> new CHashIntObjectMap<>());
    }

    public boolean isAvailable() {
        return available;
    }

    public static DailyMissionData getInstance() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final DailyMissionData INSTANCE = new DailyMissionData();
    }
}

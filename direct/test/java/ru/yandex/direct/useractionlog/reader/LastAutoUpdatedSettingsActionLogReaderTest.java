package ru.yandex.direct.useractionlog.reader;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Test;

import ru.yandex.direct.useractionlog.CampaignId;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.model.AutoChangeableSettings;
import ru.yandex.direct.useractionlog.model.AutoUpdatedSettingsEvent;
import ru.yandex.direct.useractionlog.model.RecommendationsManagementHistory;
import ru.yandex.direct.useractionlog.schema.ObjectPath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

public class LastAutoUpdatedSettingsActionLogReaderTest {

    private static final String RECOMMENDATIONS_MANAGEMENT_ENABLED = "recommendations_management_enabled";
    private static final String DAY_BUDGET = "day_budget";
    private static final String STRATEGY_DATA = "strategy_data";
    private static final String PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED = "price_recommendations_management_enabled";

    private final LastAutoUpdatedSettingsActionLogReader reader = new LastAutoUpdatedSettingsActionLogReader(null);
    private final ClientId clientId = new ClientId(64938245L);

    @Test
    public void testEnrichEventsWithSettingsData_success(){
        var actualResult = reader.enrichEventsWithSettingsData(getSettings(), getEvents());
        assertNotNull(actualResult);
        assertFalse(actualResult.isEmpty());
        actualResult.forEach(e -> {
            assertNotNull(e.getSettings());
            assertEquals(e.getItem(), e.getSettings().getItem());
            assertEquals(e.getSubitem(), e.getSettings().getSubitem());
        });
    }

    @Test
    public void testEnrichIsAutoEnabledData_success(){
        var events = reader.enrichEventsWithSettingsData(getSettings(), getEvents());
        var actualResult = reader.enrichIsAutoEnabledData(getHistoryRecords(), events);
        var eventsMap = listToMap(actualResult,
                e -> new SettingsEventKey(e.getPath(), e.getItem(), e.getSubitem()));

        //сначала опция была выключена, потом её включили
        var switchedToEnabledEvent = getEventByCompositeKey(eventsMap, 57254816L,
                STRATEGY_DATA, "sum");
        assertTrue(switchedToEnabledEvent.isAutoApplyEnabled());

        //сначала опция была включена, потом её выключили
        var switchedToDisabledEvent = getEventByCompositeKey(eventsMap, 57254816L,
                STRATEGY_DATA, "avg_bid");
        assertFalse(switchedToDisabledEvent.isAutoApplyEnabled());

        //опция всегда была включена
        var alwaysEnabledEvent = getEventByCompositeKey(eventsMap, 65126691L,
                DAY_BUDGET, null);
        assertTrue(alwaysEnabledEvent.isAutoApplyEnabled());

        //опция была включена, применилась рекомендация, после чего опцию отключили и снова включили
        var enabledToDisabledToEnabledEvent = getEventByCompositeKey(eventsMap, 65126600L,
                DAY_BUDGET, null);
        assertTrue(enabledToDisabledToEnabledEvent.isAutoApplyEnabled());
    }

    @Test
    public void testFilterEventsBeforeRecommendationManagementEnabling_success(){
        var events = reader.enrichEventsWithSettingsData(getSettings(), getEvents());
        var filteredEvents = reader.filterEventsBeforeRecommendationManagementEnabling(events, getHistoryRecords());
        var filteredEventsMap =
                listToMap(filteredEvents, e -> new SettingsEventKey(e.getPath(), e.getItem(), e.getSubitem()));
        assertNotNull(filteredEvents);
        assertEquals(filteredEvents.size(), 3);

        //сначала опция была выключена, потом её включили, после чего применилась рекомендация
        var switchedToEnabledEvent = getEventByCompositeKey(filteredEventsMap, 57254816L,
                STRATEGY_DATA, "sum");
        assertNotNull(switchedToEnabledEvent);

        //та же кампания. другая опция была включена, применилась рекомендация, после чего опцию отключили
        var switchedToDisabledEvent = getEventByCompositeKey(filteredEventsMap, 57254816L,
                STRATEGY_DATA, "avg_bid");
        assertNotNull(switchedToDisabledEvent);

        //опция всегда была включена
        var alwaysEnabledEvent = getEventByCompositeKey(filteredEventsMap, 65126691L,
                DAY_BUDGET, null);
        assertNotNull(alwaysEnabledEvent);

        //опция была включена, применилась рекомендация, после чего опцию отключили и снова включили
        var enabledToDisabledToEnabledEvent = getEventByCompositeKey(filteredEventsMap, 65126600L,
                DAY_BUDGET, null);
        assertNull(enabledToDisabledToEnabledEvent);
    }

    private AutoUpdatedSettingsEvent getEventByCompositeKey(
            Map<SettingsEventKey, AutoUpdatedSettingsEvent> eventsMap, Long cid,
            String item, String subItem){
        return eventsMap.get(new SettingsEventKey(getCampaignPathByCid(cid), item, subItem));

    }

    private List<AutoChangeableSettings> getSettings(){
        return List.of(
                new AutoChangeableSettings()
                        .withType("campaigns")
                        .withItem(STRATEGY_DATA)
                        .withSubitem("sum")
                        .withRecommendationOptionName(PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED),
                new AutoChangeableSettings()
                        .withType("campaigns")
                        .withItem(STRATEGY_DATA)
                        .withSubitem("avg_bid")
                        .withRecommendationOptionName(RECOMMENDATIONS_MANAGEMENT_ENABLED),
                new AutoChangeableSettings()
                        .withType("campaigns")
                        .withItem(DAY_BUDGET)
                        .withSubitem(null)
                        .withRecommendationOptionName(RECOMMENDATIONS_MANAGEMENT_ENABLED)
        );
    }

    private List<RecommendationsManagementHistory> getHistoryRecords(){
        return List.of(
                        //сначала опция была выключена, потом её включили
                        new RecommendationsManagementHistory(
                                PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED,
                                getCampaignPathByCid(57254816L),
                                LocalDateTime.of(2022, 6, 1, 0, 0),
                                "0"
                        ),
                        new RecommendationsManagementHistory(
                                PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED,
                                getCampaignPathByCid(57254816L),
                                LocalDateTime.of(2022, 6, 25, 11, 29),
                                "1"
                        ),
                        //изначально опция была включена, но потом её выключили
                        new RecommendationsManagementHistory(
                                RECOMMENDATIONS_MANAGEMENT_ENABLED,
                                getCampaignPathByCid(57254816L),
                                LocalDateTime.of(2022, 6, 1, 0, 0),
                                "1"
                        ),
                        new RecommendationsManagementHistory(
                                RECOMMENDATIONS_MANAGEMENT_ENABLED,
                                getCampaignPathByCid(57254816L),
                                LocalDateTime.of(2022, 6, 25, 10, 0),
                                "0"
                        ),
                        //всегда была включена
                        new RecommendationsManagementHistory(
                                RECOMMENDATIONS_MANAGEMENT_ENABLED,
                                getCampaignPathByCid(65126691L),
                                LocalDateTime.of(2022, 6, 1, 0, 0),
                                "1"
                        ),
                        //была применена рекомендация (25.06.2022 в 14:22), опцию отключили и снова включили
                        new RecommendationsManagementHistory(
                                RECOMMENDATIONS_MANAGEMENT_ENABLED,
                                getCampaignPathByCid(65126600L),
                                LocalDateTime.of(2022, 6, 25, 14, 30),
                                "0"
                        ),
                        new RecommendationsManagementHistory(
                                RECOMMENDATIONS_MANAGEMENT_ENABLED,
                                getCampaignPathByCid(65126600L),
                                LocalDateTime.of(2022, 6, 25, 18, 0),
                        "1"
                )
                );
    }

    private List<AutoUpdatedSettingsEvent> getEvents(){
        return List.of(new AutoUpdatedSettingsEvent()
                        .withPath(getCampaignPathByCid(57254816L))
                        .withItem(STRATEGY_DATA)
                        .withSubitem("sum")
                        .withOldValue("700")
                        .withNewValue("1000")
                        .withTargetObjectId(57254816L)
                        .withLastAutoUpdateTime(LocalDateTime.of(2022, 6, 25, 11, 30)),

                new AutoUpdatedSettingsEvent()
                        .withPath(getCampaignPathByCid(57254816L))
                        .withItem(STRATEGY_DATA)
                        .withSubitem("avg_bid")
                        .withOldValue("5")
                        .withNewValue("6")
                        .withTargetObjectId(57254816L)
                        .withLastAutoUpdateTime(LocalDateTime.of(2022, 6, 25, 9, 15)),

                new AutoUpdatedSettingsEvent()
                        .withPath(getCampaignPathByCid(65126691L))
                        .withItem(DAY_BUDGET)
                        .withSubitem(null)
                        .withOldValue("500")
                        .withNewValue("660")
                        .withTargetObjectId(65126691L)
                        .withLastAutoUpdateTime(LocalDateTime.of(2022, 6, 25, 7, 45)),

                new AutoUpdatedSettingsEvent()
                        .withPath(getCampaignPathByCid(65126600L))
                        .withItem(DAY_BUDGET)
                        .withSubitem(null)
                        .withOldValue("8000")
                        .withNewValue("8750")
                        .withTargetObjectId(65126600L)
                        .withLastAutoUpdateTime(LocalDateTime.of(2022, 6, 25, 14, 22))
        );
    }

    private ObjectPath.CampaignPath getCampaignPathByCid(long cid){
        return new ObjectPath.CampaignPath(clientId, new CampaignId(cid));
    }

    private static class SettingsEventKey {
        private final ObjectPath path;
        private final String item;
        private final String subitem;

        public SettingsEventKey(ObjectPath path, String item, String subitem) {
            this.path = path;
            this.item = item;
            this.subitem = subitem;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SettingsEventKey that = (SettingsEventKey) o;
            return Objects.equals(path, that.path) && Objects.equals(item, that.item) && Objects.equals(subitem,
                    that.subitem);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, item, subitem);
        }

        @Override
        public String toString() {
            return "SettingsEventKey{" +
                    "path=" + path +
                    ", item='" + item + '\'' +
                    ", subitem='" + subitem + '\'' +
                    '}';
        }
    }
}

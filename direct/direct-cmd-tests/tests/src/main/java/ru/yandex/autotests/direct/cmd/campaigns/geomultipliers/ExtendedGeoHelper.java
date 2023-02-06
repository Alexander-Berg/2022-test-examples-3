package ru.yandex.autotests.direct.cmd.campaigns.geomultipliers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.autotests.direct.cmd.data.commons.campaign.extendedgeo.ExtendedGeoItem;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class ExtendedGeoHelper {

    /**
     * Позволяет получить Map<String, ExtendedGeoItem> в параметрах теста
     */
    public static class ExtendedGeoMapBuilder {
        private Map<String, ExtendedGeoItem> map = new HashMap<>();

        public static ExtendedGeoMapBuilder builder() {
            return new ExtendedGeoMapBuilder();
        }

        public ExtendedGeoMapBuilder add(String id, ExtendedGeoItem extendedGeoItem) {
            map.put(id, extendedGeoItem);
            return this;
        }

        /**
         * заменяет 1 и 2 на реальные id групп и возвращает мапу ExtendedGeoItem
         */
        public Map<String, ExtendedGeoItem> build(Long adGroupId1, Long adGroupId2) {
            for (ExtendedGeoItem extendedGeoItem : map.values()) {
                if (extendedGeoItem.getPartly() != null && extendedGeoItem.getPartly().getGroupIds() != null) {
                    extendedGeoItem.getPartly().withGroupIds(
                            getReplaced(extendedGeoItem.getPartly().getGroupIds(), adGroupId1, adGroupId2));
                }
                if (extendedGeoItem.getNegative() != null && extendedGeoItem.getNegative().getPartly() != null
                        && extendedGeoItem.getNegative().getPartly().getGroupIds() != null)
                {
                    extendedGeoItem.getNegative().getPartly()
                            .withGroupIds(
                                    getReplaced(extendedGeoItem.getNegative().getPartly().getGroupIds(), adGroupId1,
                                            adGroupId2));
                }
            }
            return map;
        }

        private String[] getReplaced(List<String> ids, Long adGroupId1, Long adGroupId2) {
            return ids.stream()
                    .map(id -> "1".equals(id) ? adGroupId1.toString() : "2".equals(id) ? adGroupId2.toString() : "0")
                    .collect(toList())
                    .toArray(new String[ids.size()]);
        }

        @Override
        public String toString() {
            return map.keySet().stream().map(String::valueOf).collect(joining(","));
        }
    }
}

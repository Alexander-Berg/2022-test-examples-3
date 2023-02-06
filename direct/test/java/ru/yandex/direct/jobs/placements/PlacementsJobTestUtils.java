package ru.yandex.direct.jobs.placements;

import static java.util.Arrays.asList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

class PlacementsJobTestUtils {

    static final int FIELDS_NUM = QueryField.values().length;
    static final String[] FIELD_NAMES =
            mapList(asList(QueryField.values()), q -> q.name).toArray(new String[0]);
    static final String[] FIELD_TYPES =
            new String[]{"Int64", "String", "String", "String", "Boolean", "Boolean", "Boolean", "String", "String", "Int64", "String"};
}

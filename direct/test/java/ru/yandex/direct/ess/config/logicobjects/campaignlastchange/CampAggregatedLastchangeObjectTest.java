package ru.yandex.direct.ess.config.logicobjects.campaignlastchange;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.ess.common.utils.TablesEnum;
import ru.yandex.direct.ess.config.ConverterUtils;
import ru.yandex.direct.ess.logicobjects.campaignlastchange.CampAggregatedLastchangeObject;

import static org.assertj.core.api.Assertions.assertThat;

class CampAggregatedLastchangeObjectTest {

    @Test
    void testCampAggregatedLastchangeObject_SerializeAndDeserialize() {
        List<CampAggregatedLastchangeObject> objects = new ArrayList<>();
        objects.add(new CampAggregatedLastchangeObject(
                TablesEnum.PHRASES, 1L, 1L, LocalDateTime.of(2019, 2, 19, 22, 0, 0)));
        objects.add(new CampAggregatedLastchangeObject(
                TablesEnum.BANNERS, 0L, 0L, null));
        objects.add(new CampAggregatedLastchangeObject(TablesEnum.BANNER_IMAGES, 0L, 0L,
                LocalDateTime.of(2100, 1, 1, 1, 1, 1)));
        objects.add(new CampAggregatedLastchangeObject(null, null, null, null));

        var gotObjects = ConverterUtils.logicObjectsSerializeDeserialize(objects);
        assertThat(gotObjects).containsExactlyElementsOf(objects);
    }
}

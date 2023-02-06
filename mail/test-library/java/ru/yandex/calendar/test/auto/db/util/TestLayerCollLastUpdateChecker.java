package ru.yandex.calendar.test.auto.db.util;

import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.beans.generated.Layer;
import ru.yandex.calendar.logic.layer.LayerDao;
import ru.yandex.misc.test.Assert;

/**
 * @author gutman
 */
public class TestLayerCollLastUpdateChecker {

    @Autowired
    private LayerDao layerDao;

    public void assertUpdated(ListF<Long> layerIds, Instant expectedCollLastUpdate) {
        ListF<Layer> layers = layerDao.findLayersByIds(layerIds);

        for (Layer layer : layers) {
            Assert.equals(expectedCollLastUpdate, layer.getCollLastUpdateTs());
        }
    }

    public void assertUpdated(long layerId, Instant expectedCollLastUpdate) {
        assertUpdated(Cf.list(layerId), expectedCollLastUpdate);
    }

}

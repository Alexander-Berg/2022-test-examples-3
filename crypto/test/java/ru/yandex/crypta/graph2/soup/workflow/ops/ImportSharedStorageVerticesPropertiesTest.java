package ru.yandex.crypta.graph2.soup.workflow.ops;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph2.dao.yt.bendable.YsonMultiEntitySupport;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.dao.yt.local.StatisticsSlf4jLoggingImpl;
import ru.yandex.crypta.graph2.model.soup.props.CommonShared;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImportSharedStorageVerticesPropertiesTest {

    private YsonMultiEntitySupport serializer = new YsonMultiEntitySupport();

    private Optional<CommonShared> processInMapper(String idType, ListF<String> sharedTypes) {
        YTreeMapNode rec = YTree.mapBuilder()
                .key("id").value("y1")
                .key("id_type").value(idType)
                .key("shared_types").value(sharedTypes)
                .buildMap();

        // for deserialization
        rec.put("cryptaId", YTree.stringNode("fake"));

        ImportSharedStorageVerticesProperties mapper = new ImportSharedStorageVerticesProperties();

        LocalYield<YTreeMapNode> yield = new LocalYield<>();
        mapper.map(rec, yield, new StatisticsSlf4jLoggingImpl());
        List<YTreeMapNode> allRecs = yield.getAllRecs();

        return allRecs.stream().findFirst().map(r -> serializer.parse(r, CommonShared.class));
    }

    @Test
    public void correctlyProcessesSharedVertices() {
        ListF<String> sharedTypes = Cf.list("shared_type_1", "shared_type_1");
        Optional<CommonShared> correct = processInMapper("yandexuid", sharedTypes);

        assertTrue(correct.isPresent());
        assertEquals(EIdType.YANDEXUID, correct.get().getVertex().getIdType());
//        assertEquals(sharedTypes, correct.get().getSharedTypes());
    }

    @Test
    public void filterSomeSharedTypes() {
        ListF<String> sharedTypes = Cf.list("HEURISTIC_DESKTOP");
        Optional<CommonShared> correct = processInMapper("yandexuid", sharedTypes);

        assertFalse(correct.isPresent());
    }
}

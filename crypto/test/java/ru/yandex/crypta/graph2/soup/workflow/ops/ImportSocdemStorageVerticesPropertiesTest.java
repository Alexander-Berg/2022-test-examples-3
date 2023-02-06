package ru.yandex.crypta.graph2.soup.workflow.ops;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import ru.yandex.crypta.graph2.dao.yt.bendable.YsonMultiEntitySupport;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.dao.yt.local.StatisticsSlf4jLoggingImpl;
import ru.yandex.crypta.graph2.model.soup.props.VertexExactSocdem;
import ru.yandex.crypta.graph2.model.soup.props.info.ExactSocdem.Gender;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.crypta.graph2.dao.yt.utils.YTreeUtils.YSON_NULL;

public class ImportSocdemStorageVerticesPropertiesTest {

    private YsonMultiEntitySupport serializer = new YsonMultiEntitySupport();

    private Optional<VertexExactSocdem> processInMapper(String idType, String gender) throws Exception {
        YTreeMapNode rec = YTree.mapBuilder()
                .key("id").value("y1")
                .key("id_type").value(idType)
                .key("gender").value(gender)
                .key("year_of_birth").value(YSON_NULL)
                .key("source").value("test_source")
                .key("socdemSource").value(YSON_NULL)
                .buildMap();

        // for deserialization
        rec.put("cryptaId", YTree.stringNode("fake"));

        ImportSocdemStorageVerticesProperties mapper = new ImportSocdemStorageVerticesProperties();

        LocalYield<YTreeMapNode> yield = new LocalYield<>();
        mapper.map(rec, yield, new StatisticsSlf4jLoggingImpl());
        List<YTreeMapNode> allRecs = yield.getAllRecs();

        return allRecs.stream().findFirst().map(r -> serializer.parse(r, VertexExactSocdem.class));
    }

    @Test
    public void correctlyProcessesIdTypeAndSocdem() throws Exception {
        Optional<VertexExactSocdem> correct = processInMapper("yandexuid", "m");
        assertTrue(correct.isPresent());
        assertEquals(EIdType.YANDEXUID, correct.get().getVertex().getIdType());
        assertEquals(Gender.MALE, correct.get().getSocdem().getGender().get());

        Optional<VertexExactSocdem> fixedGender = processInMapper("yandexuid", "male");
        assertTrue(fixedGender.isPresent());
        assertEquals(EIdType.YANDEXUID, fixedGender.get().getVertex().getIdType());
        assertEquals(Gender.MALE, fixedGender.get().getSocdem().getGender().get());

        Optional<VertexExactSocdem> fixedIdType = processInMapper("yandexuid", "male");
        assertTrue(fixedIdType.isPresent());
        assertEquals(EIdType.YANDEXUID, fixedIdType.get().getVertex().getIdType());
        assertEquals(Gender.MALE, fixedIdType.get().getSocdem().getGender().get());

        Optional<VertexExactSocdem> filteredBySocdem = processInMapper("yandexuid", "male1");
        assertFalse(filteredBySocdem.isPresent());

    }

}

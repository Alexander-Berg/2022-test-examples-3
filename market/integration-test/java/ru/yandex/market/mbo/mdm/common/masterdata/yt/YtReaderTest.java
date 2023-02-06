package ru.yandex.market.mbo.mdm.common.masterdata.yt;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.common.http.HttpUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.lightmapper.ProtobufMapper;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

/**
 * @author amaslak
 */
public class YtReaderTest extends MdmBaseIntegrationTestClass {
    private static final int SEED = -4;
    private static final int YT_INIT_TIMEOUT = 10;
    private static final int DATASET_SIZE_LARGE = 100;
    private static final int PARALLEL_BATCH_SIZE = 10;

    private EnhancedRandom random;

    private IrisYtReader mdmIrisYtReader;

    @Value("${market.mdm.yt.iris-reference-table.test-prefix}")
    private String irisTablePrefix;

    @Autowired
    @Qualifier("hahnYtHttpApi")
    private UnstableInit<Yt> hahnYtHttpApi;

    private Yt yt;
    private YPath root;
    private YPath irisTable;

    @Before
    public void setUp() throws Exception {
        random = TestDataUtils.defaultRandom(SEED);
        yt = hahnYtHttpApi.get(YT_INIT_TIMEOUT, TimeUnit.SECONDS);
        root = YPath.simple(irisTablePrefix).child(UUID.randomUUID().toString());
        yt.cypress().create(
            root, CypressNodeType.MAP, true, false,
            Map.of(
                "expiration_time",
                YTree.stringNode(HttpUtils.YT_INSTANT_FORMATTER.format(Instant.now().plus(Duration.ofDays(1))))
            )
        );
        irisTable = root.child("complete_reference_information");
        createCompleteInformationTable(yt, irisTable);
        mdmIrisYtReader = new IrisYtReader(yt, irisTable.toString());
    }

    @After
    public void tearDown() {
        yt.cypress().remove(root);
    }

    @Test
    public void shouldReadCompleteItemsFromYtInParallel() {
        ProtobufMapper<MdmIrisPayload.CompleteItem> protobufMapper =
            new ProtobufMapper<>(MdmIrisPayload.CompleteItem::getDefaultInstance);
        Set<MdmIrisPayload.CompleteItem> dataset =
            random.objects(MdmIrisPayload.CompleteItem.class, DATASET_SIZE_LARGE).collect(Collectors.toSet());

        List<YTreeMapNode> data = dataset.stream().map(item -> YTree.mapBuilder()
            .key("complete_item")
            .value(protobufMapper.serializeMessage(item))
            .buildMap()
        ).collect(Collectors.toList());

        yt.tables().write(irisTable, YTableEntryTypes.YSON, data);

        Set<MdmIrisPayload.CompleteItem> completeItems = new HashSet<>(DATASET_SIZE_LARGE);
        int count = mdmIrisYtReader.read(PARALLEL_BATCH_SIZE, completeItems::addAll);

        Assertions.assertThat(count).isEqualTo(dataset.size());
        Assertions.assertThat(completeItems).isEqualTo(dataset);
    }

    private void createCompleteInformationTable(Yt yt, YPath irisTable) {
        yt.cypress().create(
            irisTable,
            CypressNodeType.TABLE,
            true,
            false,
            Map.of("schema", YTree.builder()
                .beginAttributes()
                .key("unique_keys").value(false)
                .key("strict").value(true)
                .endAttributes()
                .beginList()
                .beginMap()
                .key("name").value("complete_item")
                .key("type").value("any")
                .endMap()
                .endList()
                .build()
            ));
    }
}

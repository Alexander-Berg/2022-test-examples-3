package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.operations.specs.MergeSpec;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.yt.ytclient.proxy.request.CreateNode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
abstract class AbstractExportExecutorTest extends AbstractContextualTest {

    @Autowired
    protected Yt hahnYt;

    @Autowired
    protected Cypress cypress;

    @Autowired
    protected YtTables ytTables;

    @Autowired
    protected YtOperations ytOperations;

    @Autowired
    protected ObjectMapper objectMapper;

    protected abstract void setupExecutor();

    protected abstract String getDirectoryPath();

    @BeforeEach
    void setup() {
        setupExecutor();
        clock.setFixed(Instant.parse("2021-07-31T09:00:00Z"), DateTimeUtils.MOSCOW_ZONE);
        when(hahnYt.cypress()).thenReturn(cypress);
        when(hahnYt.tables()).thenReturn(ytTables);
        when(hahnYt.operations()).thenReturn(ytOperations);
        mockGetDirectoryContent();
    }

    private void mockGetDirectoryContent() {
        when(cypress.get(any(YPath.class))).thenReturn(new YTreeMapNodeImpl(
            Map.of(
                "20210731_090000", new YTreeMapNodeImpl(null),
                "20210731_120000", new YTreeMapNodeImpl(null),
                "20210731_110000", new YTreeMapNodeImpl(null),
                "20210731_100000", new YTreeMapNodeImpl(null)
            ),
            null
        ));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(cypress, ytTables);
    }


    @SneakyThrows
    protected void verifyRowsWritten(String... expectedJsonPaths) {
        ArgumentCaptor<List<JsonNode>> captor = ArgumentCaptor.forClass(List.class);
        verify(ytTables, times(expectedJsonPaths.length)).write(
            eq(YPath.simple(getDirectoryPath() + "/20210731_120000").append(true)),
            eq(YTableEntryTypes.JACKSON),
            captor.capture()
        );
        StreamEx.of(expectedJsonPaths)
            .zipWith(captor.getAllValues().stream().map(this::toJsonString))
            .forKeyValue(this::assertJsonEquals);
    }

    protected void verifyLinkCreated() {
        verify(cypress).link(
            YPath.simple(getDirectoryPath() + "/20210731_120000").append(true),
            YPath.simple(getDirectoryPath() + "/recent"),
            true
        );
    }

    protected void verifyDirectoryContentGot() {
        verify(cypress).get(YPath.simple(getDirectoryPath()));
    }

    protected void verifyOutdatedTablesRemoved() {
        verify(cypress).remove(YPath.simple(getDirectoryPath() + "/20210731_090000"));
        verify(cypress).remove(YPath.simple(getDirectoryPath() + "/20210731_100000"));
        verify(cypress).remove(YPath.simple(getDirectoryPath() + "/20210731_110000"));
    }

    protected void verifyTableRemoved() {
        verify(cypress).remove(YPath.simple(getDirectoryPath() + "/20210731_120000").append(true));
    }

    protected void verifyChunksCombined() {
        YPath path = YPath.simple(getDirectoryPath() + "/20210731_120000").append(false);
        verify(ytOperations).merge(safeRefEq(
            MergeSpec.builder()
                .addInputTable(path)
                .setOutputTable(path)
                .setCombineChunks(true)
                .build()
        ));
    }

    @Nonnull
    @SneakyThrows
    protected String toJsonString(Object object) {
        return objectMapper.writeValueAsString(object);
    }


    protected void verifyTableCreated() {
        ArgumentCaptor<CreateNode> captor = ArgumentCaptor.forClass(CreateNode.class);
        verify(cypress).create(captor.capture());
        CreateNode createNode = captor.getValue();
        softly.assertThat(createNode.getPath().toString())
            .isEqualTo(getDirectoryPath() + "/20210731_120000");
        softly.assertThat(createNode.getType().toCypressNodeType()).isEqualTo(CypressNodeType.TABLE);
        softly.assertThat(createNode.getAttributes().get("schema")).isNotNull();
    }

    @SneakyThrows
    private void assertJsonEquals(String path, String content) {
        IntegrationTestUtils.assertJson(path, content);
    }

}

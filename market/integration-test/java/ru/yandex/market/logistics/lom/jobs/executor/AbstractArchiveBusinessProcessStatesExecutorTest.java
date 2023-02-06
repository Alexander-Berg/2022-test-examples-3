package ru.yandex.market.logistics.lom.jobs.executor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.entity.enums.EntityType;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateEntityIdYdb;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateYdb;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.service.businessProcess.AbstractBusinessProcessStateYdbServiceTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus.SYNC_PROCESS_SUCCEEDED;

@ParametersAreNonnullByDefault
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public abstract class AbstractArchiveBusinessProcessStatesExecutorTest
    extends AbstractBusinessProcessStateYdbServiceTest {

    private static final String EXPECTED_TABLE_PATH =
        "//home/market/testing/delivery/logistics_lom/business_process_state_archive";
    protected static final Instant NOW_TIME = Instant.parse("2020-12-04T07:40:35Z");

    @Autowired
    protected Yt hahnYt;

    @Autowired
    private YtTables ytTables;

    @Autowired
    private Operation ytOperation;

    @BeforeEach
    void setup() {
        clock.setFixed(NOW_TIME, DateTimeUtils.MOSCOW_ZONE);
        when(hahnYt.tables()).thenReturn(ytTables);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(ytTables, hahnYt, ytOperation);
    }

    @Test
    @DisplayName("Размер батча и число батчей не указаны в internal_variable")
    abstract void noInternalVariablesSet();

    @Test
    @DisplayName("С заданными размером батча и числом процессов в батче")
    abstract void withInternalVariables();

    @Test
    @DisplayName("Нет процессов для архивирования")
    abstract void noProcessesToArchive();

    protected void noProcessesToArchiveLogged() {
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Not enough records found for processing, stopping job");
    }

    protected void verifyWriteRows(YTreeMapNode... nodes) {
        verify(ytTables).write(
            YPath.simple(EXPECTED_TABLE_PATH).append(true),
            YTableEntryTypes.YSON,
            Arrays.asList(nodes)
        );
    }

    @Nonnull
    protected YTreeMapNode buildMapNode(
        long id,
        long updated
    ) {
        return (YTreeMapNode) new YTreeBuilder()
            .beginMap()
            .key("id").value(id)
            .key("created").value(1600414655000L)
            .key("updated").value(updated)
            .key("queue_type").value(QueueType.VALIDATE_ORDER_EXTERNAL.name())
            .key("status").value(SYNC_PROCESS_SUCCEEDED.name())
            .key("author").value("{\"abcServiceId\":" + id + ",\"yandexUid\":" + id + "}")
            .key("payload").value("payload" + id)
            .key("comment").value("comment" + id)
            .key("sequence_id").value(id)
            .key("parent_id").value((Long) null)
            .key("entity_ids").value("[{\"entityId\":" + id + ",\"entityType\":\"ORDER\"}]")
            .endMap().build();
    }

    @Nonnull
    protected BusinessProcessStateYdb ydbProcess(long id, Instant updated) {
        return new BusinessProcessStateYdb()
            .setId(id)
            .setSequenceId(id)
            .setParentId(null)
            .setCreated(Instant.parse("2020-09-18T07:37:35Z"))
            .setUpdated(updated)
            .setQueueType(QueueType.VALIDATE_ORDER_EXTERNAL)
            .setStatus(SYNC_PROCESS_SUCCEEDED)
            .setAuthor(
                new OrderHistoryEventAuthor()
                    .setYandexUid(BigDecimal.valueOf(id))
                    .setTvmServiceId(id)
            )
            .setPayload("payload" + id)
            .setMessage("comment" + id)
            .setEntityIds(List.of(
                new BusinessProcessStateEntityIdYdb()
                    .setEntityId(id)
                    .setEntityType(EntityType.ORDER)
            ));
    }
}

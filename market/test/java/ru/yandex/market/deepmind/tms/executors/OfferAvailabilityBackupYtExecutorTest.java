package ru.yandex.market.deepmind.tms.executors;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.solomon.DeepmindSolomonPushService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;


public class OfferAvailabilityBackupYtExecutorTest extends DeepmindBaseDbTestClass {

    private StorageKeyValueService keyValue;
    private OfferAvailabilityBackupYtExecutor executor;
    private Operation job;

    @Before
    public void setUp() {
        var ytPrimary = Mockito.mock(Yt.class);
        var ytSecondary = Mockito.mock(Yt.class);
        var ytOperations = Mockito.mock(YtOperations.class);
        job = Mockito.mock(Operation.class);
        Mockito.when(ytPrimary.operations()).thenReturn(ytOperations);
        Mockito.when(ytSecondary.operations()).thenReturn(ytOperations);
        Mockito.when(ytOperations.mergeAndGetOp(any())).thenReturn(job);
        Mockito.doNothing().when(job).awaitAndThrowIfNotSuccess();
        Cypress cypress = Mockito.mock(Cypress.class);
        Mockito.when(cypress.list(any(YPath.class))).thenReturn(List.of());
        Mockito.when(ytPrimary.cypress()).thenReturn(cypress);
        Mockito.when(ytSecondary.cypress()).thenReturn(cypress);

        var attributes = Mockito.mock(YTreeNode.class);
        Mockito.when(cypress.get(any(), any())).thenReturn(attributes);
        Mockito.when(attributes.getAttribute(any()))
            .thenReturn(Optional.of(new YTreeIntegerNodeImpl(true, 0, null)));

        var yql = Mockito.mock(JdbcTemplate.class);
        Mockito.when(yql.query(anyString(), any(ResultSetExtractor.class))).thenReturn(Map.of());
        Mockito.when(yql.queryForObject(anyString(), Mockito.eq(Long.class))).thenReturn(0L);

        keyValue = new StorageKeyValueServiceMock();
        executor = new OfferAvailabilityBackupYtExecutor(UnstableInit.simple(ytPrimary),
            UnstableInit.simple(ytSecondary), yql, yql, keyValue, YPath.simple("//tmp"),
            YPath.simple("//tmp"), "pool", Mockito.mock(DeepmindSolomonPushService.class));
    }

    @Test
    public void testRerunningJob() {
        // first run
        Assertions
            .assertThat(keyValue.getLocalDate("offer_availability_last_backup_primary", null)).isNull();
        Assertions
            .assertThat(keyValue.getLocalDate("offer_availability_last_backup_secondary", null)).isNull();

        executor.execute();

        Assertions
            .assertThat(keyValue.getLocalDate("offer_availability_last_backup_primary", null))
            .isEqualTo(LocalDate.now());
        Assertions
            .assertThat(keyValue.getLocalDate("offer_availability_last_backup_secondary", null))
            .isEqualTo(LocalDate.now());

        // check that parameters will not allow running the second time at one day
        Mockito.doThrow(new RuntimeException("Should not be called")).when(job).awaitAndThrowIfNotSuccess();
        // will not run the second time
        executor.execute();
        keyValue.putValue("offer_availability_last_backup_primary", null);
        keyValue.putValue("offer_availability_last_backup_secondary", null);
        Assertions.assertThatThrownBy(() -> executor.execute()).hasMessageContaining("Should not be called");

    }
}

package ru.yandex.direct.core.entity.cashback;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.cashback.model.CashbackRewardDetails;
import ru.yandex.direct.core.entity.cashback.model.CashbackRewardDetailsRow;
import ru.yandex.direct.core.entity.cashback.model.CashbackRewardTableRow;
import ru.yandex.direct.core.entity.cashback.model.CashbackRewardsImportParams;
import ru.yandex.direct.core.entity.cashback.repository.ImportCashbackRewardsRepository;
import ru.yandex.direct.core.entity.cashback.service.CashbackRewardTableRowConsumer;
import ru.yandex.direct.core.entity.cashback.service.ImportCashbackRewardsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@CoreTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class ImportCashbackRewardsServiceTest {
    private static final int DIRECT_SERVICE_ID = 7;
    private static final LocalDate DATE = LocalDate.of(2020, 8, 1);

    @Mock
    private ImportCashbackRewardsRepository repository;

    @Mock
    private YtOperator operator;

    private ImportCashbackRewardsService service;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        var commonConfig = mock(DirectConfig.class);
        doReturn(1).when(commonConfig).getInt("cashbacks.yt.chunk_size");
        doReturn(DIRECT_SERVICE_ID).when(commonConfig).getInt("balance.directServiceId");

        var provider = mock(YtProvider.class);
        doReturn(operator).when(provider).getOperator(any());

        service = new ImportCashbackRewardsService(provider, repository, commonConfig);
    }

    @Test
    public void testImportRewardsTable_emptyTable() {
        service.importRewardsTable(defaultParams());

        verifyZeroInteractions(repository);
    }

    @Test
    public void testmportRewardsTable_notEmptyTable() {
        var clientId = ClientId.fromLong(1L);

        var rowToReturn = mock(CashbackRewardTableRow.class);
        doReturn(clientId.asLong()).when(rowToReturn).getClientId();
        doReturn(DIRECT_SERVICE_ID).when(rowToReturn).getServiceId();
        doReturn("{\"details\": [{\"program_id\": 1, \"reward\": 41.5619939, \"reward_wo_nds\": 34.63504636}]}")
                .when(rowToReturn).getCashbackDetails();

        doAnswer(invocation -> {
            var from = (long) invocation.getArgument(3);
            if (from == 0L) { // Only on first call
                var consumer = (CashbackRewardTableRowConsumer) invocation.getArgument(1);
                consumer.accept(rowToReturn);
            }
            return null;
        }).when(operator).readTableByRowRange(any(), any(), any(), anyLong(), anyLong());

        service.importRewardsTable(defaultParams());

        var expectedDetails = new CashbackRewardDetails()
                .withDetails(List.of(
                        new CashbackRewardDetailsRow()
                                .withProgramId(1L)
                                .withReward(new BigDecimal("41.5619939"))
                                .withRewardWithoutNds(new BigDecimal("34.63504636"))));
        verify(repository).saveRewardDetails(Map.of(clientId, expectedDetails), DATE, true);
    }

    @Test
    public void testImportRewardsTable_manualCashbackWoDetails() {
        var clientId = ClientId.fromLong(1L);
        var rowToReturn = mock(CashbackRewardTableRow.class);
        doReturn(clientId.asLong()).when(rowToReturn).getClientId();
        doReturn(DIRECT_SERVICE_ID).when(rowToReturn).getServiceId();
        doReturn("{}").when(rowToReturn).getCashbackDetails();
        doReturn("3600").when(rowToReturn).getReward();

        doAnswer(invocation -> {
            var from = (long) invocation.getArgument(3);
            if (from == 0L) { // Only on first call
                var consumer = (CashbackRewardTableRowConsumer) invocation.getArgument(1);
                consumer.accept(rowToReturn);
            }
            return null;
        }).when(operator).readTableByRowRange(any(), any(), any(), anyLong(), anyLong());
        service.importRewardsTable(defaultParams());
        var expectedDetails = new CashbackRewardDetails()
                .withDetails(List.of(
                        new CashbackRewardDetailsRow()
                                .withProgramId(14L)
                                .withReward(new BigDecimal("3600"))
                                .withRewardWithoutNds(new BigDecimal("3000"))));
        verify(repository).saveRewardDetails(Map.of(clientId, expectedDetails), DATE, true);
    }

    @Test
    public void testImportRewardsTable_severalRowsForClientDifferentPrograms() {
        var clientId = ClientId.fromLong(1L);
        var rowToReturn1 = mock(CashbackRewardTableRow.class);
        doReturn(clientId.asLong()).when(rowToReturn1).getClientId();
        doReturn(DIRECT_SERVICE_ID).when(rowToReturn1).getServiceId();
        doReturn("{\"details\": [{\"program_id\": 1, \"reward\": 41.5619939, \"reward_wo_nds\": 34.63504636}]}").when(rowToReturn1).getCashbackDetails();
        doReturn("3600").when(rowToReturn1).getReward();

        var rowToReturn2 = mock(CashbackRewardTableRow.class);
        doReturn(clientId.asLong()).when(rowToReturn2).getClientId();
        doReturn(DIRECT_SERVICE_ID).when(rowToReturn2).getServiceId();
        doReturn("{\"details\": [{\"program_id\": 2, \"reward\": 41.5619939, \"reward_wo_nds\": 34.63504636}]}").when(rowToReturn2).getCashbackDetails();
        doReturn("3600").when(rowToReturn2).getReward();

        doAnswer(invocation -> {
            var from = (long) invocation.getArgument(3);
            if (from == 0L) { // Only on first call
                var consumer = (CashbackRewardTableRowConsumer) invocation.getArgument(1);
                consumer.accept(rowToReturn1);
                consumer.accept(rowToReturn2);
            }
            return null;
        }).when(operator).readTableByRowRange(any(), any(), any(), anyLong(), anyLong());
        service.importRewardsTable(defaultParams());
        var expectedDetails = new CashbackRewardDetails()
                .withDetails(List.of(
                        new CashbackRewardDetailsRow()
                                .withProgramId(1L)
                                .withReward(new BigDecimal("41.5619939"))
                                .withRewardWithoutNds(new BigDecimal("34.63504636")),
                        new CashbackRewardDetailsRow()
                                .withProgramId(2L)
                                .withReward(new BigDecimal("41.5619939"))
                                .withRewardWithoutNds(new BigDecimal("34.63504636"))));
        verify(repository).saveRewardDetails(Map.of(clientId, expectedDetails), DATE, true);
    }

    @Test
    public void testImportRewardsTable_severalRowsForClientSamePrograms() {
        var clientId = ClientId.fromLong(1L);
        var rowToReturn1 = mock(CashbackRewardTableRow.class);
        doReturn(clientId.asLong()).when(rowToReturn1).getClientId();
        doReturn(DIRECT_SERVICE_ID).when(rowToReturn1).getServiceId();
        doReturn("{\"details\": [{\"program_id\": 1, \"reward\": 3600, \"reward_wo_nds\": 3000}]}").when(rowToReturn1).getCashbackDetails();
        doReturn("3600").when(rowToReturn1).getReward();

        var rowToReturn2 = mock(CashbackRewardTableRow.class);
        doReturn(clientId.asLong()).when(rowToReturn2).getClientId();
        doReturn(DIRECT_SERVICE_ID).when(rowToReturn2).getServiceId();
        doReturn("{\"details\": [{\"program_id\": 1, \"reward\": 3600, \"reward_wo_nds\": 3000}]}").when(rowToReturn2).getCashbackDetails();
        doReturn("3600").when(rowToReturn2).getReward();

        doAnswer(invocation -> {
            var from = (long) invocation.getArgument(3);
            if (from == 0L) { // Only on first call
                var consumer = (CashbackRewardTableRowConsumer) invocation.getArgument(1);
                consumer.accept(rowToReturn1);
                consumer.accept(rowToReturn2);
            }
            return null;
        }).when(operator).readTableByRowRange(any(), any(), any(), anyLong(), anyLong());
        service.importRewardsTable(defaultParams());
        var expectedDetails = new CashbackRewardDetails()
                .withDetails(List.of(
                        new CashbackRewardDetailsRow()
                                .withProgramId(1L)
                                .withReward(new BigDecimal("7200"))
                                .withRewardWithoutNds(new BigDecimal("6000"))));
        verify(repository).saveRewardDetails(Map.of(clientId, expectedDetails), DATE, true);
    }

    @Test
    public void testImportRewardsTable_severalRowsForClientSameAndDifferentPrograms() {
        var clientId = ClientId.fromLong(1L);
        var rowToReturn1 = mock(CashbackRewardTableRow.class);
        doReturn(clientId.asLong()).when(rowToReturn1).getClientId();
        doReturn(DIRECT_SERVICE_ID).when(rowToReturn1).getServiceId();
        doReturn("{\"details\": [{\"program_id\": 1, \"reward\": 3600, \"reward_wo_nds\": 3000},{\"program_id\": 2, " +
                "\"reward\": 3600, \"reward_wo_nds\": 3000}]}").when(rowToReturn1).getCashbackDetails();
        doReturn("3600").when(rowToReturn1).getReward();

        var rowToReturn2 = mock(CashbackRewardTableRow.class);
        doReturn(clientId.asLong()).when(rowToReturn2).getClientId();
        doReturn(DIRECT_SERVICE_ID).when(rowToReturn2).getServiceId();
        doReturn("{\"details\": [{\"program_id\": 2, \"reward\": 3600, \"reward_wo_nds\": 3000},{\"program_id\": 3, " +
                "\"reward\": 3600, \"reward_wo_nds\": 3000}]}").when(rowToReturn2).getCashbackDetails();
        doReturn("3600").when(rowToReturn2).getReward();

        doAnswer(invocation -> {
            var from = (long) invocation.getArgument(3);
            if (from == 0L) { // Only on first call
                var consumer = (CashbackRewardTableRowConsumer) invocation.getArgument(1);
                consumer.accept(rowToReturn1);
                consumer.accept(rowToReturn2);
            }
            return null;
        }).when(operator).readTableByRowRange(any(), any(), any(), anyLong(), anyLong());
        service.importRewardsTable(defaultParams());
        var expectedDetails = new CashbackRewardDetails()
                .withDetails(List.of(
                        new CashbackRewardDetailsRow()
                                .withProgramId(1L)
                                .withReward(new BigDecimal("3600"))
                                .withRewardWithoutNds(new BigDecimal("3000")),
                        new CashbackRewardDetailsRow()
                                .withProgramId(2L)
                                .withReward(new BigDecimal("7200"))
                                .withRewardWithoutNds(new BigDecimal("6000")),
                        new CashbackRewardDetailsRow()
                                .withProgramId(3L)
                                .withReward(new BigDecimal("3600"))
                                .withRewardWithoutNds(new BigDecimal("3000"))));
        verify(repository).saveRewardDetails(Map.of(clientId, expectedDetails), DATE, true);
    }

    @Test
    public void testmportRewardsTable_entryNotForDirect() {
        var clientId = ClientId.fromLong(1L);

        var rowToReturn = mock(CashbackRewardTableRow.class);
        doReturn(clientId.asLong()).when(rowToReturn).getClientId();
        doReturn(1234).when(rowToReturn).getServiceId();

        doAnswer(invocation -> {
            var from = (long) invocation.getArgument(3);
            if (from == 0L) { // Only on first call
                var consumer = (CashbackRewardTableRowConsumer) invocation.getArgument(1);
                consumer.accept(rowToReturn);
            }
            return null;
        }).when(operator).readTableByRowRange(any(), any(), any(), anyLong(), anyLong());

        service.importRewardsTable(defaultParams());

        verifyZeroInteractions(repository);
    }

    @Test
    public void testImportRewardsTable_manualCashbackWithNullDetails() {
        var clientId = ClientId.fromLong(1L);
        var rowToReturn = mock(CashbackRewardTableRow.class);
        doReturn(clientId.asLong()).when(rowToReturn).getClientId();
        doReturn(DIRECT_SERVICE_ID).when(rowToReturn).getServiceId();
        doReturn(null).when(rowToReturn).getCashbackDetails();
        doReturn("3600").when(rowToReturn).getReward();

        doAnswer(invocation -> {
            var from = (long) invocation.getArgument(3);
            if (from == 0L) { // Only on first call
                var consumer = (CashbackRewardTableRowConsumer) invocation.getArgument(1);
                consumer.accept(rowToReturn);
            }
            return null;
        }).when(operator).readTableByRowRange(any(), any(), any(), anyLong(), anyLong());
        service.importRewardsTable(defaultParams());
        var expectedDetails = new CashbackRewardDetails()
                .withDetails(List.of(
                        new CashbackRewardDetailsRow()
                                .withProgramId(14L)
                                .withReward(new BigDecimal("3600"))
                                .withRewardWithoutNds(new BigDecimal("3000"))));
        verify(repository).saveRewardDetails(Map.of(clientId, expectedDetails), DATE, true);
    }

    private static CashbackRewardsImportParams defaultParams() {
        return new CashbackRewardsImportParams()
                .withCluster(YtCluster.HAHN)
                .withDate(DATE)
                .withTablePath("//some/path/202008");
    }
}

package ru.yandex.market.core.program.partner.calculator.marketplace;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.PartnerStatusResolverResult;
import ru.yandex.market.core.program.partner.model.ProgramArgs;
import ru.yandex.market.core.program.partner.model.ProgramStatus;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.program.partner.status.PartnerStatusService;
import ru.yandex.market.mbi.partner.status.client.model.PartnerStatusInfo;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolverResults;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolverType;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolversResponse;
import ru.yandex.market.mbi.partner.status.client.model.WizardStepStatus;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class ReplicationStatusResolverTest extends FunctionalTest {

    @Autowired
    private ReplicationStatusResolver replicationStatusResolver;

    @Autowired
    private PartnerStatusService partnerStatusService;

    @Test
    @DisplayName("Резолвер отключен")
    void disabled() {
        Optional<ProgramStatus.Builder> result = replicationStatusResolver.resolveWithMetrics(100L, ProgramArgs.DEFAULT);
        Assertions.assertThat(result)
                .isEmpty();

        Mockito.verify(partnerStatusService, never()).getStatusResolvers(any());
    }

    @Test
    @DisplayName("Данные есть в ProgramArgs. Не идем в partner-status")
    void dataInProgramArgs() {
        ProgramArgs args = ProgramArgs.builder()
                .withRemoteResolvers(
                        Map.of(100L, Map.of(
                                ProgramStatusResolverType.REPLICATION_STATUS,
                                new PartnerStatusResolverResult(true, ProgramStatus.builder()
                                        .status(Status.ENABLING)))
                        )
                ).build();

        Optional<ProgramStatus.Builder> result = replicationStatusResolver.resolveWithMetrics(100L, args);
        Assertions.assertThat(result)
                .map(ProgramStatus.Builder::getStatus)
                .get()
                .isEqualTo(Status.ENABLING);

        Mockito.verify(partnerStatusService, never()).getStatusResolvers(any());
    }

    @Test
    @DisplayName("Идем за данными в partner-status, но он ничего не вернул")
    @DbUnitDataSet(before = "ReplicationStatusResolverTest/enabledResolver.before.csv")
    void emptyResult() {
        Mockito.when(partnerStatusService.getStatusResolvers(any()))
                .thenReturn(CompletableFuture.completedFuture(new StatusResolversResponse()));
        Optional<ProgramStatus.Builder> result = replicationStatusResolver.resolveWithMetrics(100L, ProgramArgs.DEFAULT);
        Assertions.assertThat(result)
                .isEmpty();

        Mockito.verify(partnerStatusService, times(1)).getStatusResolvers(any());
    }

    @Test
    @DisplayName("Идем за данными в partner-status, получаем результат")
    @DbUnitDataSet(before = "ReplicationStatusResolverTest/enabledResolver.before.csv")
    void resultFromPartnerStatus() {
        Mockito.when(partnerStatusService.getStatusResolvers(any()))
                .thenReturn(CompletableFuture.completedFuture(new StatusResolversResponse()
                                .addResolversItem(new StatusResolverResults()
                                        .resolver(StatusResolverType.REPLICATION_STATUS)
                                        .addResultsItem(new PartnerStatusInfo()
                                                .enabled(false)
                                                .partnerId(100L)
                                                .status(WizardStepStatus.ENABLING)
                                        )
                                )
                        )
                );

        Optional<ProgramStatus.Builder> result = replicationStatusResolver.resolveWithMetrics(100L, ProgramArgs.DEFAULT);
        Assertions.assertThat(result)
                .map(ProgramStatus.Builder::getStatus)
                .get()
                .isEqualTo(Status.ENABLING);

        Mockito.verify(partnerStatusService, times(1)).getStatusResolvers(any());
    }
}

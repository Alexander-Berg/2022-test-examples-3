package ru.yandex.market.core.program.partner.status;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.calculator.marketplace.ProgramStatusResolverType;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.mbi.partner.status.client.model.PartnerStatusInfo;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolverResults;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolverType;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolversRequest;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolversRequestItem;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolversResponse;
import ru.yandex.market.mbi.partner.status.client.model.WizardStepStatus;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;

/**
 * Тесты для {@link PartnerStatusMigrationService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class PartnerStatusMigrationServiceTest extends FunctionalTest {

    @Autowired
    private PartnerStatusMigrationService partnerStatusMigrationService;

    @Autowired
    private PartnerStatusService partnerStatusService;

    @AfterEach
    void checkMocks() {
        Mockito.verifyNoMoreInteractions(partnerStatusService);
    }

    @Test
    void disabled() {
        var result = partnerStatusMigrationService.requestResolverStatuses(List.of(100L, 200L)).join();
        Assertions.assertThat(result)
                .isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "PartnerStatusMigrationServiceTest/enabled.before.csv")
    void emptyResolversList() {
        var result = partnerStatusMigrationService.requestResolverStatuses(List.of(100L, 200L)).join();
        Assertions.assertThat(result)
                .isEmpty();
    }

    @Test
    @DbUnitDataSet(before = {
            "PartnerStatusMigrationServiceTest/enabled.before.csv",
            "PartnerStatusMigrationServiceTest/singleResolver.before.csv",
    })
    void singleResolver() {
        Mockito.when(partnerStatusService.getStatusResolvers(any()))
                .thenReturn(CompletableFuture.completedFuture(getSingleResponse()));

        var result = partnerStatusMigrationService.requestResolverStatuses(List.of(100L, 200L)).join();
        Assertions.assertThat(result.get(100L).get(ProgramStatusResolverType.NO_LOADED_OFFERS).getResult().get().getStatus())
                .isEqualTo(Status.FULL);
        Assertions.assertThat(result.get(200L).get(ProgramStatusResolverType.NO_LOADED_OFFERS).getResult().get().getStatus())
                .isEqualTo(Status.FAILED);

        Mockito.verify(partnerStatusService).getStatusResolvers(any());
    }

    @Test
    @DbUnitDataSet(before = {
            "PartnerStatusMigrationServiceTest/enabled.before.csv",
            "PartnerStatusMigrationServiceTest/singleResolver.before.csv",
    })
    void partnerStatusError() {
        Mockito.when(partnerStatusService.getStatusResolvers(any()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException()));

        var result = partnerStatusMigrationService.requestResolverStatuses(List.of(100L, 200L)).join();
        Assertions.assertThat(result.get(100L).get(ProgramStatusResolverType.NO_LOADED_OFFERS).isRequested())
                .isTrue();
        Assertions.assertThat(result.get(200L).get(ProgramStatusResolverType.NO_LOADED_OFFERS).isRequested())
                .isTrue();

        Mockito.verify(partnerStatusService).getStatusResolvers(any());
    }

    @Test
    @DbUnitDataSet(before = {
            "PartnerStatusMigrationServiceTest/whiteList.before.csv",
            "PartnerStatusMigrationServiceTest/singleResolver.before.csv",
    })
    void whiteList() {
        Mockito.when(partnerStatusService.getStatusResolvers(any()))
                .thenReturn(CompletableFuture.completedFuture(getSingleResponse()));

        partnerStatusMigrationService.requestResolverStatuses(List.of(100L, 200L)).join();

        var requestCaptor = ArgumentCaptor.forClass(StatusResolversRequest.class);
        Mockito.verify(partnerStatusService).getStatusResolvers(requestCaptor.capture());

        StatusResolversRequest actualRequest = requestCaptor.getValue();
        Assertions.assertThat(actualRequest.getResolvers())
                .hasSize(1)
                .first()
                .extracting(StatusResolversRequestItem::getNames, StatusResolversRequestItem::getPartnerIds)
                .containsExactly(List.of(StatusResolverType.NO_LOADED_OFFERS), List.of(100L));
    }

    @Test
    @DbUnitDataSet(before = "PartnerStatusMigrationServiceTest/whiteListAndEnabledAll.before.csv")
    void whiteListAndEnabledAll() {
        // NO_LOADED_OFFERS - в белом списке только 100
        // FBS_SORTING_CENTER - включен для всех
        Mockito.when(partnerStatusService.getStatusResolvers(any()))
                .thenReturn(CompletableFuture.completedFuture(getSingleResponse()));

        partnerStatusMigrationService.requestResolverStatuses(List.of(100L, 200L)).join();

        var requestCaptor = ArgumentCaptor.forClass(StatusResolversRequest.class);
        Mockito.verify(partnerStatusService).getStatusResolvers(requestCaptor.capture());

        StatusResolversRequest actualRequest = requestCaptor.getValue();
        Assertions.assertThat(actualRequest.getResolvers())
                .hasSize(2)
                .extracting(StatusResolversRequestItem::getNames, StatusResolversRequestItem::getPartnerIds)
                .containsExactlyInAnyOrder(
                        Tuple.tuple(List.of(StatusResolverType.FBS_SORTING_CENTER), List.of(100L, 200L)),
                        Tuple.tuple(List.of(StatusResolverType.NO_LOADED_OFFERS), List.of(100L))
                );
    }

    private StatusResolversResponse getSingleResponse() {
        return new StatusResolversResponse()
                .addResolversItem(new StatusResolverResults()
                        .resolver(StatusResolverType.NO_LOADED_OFFERS)
                        .addResultsItem(new PartnerStatusInfo()
                                .partnerId(100L)
                                .status(WizardStepStatus.FULL)
                                .enabled(true)
                                .newbie(false))
                )
                .addResolversItem(new StatusResolverResults()
                        .resolver(StatusResolverType.NO_LOADED_OFFERS)
                        .addResultsItem(new PartnerStatusInfo()
                                .partnerId(200L)
                                .status(WizardStepStatus.FAILED)
                                .enabled(false)
                                .newbie(false))
                );
    }
}

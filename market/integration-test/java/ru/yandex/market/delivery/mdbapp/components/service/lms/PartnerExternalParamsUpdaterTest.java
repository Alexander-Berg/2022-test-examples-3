package ru.yandex.market.delivery.mdbapp.components.service.lms;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderEventsFailoverCounter;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.PartnerExternalParams;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.PartnerExternalParamsType;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.OrderEventsFailoverRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PartnerExternalParamsRepository;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;

public class PartnerExternalParamsUpdaterTest extends MockContextualTest {

    @MockBean
    private LMSClient lmsClient;

    @Autowired
    private PartnerExternalParamsUpdater partnerExternalParamsUpdater;

    @Autowired
    private PartnerExternalParamsRepository partnerExternalParamsRepository;

    @Autowired
    private OrderEventsFailoverRepository orderEventsFailoverRepository;

    @Test
    @Sql(
        value = "/data/repository/partnerExternalParams/cleanup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
        value = "/data/repository/partnerExternalParams/partner_external_param.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    public void update() {
        List<PartnerExternalParamGroup> mockResult = List.of(
            new PartnerExternalParamGroup(
                2L,
                List.of(new PartnerExternalParam("GET_DELIVERY_DATE_ENABLED", "Описание", "1"))
            ),
            new PartnerExternalParamGroup(
                3L,
                List.of(
                    new PartnerExternalParam("UPDATE_ORDER_WITH_ONE_BOX_ENABLED", "Описание", "1"),
                    new PartnerExternalParam("AUTO_ITEM_REMOVING_ENABLED", "Описание", "1"),
                    new PartnerExternalParam("ITEM_REMOVING_ENABLED", "Описание", "1")
                )
            )
        );

        Mockito.when(lmsClient.getPartnerExternalParams(Mockito.anySet())).thenReturn(mockResult);

        partnerExternalParamsUpdater.update();

        List<PartnerExternalParams> result = partnerExternalParamsRepository.findAll();

        softly.assertThat(result)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrderElementsOf(
                List.of(
                    createPartnerExternalParams(1L, PartnerExternalParamsType.CREATE_ORDER_FREEZE_ENABLED, false),
                    createPartnerExternalParams(2L, PartnerExternalParamsType.GET_DELIVERY_DATE_ENABLED, true),
                    createPartnerExternalParams(2L, PartnerExternalParamsType.CREATE_ORDER_FREEZE_ENABLED, false),
                    createPartnerExternalParams(3L, PartnerExternalParamsType.UPDATE_ORDER_WITH_ONE_BOX_ENABLED, true),
                    createPartnerExternalParams(3L, PartnerExternalParamsType.AUTO_ITEM_REMOVING_ENABLED, true),
                    createPartnerExternalParams(3L, PartnerExternalParamsType.ITEM_REMOVING_ENABLED, true)
                )
            );
    }

    @Test
    @Sql(
        value = "/data/repository/partnerExternalParams/cleanup.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
        value = "/data/repository/partnerExternalParams/frozen_param.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    public void updateFreeze() {
        List<PartnerExternalParamGroup> mockResult = List.of(
            new PartnerExternalParamGroup(
                1L,
                List.of(new PartnerExternalParam("CREATE_ORDER_FREEZE_ENABLED", "Описание", "0"))
            ),
            new PartnerExternalParamGroup(
                2L,
                List.of(new PartnerExternalParam("CREATE_ORDER_FREEZE_ENABLED", "Описание", "1"))
            )
        );

        Mockito.when(lmsClient.getPartnerExternalParams(Mockito.anySet())).thenReturn(mockResult);

        partnerExternalParamsUpdater.update();

        List<PartnerExternalParams> result = partnerExternalParamsRepository.findAll();

        softly.assertThat(result)
            .usingElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrderElementsOf(
                List.of(
                    createPartnerExternalParams(1L, PartnerExternalParamsType.CREATE_ORDER_FREEZE_ENABLED, false),
                    createPartnerExternalParams(2L, PartnerExternalParamsType.CREATE_ORDER_FREEZE_ENABLED, true)
                )
            );

        List<OrderEventsFailoverCounter> counters = orderEventsFailoverRepository.findAll();

        Set<Long> eventsForRestart = Set.of(1L, 2L);

        softly.assertThat(
                counters.stream()
                    .filter(s -> eventsForRestart.contains(s.getEventId()))
                    .map(OrderEventsFailoverCounter::getAttemptCount)
                    .collect(Collectors.toList())
            )
            .containsOnly(5);

        softly.assertThat(
                counters.stream()
                    .filter(s -> !eventsForRestart.contains(s.getEventId()))
                    .map(OrderEventsFailoverCounter::getAttemptCount)
                    .collect(Collectors.toList())
            )
            .doesNotContain(5);
    }

    @Nonnull
    private PartnerExternalParams createPartnerExternalParams(
        Long partnerId,
        PartnerExternalParamsType paramType,
        Boolean active
    ) {
        return new PartnerExternalParams()
            .setPartnerId(partnerId)
            .setType(paramType)
            .setActive(active);
    }
}

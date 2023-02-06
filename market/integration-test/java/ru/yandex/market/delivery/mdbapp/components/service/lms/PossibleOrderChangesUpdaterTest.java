package ru.yandex.market.delivery.mdbapp.components.service.lms;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.PossibleOrderChange;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.PossibleOrderChangesType;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PossibleOrderChangeRepository;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.possibleOrderChanges.PossibleOrderChangeDto;
import ru.yandex.market.logistics.management.entity.response.possibleOrderChanges.PossibleOrderChangeGroup;

@Sql(
    value = "/data/repository/possibleOrderChanges/cleanup.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    value = "/data/repository/possibleOrderChanges/possible_order_changes.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public class PossibleOrderChangesUpdaterTest extends MockContextualTest {

    @MockBean
    private LMSClient lmsClient;

    @Autowired
    private PossibleOrderChangesUpdater possibleOrderChangesUpdater;

    @Autowired
    private PossibleOrderChangeRepository possibleOrderChangeRepository;

    @Test
    public void testUpdate() {
        List<PossibleOrderChangeGroup> mockResult = ImmutableList.of(
            new PossibleOrderChangeGroup(
                2L,
                ImmutableList.of(PossibleOrderChangeDto.builder()
                    .id(2L)
                    .partnerId(2L)
                    .type(ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType.ORDER_ITEMS)
                    .method(ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod.PARTNER_API)
                    .checkpointStatusFrom(null)
                    .checkpointStatusTo(120)
                    .enabled(true)
                    .build()
                )
            ),
            new PossibleOrderChangeGroup(
                3L,
                ImmutableList.of(
                    PossibleOrderChangeDto.builder()
                        .id(3L)
                        .partnerId(3L)
                        .type(ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType.ORDER_ITEMS)
                        .method(ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod.PARTNER_API)
                        .checkpointStatusFrom(10)
                        .checkpointStatusTo(100)
                        .enabled(false)
                        .build(),
                    PossibleOrderChangeDto.builder()
                        .id(3L)
                        .partnerId(3L)
                        .type(ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType.DELIVERY_DATES)
                        .method(ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod.PARTNER_API)
                        .checkpointStatusFrom(10)
                        .checkpointStatusTo(100)
                        .enabled(true)
                        .build()
                )
            )
        );

        Mockito.when(lmsClient.getPartnerPossibleOrderChanges(Mockito.anySet())).thenReturn(mockResult);

        possibleOrderChangesUpdater.update();

        List<PossibleOrderChange> result = possibleOrderChangeRepository.findAll();

        softly.assertThat(result)
            .extracting(PossibleOrderChange::getPartnerId)
            .containsExactlyInAnyOrder(1L, 2L, 3L, 3L);

        softly.assertThat(result)
            .extracting(PossibleOrderChange::getType)
            .containsExactlyInAnyOrder(
                PossibleOrderChangesType.ORDER_ITEMS,
                PossibleOrderChangesType.ORDER_ITEMS,
                PossibleOrderChangesType.ORDER_ITEMS,
                PossibleOrderChangesType.DELIVERY_DATES
            );

        softly.assertThat(result)
            .extracting(v -> Pair.of(v.getPartnerId(), v.getEnabled()))
            .containsExactlyInAnyOrder(
                Pair.of(1L, false),
                Pair.of(2L, true),
                Pair.of(3L, false),
                Pair.of(3L, true)
            );
    }
}

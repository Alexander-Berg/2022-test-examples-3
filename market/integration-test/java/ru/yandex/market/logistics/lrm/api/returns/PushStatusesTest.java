package ru.yandex.market.logistics.lrm.api.returns;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.client.model.LomReturnStatusHistory;
import ru.yandex.market.logistics.lrm.client.model.LomReturnStatuses;
import ru.yandex.market.logistics.lrm.client.model.LomSegmentStatus;
import ru.yandex.market.logistics.lrm.client.model.PushStatusesRequest;

import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.lrm.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Сохранение статусов от служб доставки")
@DatabaseSetup("/database/api/returns/push-statuses/before/prepare.xml")
@ParametersAreNonnullByDefault
class PushStatusesTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2021-01-01T12:00:00.00Z");

    @Test
    @DisplayName("Статус по одному сегменту одной коробки")
    @ExpectedDatabase(
        value = "/database/api/returns/push-statuses/after/single_box_single_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void singleBoxSingleSegment() {
        pushStatuses(List.of(getSingleLomReturnStatus(1L, 111L, NOW)));
    }

    @Test
    @DisplayName("Статусы по нескольким сегментам одной коробки")
    @ExpectedDatabase(
        value = "/database/api/returns/push-statuses/after/single_box_multiple_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void singleBoxMultipleSegments() {
        pushStatuses(
            List.of(
                getSingleLomReturnStatus(1L, 111L, NOW),
                getSingleLomReturnStatus(1L, 222L, NOW.plus(1, ChronoUnit.DAYS))
            )
        );
    }

    @Test
    @DisplayName("У коробки несколько сегментов одного партнера - статусы сохраняются для каждого")
    @DatabaseSetup(
        value = "/database/api/returns/push-statuses/before/add_segment_same_partner.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/api/returns/push-statuses/after/single_box_duplicated_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void singleBoxDuplicatedSegments() {
        pushStatuses(List.of(getSingleLomReturnStatus(1L, 111L, NOW)));
    }

    @Test
    @DisplayName("У возврата несколько коробок - статусы сохраняются для каждой")
    @DatabaseSetup(
        value = "/database/api/returns/push-statuses/before/add_second_box.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/api/returns/push-statuses/after/multiple_boxes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void multipleBoxes() {
        pushStatuses(List.of(getSingleLomReturnStatus(1L, 111L, NOW)));
    }

    @Test
    @DisplayName("Статусы по нескольким возвратам")
    @DatabaseSetup(
        value = "/database/api/returns/push-statuses/before/add_second_return.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/api/returns/push-statuses/after/multiple_returns.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void multipleReturns() {
        pushStatuses(
            List.of(
                getSingleLomReturnStatus(1L, 111L, NOW),
                getSingleLomReturnStatus(2L, 111L, NOW.plus(1, ChronoUnit.DAYS))
            )
        );
    }

    @Test
    @DisplayName("Несколько статусов по одному сегменту")
    @ExpectedDatabase(
        value = "/database/api/returns/push-statuses/after/multiple_statuses.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void multipleStatuses() {
        pushStatuses(List.of(getMultipleLomReturnStatus(1L, 111L, NOW)));
    }

    @Test
    @DisplayName("Несколько записей с одинаковым статусом, но разным временем - сохраняем все")
    @ExpectedDatabase(
        value = "/database/api/returns/push-statuses/after/duplicated_statuses.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void duplicatedStatuses() {
        pushStatuses(List.of(getDuplicatedLomReturnStatus(1L, 111L, NOW)));
    }

    private void pushStatuses(List<LomReturnStatuses> statuses) {
        PushStatusesRequest pushStatusesRequest = new PushStatusesRequest();
        pushStatusesRequest.setStatuses(statuses);
        apiClient.returns().pushCancellationReturnDeliveryServiceStatuses().body(pushStatusesRequest)
            .execute(validatedWith(shouldBeCode(SC_OK)));
    }

    @Nonnull
    private LomReturnStatuses getSingleLomReturnStatus(long returnId, long partnerId, Instant timestamp) {
        return getLomReturnStatus(
            returnId,
            partnerId,
            List.of(
                getLomReturnStatusHistory(
                    LomSegmentStatus.RETURN_PREPARING,
                    timestamp
                )
            )
        );
    }

    @Nonnull
    private LomReturnStatuses getMultipleLomReturnStatus(long returnId, long partnerId, Instant timestamp) {
        return getLomReturnStatus(
            returnId,
            partnerId,
            List.of(
                getLomReturnStatusHistory(
                    LomSegmentStatus.RETURN_PREPARING,
                    timestamp
                ),
                getLomReturnStatusHistory(
                    LomSegmentStatus.RETURN_ARRIVED,
                    timestamp.plus(1, ChronoUnit.DAYS)
                ),
                getLomReturnStatusHistory(
                    LomSegmentStatus.RETURNED,
                    timestamp.plus(2, ChronoUnit.DAYS)
                )
            )
        );
    }

    @Nonnull
    private LomReturnStatuses getDuplicatedLomReturnStatus(long returnId, long partnerId, Instant timestamp) {
        return getLomReturnStatus(
            returnId,
            partnerId,
            List.of(
                getLomReturnStatusHistory(
                    LomSegmentStatus.RETURN_PREPARING,
                    timestamp
                ),
                getLomReturnStatusHistory(
                    LomSegmentStatus.RETURN_PREPARING,
                    timestamp.plus(1, ChronoUnit.DAYS)
                )
            )
        );
    }

    @Nonnull
    private LomReturnStatuses getLomReturnStatus(
        long returnId,
        long partnerId,
        List<LomReturnStatusHistory> statusHistory
    ) {
        LomReturnStatuses lomReturnStatuses = new LomReturnStatuses();
        lomReturnStatuses.setReturnId(returnId);
        lomReturnStatuses.setPartnerId(partnerId);
        lomReturnStatuses.setStatusHistory(statusHistory);
        return lomReturnStatuses;
    }

    @Nonnull
    private LomReturnStatusHistory getLomReturnStatusHistory(LomSegmentStatus status, Instant timestamp) {
        LomReturnStatusHistory lomReturnStatusHistory = new LomReturnStatusHistory();
        lomReturnStatusHistory.setStatus(status);
        lomReturnStatusHistory.setTimestamp(timestamp);
        return lomReturnStatusHistory;
    }
}

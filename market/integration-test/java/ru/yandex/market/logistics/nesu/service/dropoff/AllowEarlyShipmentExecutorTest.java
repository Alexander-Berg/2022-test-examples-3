package ru.yandex.market.logistics.nesu.service.dropoff;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.abo.api.entity.rating.exclusion.ByDateRangeExclusions;
import ru.yandex.market.abo.api.entity.rating.exclusion.PublicRatingExclusionType;
import ru.yandex.market.abo.api.entity.rating.exclusion.UploadRatingExclusionRequest;
import ru.yandex.market.abo.api.entity.rating.operational.RatingPartnerType;
import ru.yandex.market.logistics.nesu.utils.CommonsConstants;

import static org.mockito.Mockito.verify;

@DatabaseSetup("/service/dropoff/before/allow_early_shipment_subtask.xml")
@DisplayName("Тест на выполнение подзадачи отключения дропоффа. Разрешить преждевременную отгрузку.")
class AllowEarlyShipmentExecutorTest extends AbstractDisablingSubtaskTest {

    @Test
    @ExpectedDatabase(
        value = "/service/dropoff/after/allow_early_shipment_subtask.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное выполнение подзадачи: разрешить преждевременную отгрузку.")
    void successAllowEarlyShipment() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));

        ArgumentCaptor<UploadRatingExclusionRequest> argumentCaptor = ArgumentCaptor.forClass(
            UploadRatingExclusionRequest.class
        );
        verify(aboClient).uploadRatingExclusion(argumentCaptor.capture());
        softly.assertThat(argumentCaptor.getValue()).usingRecursiveComparison().isEqualTo(
            new UploadRatingExclusionRequest(
                RatingPartnerType.DROPSHIP,
                PublicRatingExclusionType.BY_ESTIMATED_SHIPMENT,
                new ByDateRangeExclusions(
                    Set.of(11L, 22L),
                    LocalDate.ofInstant(Instant.now(clock), CommonsConstants.MSK_TIME_ZONE),
                    LocalDate.ofInstant(Instant.now(clock).plus(10, ChronoUnit.DAYS), CommonsConstants.MSK_TIME_ZONE)
                )
            ));
    }

    @Test
    @DatabaseSetup(
        value = "/service/dropoff/before/allow_early_shipment_subtask_no_affected_shops.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/service/dropoff/after/allow_early_shipment_subtask_no_affected_shops.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное выполнение подзадачи: разрешить преждевременную отгрузку, нет заафекченных партнеров.")
    void successAllowEarlyShipmentNoAffectedPartners() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));
    }
}

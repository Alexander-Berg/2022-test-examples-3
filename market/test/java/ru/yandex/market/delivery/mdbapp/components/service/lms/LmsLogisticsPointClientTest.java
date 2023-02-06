package ru.yandex.market.delivery.mdbapp.components.service.lms;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.PARTNER_ID;

@RunWith(MockitoJUnitRunner.class)
public class LmsLogisticsPointClientTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Captor
    private ArgumentCaptor<LogisticsPointFilter> logisticsPointFilterArgumentCaptor;

    @Mock
    private LMSClient lmsClient;

    @InjectMocks
    private LmsLogisticsPointClient lmsLogisticsPointClient;

    @Test
    public void getWarehousesByPartnerId() {
        lmsLogisticsPointClient.getWarehousesByPartnerId(PARTNER_ID);

        Mockito.verify(lmsClient).getLogisticsPoints(logisticsPointFilterArgumentCaptor.capture());

        LogisticsPointFilter actualLogisticsPointFilter = logisticsPointFilterArgumentCaptor.getValue();

        softly.assertThat(actualLogisticsPointFilter.getType())
            .isEqualTo(PointType.WAREHOUSE);
        softly.assertThat(actualLogisticsPointFilter.getPartnerIds())
            .isEqualTo(ImmutableSet.of(PARTNER_ID));
    }

    @Test
    public void getActivePickupPointBetweenTwoInResponse() {
        when(lmsClient.getLogisticsPoints(any(LogisticsPointFilter.class)))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder()
                    .active(false)
                    .build(),
                LogisticsPointResponse.newBuilder()
                    .active(true)
                    .build()
            ));

        softly.assertThat(lmsLogisticsPointClient.getPickupPoint(1L, "pickup-1"))
            .as("Active pickup point was chosen")
            .extracting(LogisticsPointResponse::isActive)
            .isEqualTo(true);
    }

    @Test
    public void getPartner_shouldReturnPartner() {
        // given:
        doReturn(Optional.of(partnerResponse()))
            .when(lmsClient)
            .getPartner(PARTNER_ID);

        // when:
        final Optional<PartnerResponse> expected = lmsLogisticsPointClient.getPartner(PARTNER_ID);

        // then:
        softly.assertThat(expected).isEqualTo(Optional.of(partnerResponse()));
    }

    @Test
    public void getPartner_shouldReturnEmptyWhenEmptyResponse() {
        // given:
        doReturn(Optional.empty())
            .when(lmsClient)
            .getPartner(PARTNER_ID);

        // when:
        final Optional<PartnerResponse> expected = lmsLogisticsPointClient.getPartner(PARTNER_ID);

        // then:
        softly.assertThat(expected).isEqualTo(Optional.empty());
    }

    @Test
    public void getPartner_shouldReturnEmptyWhenExceptionThrown() {
        // given:
        doThrow(new RuntimeException())
            .when(lmsClient)
            .getPartner(PARTNER_ID);

        // when:
        final Optional<PartnerResponse> expected = lmsLogisticsPointClient.getPartner(PARTNER_ID);

        // then:
        softly.assertThat(expected).isEqualTo(Optional.empty());
    }

    private PartnerResponse partnerResponse() {
        return PartnerResponse.newBuilder()
            .id(PARTNER_ID)
            .build();
    }
}

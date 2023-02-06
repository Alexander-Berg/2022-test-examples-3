package ru.yandex.market.mbi.api.controller;

import java.time.LocalDate;
import java.time.ZoneId;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.AgencyCommissionRequest;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DbUnitDataSet(
        before = "AgencyCommissionControllerTest.before.csv"
)
public class AgencyCommissionControllerTest extends FunctionalTest {

    private static final Long UID = 1111L;
    private static final Long AGENCY_ID = 676L;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setUpConfiguration() {
        clock.setFixed(
                LocalDate.of(2022, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
    }

    @Test
    void testUnknownAgency() {
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().setAgencyCommission(
                        1L,
                        new AgencyCommissionRequest()
                                .partnerId(6L)
                                .onboardingRewardType(AgencyCommissionRequest.OnboardingRewardTypeEnum.FULL),
                        UID
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    void testUnknownPartner() {
        var exception = assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().setAgencyCommission(
                        AGENCY_ID,
                        new AgencyCommissionRequest()
                                .partnerId(1112233L)
                                .onboardingRewardType(AgencyCommissionRequest.OnboardingRewardTypeEnum.FULL),
                        UID
                )
        );
        assertThat(exception.getHttpErrorCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    @DbUnitDataSet(after = "AgencyCommissionControllerTest.after.csv")
    void testSuccessfulCommissionSet() {
        getMbiOpenApiClient().setAgencyCommission(
                AGENCY_ID,
                new AgencyCommissionRequest()
                        .partnerId(6L)
                        .onboardingRewardType(AgencyCommissionRequest.OnboardingRewardTypeEnum.FULL),
                UID
        );
    }
}

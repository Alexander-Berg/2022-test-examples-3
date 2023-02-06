package ru.yandex.market.mbi.partner.registration.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.BusinessNameCheckResponse;
import ru.yandex.market.mbi.open.api.client.model.BusinessRegistrationResponse;
import ru.yandex.market.mbi.partner.registration.AbstractFunctionalTest;
import ru.yandex.market.mbi.partner.registration.util.MbiOpenApiConversion;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.BusinessNameCheck;
import ru.yandex.mj.generated.client.mbi_partner_registration.model.BusinessRegistrationRequest;

/**
 * Функциональный тест для {@link ru.yandex.market.mbi.partner.registration.api.BusinessRegistrationApiService}
 */
class BusinessRegistrationApiServiceTest extends AbstractFunctionalTest {

    private static final long UID = 1001;

    @Autowired
    private MbiOpenApiClient mbiOpenApiClient;

    @Test
    void testBusinessRegistration() {
        Mockito.when(mbiOpenApiClient.registerBusiness(Mockito.eq(UID), Mockito.any()))
                .thenReturn(new BusinessRegistrationResponse().businessId(10L));

        var response = businessRegistrationApiClient.registerBusiness(
                UID,
                new BusinessRegistrationRequest().businessName("name")
        )
                .schedule()
                .join();
        Assertions.assertEquals(10L, response.getResult().getBusinessId());
        Mockito.verify(mbiOpenApiClient).registerBusiness(
                Mockito.eq(UID),
                Mockito.argThat(arg -> arg.getBusinessName().equals("name"))
        );
    }

    @Test
    void testBusinessNameCheck() {
        Mockito.when(mbiOpenApiClient.checkBusinessName(Mockito.eq("name")))
                .thenReturn(new BusinessNameCheckResponse().checkResult(BusinessNameCheckResponse.CheckResultEnum.OK));
        var response = businessRegistrationApiClient.checkBusinessName("name")
                .schedule()
                .join();
        Assertions.assertEquals(BusinessNameCheck.CheckResultEnum.OK, response.getResult().getCheckResult());
    }

    @Test
    void testCodeConversion() {
        for (BusinessNameCheckResponse.CheckResultEnum check : BusinessNameCheckResponse.CheckResultEnum.values()) {
            Assertions.assertDoesNotThrow(() -> MbiOpenApiConversion.checkResultFromMbiApi(check));
        }
    }
}

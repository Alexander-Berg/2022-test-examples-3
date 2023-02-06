package ru.yandex.market.mbi.partner.status.partner;

import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.mbi.partner.status.AbstractFunctionalTest;
import ru.yandex.mj.generated.client.mbi_partner_status.api.PartnerStatusApiClient;
import ru.yandex.mj.generated.client.mbi_partner_status.model.BusinessPartnersRequest;

public class PartnerStatusApiControllerTest extends AbstractFunctionalTest {
    @Autowired
    private PartnerStatusApiClient client;

    @Test
    void test() {
        Exception e = Assertions.assertThrows(
                CompletionException.class,
                () -> client.businessBusinessIdProgramsPost(1L, new BusinessPartnersRequest())
                        .schedule().join()
        );
        CommonRetrofitHttpExecutionException cause = (CommonRetrofitHttpExecutionException) e.getCause();
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), cause.getHttpCode());
    }
}

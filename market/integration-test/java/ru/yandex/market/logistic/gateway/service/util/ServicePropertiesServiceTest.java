package ru.yandex.market.logistic.gateway.service.util;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.common.ApiType;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.exceptions.InvalidTokenException;

public class ServicePropertiesServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ServicePropertiesService servicePropertiesService;

    @Test
    @DatabaseSetup("classpath:repository/state/properties.xml")
    public void testGetPartnersByTokenAndTypeSuccess() {
        List<Partner> partners = servicePropertiesService.getPartnersByTokenAndApiType("ABC12345", ApiType.DELIVERY);

        softAssert.assertThat(partners)
            .as("Asserting the partners list is valid")
            .hasSameElementsAs(Collections.singletonList(new Partner(3L)));
    }

    @Test(expected = InvalidTokenException.class)
    @DatabaseSetup("classpath:repository/state/properties.xml")
    public void testGetPartnersByTokenAndTypeNotFound() {
        String token = "xxxxxxxxxxxxxxxxxxxxxxmarschrouteT.T.Tokenxxxxxxxxxxxxxxxxxxxxxxxxxx";
        servicePropertiesService.getPartnersByTokenAndApiType(token, ApiType.DELIVERY);
    }
}

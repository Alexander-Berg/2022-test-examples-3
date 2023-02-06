package ru.yandex.market.logistic.gateway.service.util;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.common.PartnerMethod;
import ru.yandex.market.logistic.api.model.properties.PartnerProperties;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.exceptions.PropertiesException;

public class PropertiesServiceTest extends AbstractIntegrationTest {

    @Autowired
    private PropertiesService propertiesService;

    @Test
    @DatabaseSetup("classpath:repository/state/properties.xml")
    @ExpectedDatabase(value = "classpath:repository/expected/properties_after_synchronization.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testGetApiPartnerPropertiesExisting() {
        PartnerProperties properties = propertiesService.getApiPartnerProperties(new Partner(1L),
            PartnerMethod.CREATE_ORDER_FF);

        softAssert.assertThat(properties.getToken())
            .as("Asserting the token is valid")
            .isEqualTo("ABC12345");
        softAssert.assertThat(properties.getUrl())
            .as("Asserting the url is valid")
            .isEqualTo("https://url1.url/api/createOrder");
    }

    @Test
    @DatabaseSetup("classpath:repository/state/properties_with_defined_api_type.xml")
    @ExpectedDatabase(value = "classpath:repository/expected/properties_with_api_type_after_synchronization.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testGetApiPartnerPropertiesWithDefinedApiTypeExisting() {
        PartnerProperties properties = propertiesService.getApiPartnerProperties(new Partner(1L),
            PartnerMethod.CREATE_ORDER_FF);

        softAssert.assertThat(properties.getToken())
            .as("Asserting the token is valid")
            .isEqualTo("ABC12345");
        softAssert.assertThat(properties.getUrl())
            .as("Asserting the url is valid")
            .isEqualTo("https://url1.url/api/createOrder");
    }

    @Test(expected = PropertiesException.class)
    @DatabaseSetup("classpath:repository/state/properties.xml")
    public void testGetApiPartnerPropertiesNotExisting() throws Exception {
        propertiesService.getApiPartnerProperties(new Partner(4L),PartnerMethod.CREATE_ORDER_FF);
    }
}

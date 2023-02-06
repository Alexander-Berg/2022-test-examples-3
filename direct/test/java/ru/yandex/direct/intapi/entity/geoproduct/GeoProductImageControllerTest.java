package ru.yandex.direct.intapi.entity.geoproduct;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.image.container.BannerImageType;
import ru.yandex.direct.core.testing.data.TestClients;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.image.service.BannerImageService;
import ru.yandex.direct.intapi.validation.model.IntapiResponse;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;

import static org.mockito.Mockito.mock;
import static ru.yandex.direct.intapi.entity.geoproduct.service.GeoProductTransferMoneyValidationService.DEFAULT_GEOPRODUCT_INTAPI_MONEY_TRANSFER_OPERATORS;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class GeoProductImageControllerTest {
    public static final Long ALLOWED_OPERATOR_UID =
            DEFAULT_GEOPRODUCT_INTAPI_MONEY_TRANSFER_OPERATORS.iterator().next();

    private GeoProductImageController geoProductImageController;

    @Autowired
    private Steps steps;

    @Autowired
    private RbacService rbacService;

    @Before
    public void before() {
        BannerImageService bannerImageService = mock(BannerImageService.class);
        geoProductImageController = new GeoProductImageController(bannerImageService, rbacService);
    }

    @Test(expected = IllegalStateException.class)
    public void operatorIdNotAllowed() {
        geoProductImageController.uploadImageByUrlForImageAd("https://ya.ru", "banner_text", 0L, 1L);
    }

    @Test(expected = IllegalStateException.class)
    public void clientIdNotAllowed() {
        geoProductImageController.uploadImageByUrlForImageAd("https://ya.ru", "banner_text", 0L,
                DEFAULT_GEOPRODUCT_INTAPI_MONEY_TRANSFER_OPERATORS.iterator().next());
    }

    @Test
    public void operatorAndClientIsAllowedSoExceptionIsNotExpected() {
        ClientInfo allowedAgencyClientInfo = steps.clientSteps().createClient(new ClientInfo().withClient(
                TestClients.defaultClient(ALLOWED_OPERATOR_UID, RbacRole.AGENCY)));
        ClientInfo clientUnderAllowedAgency = steps.clientSteps().createClientUnderAgency(allowedAgencyClientInfo);

        long allowedClientId = clientUnderAllowedAgency.getClientId().asLong();

        String imageType = BannerImageType.BANNER_TEXT.name().toLowerCase();

        IntapiResponse response =
                geoProductImageController.uploadImageByUrlForImageAd("https://ya.ru", imageType,
                        allowedClientId, ALLOWED_OPERATOR_UID);
    }

}

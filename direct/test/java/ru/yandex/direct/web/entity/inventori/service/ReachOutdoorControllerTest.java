package ru.yandex.direct.web.entity.inventori.service;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.asynchttp.AsyncHttpExecuteException;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.web.core.entity.inventori.model.ReachOutdoorResult;
import ru.yandex.direct.web.core.entity.inventori.service.InventoriWebValidationService;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.inventori.controller.InventoriController;
import ru.yandex.direct.web.entity.inventori.model.ReachOutdoorResponse;
import ru.yandex.direct.web.validation.kernel.ValidationResultConversionService;
import ru.yandex.direct.web.validation.model.ValidationResponse;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.entity.inventori.service.InventoriTestWebDefects.webCannotBeNull;

@Ignore //todo выключил в рамках рефакторинга, переделать или удалить в рамках DIRECT-104384
public class ReachOutdoorControllerTest extends ReachOutdoorBaseTest {

    @Autowired
    private TranslationService translationService;

    @Autowired
    private InventoriWebValidationService inventoriValidationService;

    @Autowired
    private ValidationResultConversionService validationResultConversionService;

    @Autowired
    private CampaignForecastValidationService campaignForecastValidationService;

    @Autowired
    private CampaignForecastService campaignForecastService;

    @Autowired
    private ClientService clientService;

    private InventoriController controller;

    @Before
    public void before() {
        super.before();

        controller = new InventoriController(
                translationService,
                inventoriWebService,
                inventoriValidationService,
                validationResultConversionService,
                authenticationSource,
                campaignForecastValidationService,
                campaignForecastService, clientService);
    }

    @Test
    public void getReachOutdoor_Success() {
        inventoriSuccessResponse();

        ResponseEntity<WebResponse> response = controller.getReachOutdoor(defaultRequest(), user.getLogin());
        assumeThat(response.getStatusCodeValue(), is(HttpStatus.OK.value()));

        ReachOutdoorResult expectedResult = (ReachOutdoorResult) new ReachOutdoorResult()
                .withReach(1000L)
                .withOtsCapacity(2000L);
        assertSuccessResult(response, expectedResult);
    }

    @Test
    public void getReachOutdoor_ValidationError() {
        ResponseEntity<WebResponse> response = controller.getReachOutdoor(null, user.getLogin());

        assertThat(response.getStatusCodeValue(), is(HttpStatus.BAD_REQUEST.value()));

        WebValidationResult vr = ((ValidationResponse) response.getBody()).validationResult();
        WebValidationResult expectedResult =
                new WebValidationResult().addErrors(webCannotBeNull(""));

        Assertions.assertThat(vr).isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test(expected = IllegalStateException.class)
    public void getReachOutdoor_InventoriBadResponse() {
        inventoriBadResponse();
        controller.getReachOutdoor(defaultRequest(), user.getLogin());
    }

    @Test(expected = AsyncHttpExecuteException.class)
    public void getReachOutdoor_InventoriExceptionResponse() {
        inventoriExceptionResponse();
        controller.getReachOutdoor(defaultRequest(), user.getLogin());
    }

    private void assertSuccessResult(ResponseEntity<WebResponse> actualResponse, ReachOutdoorResult expectedResult) {
        CompareStrategy responseCompareStrategy =
                DefaultCompareStrategies.allFields().forFields(newPath("result"))
                        .useMatcher(beanDiffer(expectedResult).useCompareStrategy(REACH_OUTDOOR_RESULT_STRATEGY));
        assertThat((ReachOutdoorResponse) actualResponse.getBody(),
                beanDiffer(new ReachOutdoorResponse(expectedResult)).useCompareStrategy(responseCompareStrategy));
    }
}

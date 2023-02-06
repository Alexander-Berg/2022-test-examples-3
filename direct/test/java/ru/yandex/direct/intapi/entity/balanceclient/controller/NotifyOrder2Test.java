package ru.yandex.direct.intapi.entity.balanceclient.controller;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;
import ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderService;
import ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderTestHelper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.YANDEX_AGENCY_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.controller.BalanceClientController.INVALID_REQUEST_SIZE;

/**
 * Тесты на логику метода notifyOrder из BalanceClientController
 *
 * @see BalanceClientController
 */
public class NotifyOrder2Test {

    private static NotifyOrderParameters notifyOrderParameters;

    @Mock
    private NotifyOrderService notifyOrderService;

    private BalanceClientController controller;

    @BeforeClass
    public static void initTestData() {
        notifyOrderParameters = NotifyOrderTestHelper.generateNotifyOrderParameters();
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        controller = new BalanceClientController(notifyOrderService, YANDEX_AGENCY_SERVICE_ID);
    }


    @Test
    public void checkReturnInvalidRequestSize_WhenRequestEmpty() {
        BalanceClientResponse balanceClientResponse = controller.notifyOrder(Collections.emptyList());
        assertThat(balanceClientResponse, equalTo(INVALID_REQUEST_SIZE));
    }

    @Test
    public void checkReturnInvalidRequestSize_WhenRequestListSizeGreaterOne() {
        BalanceClientResponse balanceClientResponse =
                controller.notifyOrder(Arrays.asList(notifyOrderParameters, notifyOrderParameters));
        assertThat(balanceClientResponse, equalTo(INVALID_REQUEST_SIZE));
    }

    @Test
    public void checkReturnNotifyOrderServiceResponse() {
        BalanceClientResponse notifyOrderServiceResponse = BalanceClientResponse.success("alright");
        doReturn(notifyOrderServiceResponse).when(notifyOrderService)
                .notifyOrder(notifyOrderParameters);

        BalanceClientResponse balanceClientResponse = controller.notifyOrder(Arrays.asList(notifyOrderParameters));
        assertThat(balanceClientResponse, beanDiffer(notifyOrderServiceResponse));
    }
}

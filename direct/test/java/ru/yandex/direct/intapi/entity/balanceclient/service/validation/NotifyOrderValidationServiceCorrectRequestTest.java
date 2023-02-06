package ru.yandex.direct.intapi.entity.balanceclient.service.validation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BANANA_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BAYAN_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.DIRECT_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyOrderTestHelper.generateNotifyOrderParameters;

@RunWith(Parameterized.class)
public class NotifyOrderValidationServiceCorrectRequestTest {

    private NotifyOrderValidationService notifyOrderValidationService;
    private NotifyOrderParameters notifyOrderParameters;

    @Parameterized.Parameter()
    public String testDescription;

    @Parameterized.Parameter(1)
    public Consumer<NotifyOrderParameters> parametersConsumer;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {"Проверка всех параметров - без изменений", (Consumer<NotifyOrderParameters>) p -> {
                    //nothing
                }
                },

                //Проверка sumUnits
                {"sumUnits = 0", (Consumer<NotifyOrderParameters>) p -> p.withSumUnits(BigDecimal.ZERO)},

                //Проверка chipsCost
                {"chipsCost = null", (Consumer<NotifyOrderParameters>) p -> p.withChipsCost(null)},
                {"chipsCost = 0", (Consumer<NotifyOrderParameters>) p -> p.withChipsCost(BigDecimal.ZERO)},

                //Проверка sumRealMoney
                {"sumRealMoney = null", (Consumer<NotifyOrderParameters>) p -> p.withSumRealMoney(null)},
                {"sumRealMoney = 0", (Consumer<NotifyOrderParameters>) p -> p.withSumRealMoney(BigDecimal.ZERO)},

                //Проверка chipsSpent
                {"chipsSpent = null", (Consumer<NotifyOrderParameters>) p -> p.withChipsSpent(null)},
                {"chipsSpent = 0", (Consumer<NotifyOrderParameters>) p -> p.withChipsSpent(BigDecimal.ZERO)},
        });
    }

    @Before
    public void before() {
        notifyOrderValidationService = new NotifyOrderValidationService(null,
                DIRECT_SERVICE_ID, BAYAN_SERVICE_ID, BANANA_SERVICE_ID);

        notifyOrderParameters = generateNotifyOrderParameters()
                .withServiceId(BAYAN_SERVICE_ID);
    }


    @Test
    public void checkValidateRequest() {
        parametersConsumer.accept(notifyOrderParameters);
        BalanceClientResponse response = notifyOrderValidationService.validateRequest(notifyOrderParameters);
        assertThat("получили null в ответе", response, nullValue());
    }
}

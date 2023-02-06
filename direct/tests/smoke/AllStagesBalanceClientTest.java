package ru.yandex.autotests.directintapi.tests.smoke;

import java.net.URI;
import java.util.Collection;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.balanceproxy.EnvironmentsMongoHelper;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyClient2JSONRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.balanceclient.NotifyOrder2JSONRequest;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.hazelcast.SemaphoreRule;
import ru.yandex.terra.junit.rules.BottleMessageRule;

/**
 * Created by buhter on 05.08.17.
 */
@Aqua.Test
@RunWith(Parameterized.class)
@Features(FeatureNames.BALANCE_INTAPI_MONITOR)
public class AllStagesBalanceClientTest {
    private static final EnvironmentsMongoHelper mongoHelper = new EnvironmentsMongoHelper();

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Rule
    public BottleMessageRule bmr = new BottleMessageRule();

    @Parameterized.Parameter
    public String host;

    public DarkSideSteps darkSideSteps;

    @Parameterized.Parameters(name = "host: {0}")
    public static Collection<Object[]> data() {
        return mongoHelper.getAllEnvironments()
                .stream()
                .map(environmentBean -> new Object[]{"http://" + URI.create(environmentBean.getUrl()).getHost()})
                .collect(Collectors.toList());
    }

    @Before
    public void init() {
        darkSideSteps = new DarkSideSteps(host);
    }

    @Stories("NotifyOrder2. Доступность сред.")
    @Test
    public void notifyOrder2Test() {
        Long campaignId = 1L;
        Money qty = Money.valueOf(100.0f);
        darkSideSteps.getBalanceClientNotifyOrderJsonSteps()
                .notifyOrderExpectErrors(new NotifyOrder2JSONRequest()
                                .withServiceId(NotifyOrder2JSONRequest.DIRECT_SERVICE_ID)
                                .withServiceOrderId(campaignId)
                                .withTimestamp()
                                .withConsumeQty(qty.floatValue())
                                .withProductCurrency(Currency.RUB.value()),
                        400, 809, String.format("Campaign %s does not exists", campaignId)
                );
    }

    @Stories("NotifyClient2. Доступность сред.")
    @Test
    public void notifyClientTest() {
        Long clientId = 1L;
        Money overdraftLimit = Money.valueOf(100.0f);
        Money overdraftSpent = Money.valueOf(150.0f);
        darkSideSteps.getBalanceClientNotifyClientJsonSteps().notifyClientExpectErrors(
                new NotifyClient2JSONRequest()
                        .withClientID(clientId)
                        .withTimestamp()
                        .withOverdraftLimit(overdraftLimit.floatValue())
                        .withOverdraftSpent(overdraftSpent.floatValue()),
                200, 0, String.format("ClientID %s is not known", clientId)
        );
    }
}
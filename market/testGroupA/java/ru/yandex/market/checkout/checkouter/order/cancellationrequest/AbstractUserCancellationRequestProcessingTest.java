package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.CUSTOM;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.REPLACING_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_BOUGHT_CHEAPER;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_FORGOT_TO_USE_BONUS;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_REFUSED_DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_WANTED_ANOTHER_PAYMENT_METHOD;

public class AbstractUserCancellationRequestProcessingTest extends AbstractWebTestBase {

    protected static final String NOTES = "notes";
    private static final ClientInfo USER_CLIENT_INFO = new ClientInfo(ClientRole.USER, BuyerProvider.UID);
    @Autowired
    protected YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    protected CancellationRequestHelper cancellationRequestHelper;
    @Autowired
    protected OrderGetHelper orderGetHelper;

    public static Stream<Arguments> parameterizedTestData() {
        List<Object[]> result = new ArrayList<>();
        for (Boolean isCreateByOrderEditApi : new Boolean[]{Boolean.FALSE, Boolean.TRUE}) {
            result.addAll(Stream.of(
                    USER_CHANGED_MIND,
                    USER_REFUSED_DELIVERY,
                    USER_BOUGHT_CHEAPER,
                    USER_WANTED_ANOTHER_PAYMENT_METHOD,
                    USER_FORGOT_TO_USE_BONUS,
                    REPLACING_ORDER,
                    CUSTOM
            ).map(substatus -> new Object[]{USER_CLIENT_INFO, substatus, isCreateByOrderEditApi})
                    .collect(Collectors.toList()));
        }
        return result.stream().map(Arguments::of);
    }

}

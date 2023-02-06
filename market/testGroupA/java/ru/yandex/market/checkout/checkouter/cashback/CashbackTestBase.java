package ru.yandex.market.checkout.checkouter.cashback;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.balance.service.TrustService;
import ru.yandex.market.checkout.checkouter.service.loyalty.CashbackProfilesService;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static ru.yandex.market.checkout.providers.CashbackTestProvider.singleItemCashbackResponse;

public class CashbackTestBase extends AbstractWebTestBase {

    @Autowired
    protected CashbackProfilesService cashbackProfilesService;
    @Autowired
    protected TrustService trustService;

    protected Parameters singleItemWithCashbackParams;

    @BeforeEach
    void setUp() throws IOException {
        final Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        trustMockConfigurer.mockListWalletBalanceResponse();
        singleItemWithCashbackParams = parameters;
        singleItemWithCashbackParams.setCheckCartErrors(false);
        singleItemWithCashbackParams.setupPromo("PROMO");
        singleItemWithCashbackParams.getLoyaltyParameters()
                .setExpectedCashbackOptionsResponse(singleItemCashbackResponse());
    }
}

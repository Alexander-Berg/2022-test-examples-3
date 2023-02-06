package ru.yandex.market.checkout.wiremock;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Helper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Extension;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;

import ru.yandex.market.checkout.util.loyalty.CalcV3ResponseTransformer;
import ru.yandex.market.checkout.util.loyalty.ReCalcResponseTranformer;
import ru.yandex.market.checkout.util.loyalty.ReSpendResponseTransformer;
import ru.yandex.market.checkout.util.loyalty.SpendV3ResponseTransformer;

public final class DynamicWiremockFactoryBean {

    private static final Map<String, Helper> HELPERS = new HashMap<>();

    static {
        HELPERS.put(ContainsIdTemplateHelper.NAME, new ContainsIdTemplateHelper());
        HELPERS.put(OrHelper.NAME, new OrHelper());
    }

    private DynamicWiremockFactoryBean() {
    }

    public static WireMockServer create(ObjectMapper marketLoyaltyObjectMapper) {
        return create(new ResponseTemplateTransformer(false, HELPERS),
                new SpendV3ResponseTransformer(marketLoyaltyObjectMapper),
                new CalcV3ResponseTransformer(marketLoyaltyObjectMapper),
                new ReCalcResponseTranformer(marketLoyaltyObjectMapper),
                new ReSpendResponseTransformer(marketLoyaltyObjectMapper)
        );
    }

    public static WireMockServer create() {
        return create(new ResponseTemplateTransformer(false, HELPERS));
    }

    public static WireMockServer create(Extension... extensions) {
        WireMockConfiguration wireMockConfiguration = new WireMockConfiguration()
                .dynamicPort()
                .maxRequestJournalEntries(90)
                .notifier(new Slf4jNotifier(false));

        return new WireMockServer(wireMockConfiguration.extensions(extensions));
    }
}

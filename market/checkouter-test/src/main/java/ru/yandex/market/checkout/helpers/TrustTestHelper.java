package ru.yandex.market.checkout.helpers;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.Matcher;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.common.TestHelper;
import ru.yandex.market.checkout.util.balance.TrustMockConfigurer;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.common.util.ChainCalls.safe;
import static ru.yandex.market.checkout.common.util.ChainCalls.safeNull;

/**
 * @author sergeykoles
 * Created on: 27.02.19
 */
@TestHelper
public class TrustTestHelper {

    @Autowired
    private TrustMockConfigurer trustMockConfigurer;

    public void assertClearCallCollection(Matcher<Collection<? extends ServeEvent>> collectionMatcher) {
        assertThat(
                trustMockConfigurer.servedEvents().stream()
                        .filter(WireMockPredicates.clearPaymentEvent())
                        .collect(Collectors.toList()),
                collectionMatcher
        );
    }

    public void resetMockRequests() {
        trustMockConfigurer.resetRequests();
    }


    public static class WireMockPredicates {

        public static Predicate<ServeEvent> clearPaymentEvent() {
            return se -> safe(se, ServeEvent::getRequest, LoggedRequest::getUrl, "").endsWith("/clear");
        }

        public static Predicate<ServeEvent> createPaymentEvent() {
            return se -> {
                final LoggedRequest request = safeNull(se, ServeEvent::getRequest);
                return safe(request, LoggedRequest::getUrl, "").endsWith("/payments?show_trust_payment_id=true")
                        && RequestMethod.POST.equals(safeNull(request, LoggedRequest::getMethod));
            };
        }
    }

}

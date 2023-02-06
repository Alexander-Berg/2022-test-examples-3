package ru.yandex.market.ff.service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.bo.LGWRequestType;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.implementation.LgwRequestTypedServiceProvider;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestAdditionalSupplyService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestCustomerReturnService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestSupplyService;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestWithdrawService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class LgwRequestTypedServiceProviderTest extends IntegrationTest {

    @Autowired
    private Collection<LgwRequestTypedService> services;

    private LgwRequestTypedServiceProvider provider;

    private static Stream<Arguments> supportedParams() {
        return Stream.of(
                Arguments.of(LGWRequestType.asNotXdocForFfApi(RequestType.MOVEMENT_SUPPLY),
                        LgwRequestSupplyService.class),
                Arguments.of(LGWRequestType.asNotXdocForFfApi(RequestType.MOVEMENT_WITHDRAW),
                        LgwRequestWithdrawService.class),
                Arguments.of(LGWRequestType.asNotXdocForFfApi(RequestType.ADDITIONAL_SUPPLY),
                        LgwRequestAdditionalSupplyService.class),
        Arguments.of(LGWRequestType.asNotXdocForFfApi(RequestType.CUSTOMER_RETURN),
                LgwRequestCustomerReturnService.class)
                );
    }

    private static Stream<Arguments> unsupportedParams() {
        return Stream.of(
                Arguments.of(LGWRequestType.asNotXdocForDsApi(RequestType.MOVEMENT_SUPPLY)),
                Arguments.of(LGWRequestType.asNotXdocForDsApi(RequestType.MOVEMENT_WITHDRAW))
        );
    }

    @BeforeEach
    void init() {
        provider = new LgwRequestTypedServiceProvider(services);
    }

    @ParameterizedTest
    @MethodSource("supportedParams")
    void providerParameterizedTest(LGWRequestType lgwRequestType,
                                   Class<?> expectedProvidedClass) {
        LgwRequestTypedService providedService = provider.provide(lgwRequestType);

        assertEquals(expectedProvidedClass, providedService.getClass());
    }

    @ParameterizedTest
    @MethodSource("unsupportedParams")
    void throwsUnsupportedOperationException(LGWRequestType lgwRequestType) {
        assertThrows(UnsupportedOperationException.class, () -> provider.provide(lgwRequestType));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request/requests.xml")
    void shouldImplementPushGetDetailsRequest() {
        Set<RequestType> skipTypes =
                Sets.union(Set.of(RequestType.TRANSFER, RequestType.CROSSDOCK), RequestType.SHADOW_TYPES);
        ShopRequest shopRequest = shopRequestFetchingService.getRequestOrThrow(1L);
        for (RequestType requestType : RequestType.values()) {
            if (skipTypes.contains(requestType)) {
                continue;
            }
            LGWRequestType lgwRequestType = new LGWRequestType(requestType);
            log.debug("Check for: {}", requestType);
            LgwRequestTypedService service = provider.provide(lgwRequestType);
            service.pushGetDetailsRequest(shopRequest);
        }
    }
}

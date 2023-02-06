package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.security.model.Authority;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class FeatureInStatusCheckerTest {

    private final static Authority AUTH_MATCH_STATUSES
            = new Authority("test", "SUBSIDIES:NEW,FAIL,SUCCESS");
    private final static Authority AUTH_NOT_MATCH_STATUSES
            = new Authority("test", "DROPSHIP:-REVOKE,FAIL");
    private FeatureService featureService = mock(FeatureService.class);
    private FeatureInStatusChecker checker = new FeatureInStatusChecker(featureService);

    public static Stream<Arguments> parameters() {
        return Stream.of(
                Arguments.of(ParamCheckStatus.SUCCESS, Boolean.TRUE, 1L, AUTH_MATCH_STATUSES),
                Arguments.of(ParamCheckStatus.NEW, Boolean.TRUE, 1L, AUTH_MATCH_STATUSES),
                Arguments.of(ParamCheckStatus.DONT_WANT, Boolean.FALSE, 1L, AUTH_MATCH_STATUSES),
                Arguments.of(ParamCheckStatus.DONT_WANT, Boolean.FALSE, -1L, AUTH_MATCH_STATUSES),
                Arguments.of(ParamCheckStatus.SUCCESS, Boolean.TRUE, 1L, AUTH_NOT_MATCH_STATUSES),
                Arguments.of(ParamCheckStatus.REVOKE, Boolean.FALSE, 1L, AUTH_NOT_MATCH_STATUSES)
        );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void checkTyped(ParamCheckStatus status, boolean expected, long shopId, Authority auth) {
        if (shopId > 0) {
            when(featureService.getFeature(eq(shopId), any()))
                    .thenReturn(new ShopFeature(shopId, 1, FeatureType.SUBSIDIES, status));
        }

        MockPartnerRequest mockPartnerRequest = Mockito.spy(new MockPartnerRequest(0, 0, shopId, shopId));
        assertThat(
                checker.checkTyped(mockPartnerRequest, auth),
                equalTo(expected)
        );
        //noinspection ResultOfMethodCallIgnored
        Mockito.verify(mockPartnerRequest).getPartnerId();
        //noinspection ResultOfMethodCallIgnored
        Mockito.verify(mockPartnerRequest, times(0)).getDatasourceId();
    }

}

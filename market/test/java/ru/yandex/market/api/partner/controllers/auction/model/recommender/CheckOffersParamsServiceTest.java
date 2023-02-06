package ru.yandex.market.api.partner.controllers.auction.model.recommender;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link CheckOffersParamsService}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class CheckOffersParamsServiceTest {

    @Mock
    private EnvironmentService environmentService;

    @Test
    void test_cachingUrlLength() {
        when(environmentService.getIntValue(eq("mbi.auction.check_offers.max_url_length"), eq(10000)))
                .thenReturn(100);

        final CheckOffersParamsService service = new CheckOffersParamsService(environmentService);
        int res = service.getMaxUriLength();
        assertThat(res, is(100));
        res = service.getMaxUriLength();
        assertThat(res, is(100));

        verify(environmentService, times(1))
                .getIntValue(eq("mbi.auction.check_offers.max_url_length"), eq(10000));
    }

    @Test
    void test_cachingState() {
        when(environmentService.getIntValue(eq("mbi.auction.check_offers.enabled"),
                eq(0)
        )).thenReturn(1);

        final CheckOffersParamsService service = new CheckOffersParamsService(environmentService);
        boolean res = service.isCheckOffersEnabled();
        assertThat(res, is(true));
        res = service.isCheckOffersEnabled();
        assertThat(res, is(true));

        verify(environmentService, times(1))
                .getIntValue(eq("mbi.auction.check_offers.enabled"), eq(0));

    }

}
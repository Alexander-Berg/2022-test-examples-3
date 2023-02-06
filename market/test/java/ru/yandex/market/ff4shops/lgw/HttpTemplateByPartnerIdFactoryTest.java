package ru.yandex.market.ff4shops.lgw;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.logistic.gateway.client.utils.TvmHttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HttpTemplateByPartnerIdFactoryTest extends FunctionalTest {

    private static final long PARTNER_ID = 1L;

    HttpTemplateByPartnerIdFactory factory;

    @Test
    void createWithTvm() {
        factory = getFactory(true);
        assertThat(factory.create(PARTNER_ID))
            .isInstanceOf(TvmHttpTemplateByPartnerId.class);
    }

    @Test
    void createWithoutTvm() {
        factory = getFactory(false);
        assertThat(factory.create(PARTNER_ID))
            .isNotInstanceOf(TvmHttpTemplate.class);
    }

    @NotNull
    private HttpTemplateByPartnerIdFactory getFactory(boolean isTvmEnabled) {
        return new HttpTemplateByPartnerIdFactory(
            "any host",
            mock(RestTemplate.class),
            MediaType.TEXT_XML,
            mock(TvmTicketProvider.class),
            mock(HttpTemplate.class),
            isTvmEnabled
        );
    }
}

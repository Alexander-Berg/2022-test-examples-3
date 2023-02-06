package ru.yandex.market.crm.campaign.services.sending.template;

import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class YaSenderMessageTemplateContextTest {

    private final YaSenderMessageTemplateContext ctx = new YaSenderMessageTemplateContext(null);

    @Test
    public void testWrapSimpleUrl() {
        String preparedUrl = ctx.url("https://market.yandex.ru/product/12473756");
        assertThat(preparedUrl, startsWith("{% wrap \"0\" %}https://market.yandex.ru/product/12473756"));
        assertUtms(preparedUrl);
    }

    @Test
    public void testUnwrapSimpleUrl() {
        String preparedUrl = ctx.url("!https://market.yandex.ru/product/12473756");
        assertThat(preparedUrl, startsWith("https://market.yandex.ru/product/12473756"));
        assertUtms(preparedUrl);
    }

    @Test
    public void testWrapUrlInPlaceholder() {
        String preparedUrl = ctx.url("{{someLink}}");
        assertThat(preparedUrl, startsWith("{% wrap \"0\" %}{{someLink}}"));
        assertUtms(preparedUrl);
    }

    @Test
    public void testUnwrapUrlInPlaceholder() {
        String preparedUrl = ctx.url("!{{someLink}}");
        assertThat(preparedUrl, startsWith("{{someLink}}"));
        assertUtms(preparedUrl);
    }

    /**
     * Url с jinja переменными должен корректно и без ошибок оборачиваться с текущим номером ссылки,
     * а также в него должны быть подставлены utm метки
     */
    @Test
    public void testWrapUrlWithJinjaVariables() {
        String sourceUrl = "https://market.yandex.ru/api/orders/{{orderId}}/returns/{{returnId}}/pdf";
        String preparedUrl = ctx.url(sourceUrl);
        assertThat(preparedUrl, startsWith("{% wrap \"0\" %}" + sourceUrl));
        assertUtms(preparedUrl);
    }

    @Test
    public void testUnwrapUrlWithJinjaVariables() {
        String sourceUrl = "!https://market.yandex.ru/api/orders/{{orderId}}/returns/{{returnId}}/pdf";
        String preparedUrl = ctx.url(sourceUrl);
        assertThat(preparedUrl, startsWith("https://market.yandex.ru/api/orders/{{orderId}}/returns/{{returnId}}/pdf"));
        assertUtms(preparedUrl);
    }

    private void assertUtms(String preparedUrl) {
        assertThat(preparedUrl, containsString("utm_campaign={{ data.utm_campaign }}"));
        assertThat(preparedUrl, containsString("utm_medium={{ data.utm_medium }}"));
        assertThat(preparedUrl, containsString("utm_source={{ data.utm_source }}"));
        assertThat(preparedUrl, containsString("utm_referrer={{ data.utm_referrer }}"));
        assertThat(preparedUrl, containsString("clid={{ data.clid }}"));
    }
}

package ru.yandex.market.crm.campaign.services.sending.template;

import java.time.LocalDateTime;

import org.junit.Test;

import ru.yandex.market.crm.core.domain.HasUtmLinks;
import ru.yandex.market.crm.core.services.sending.UtmLinks;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * @author vtarasoff
 * @since 10.03.2022
 */
public class YaSenderTemplateContextTest {

    private final YaSenderTemplateContext ctx = new YaSenderTemplateContext(
            LocalDateTime.now(),
            UtmLinks.forEmailTrigger("id", HasUtmLinks.from("a", "b", "c", "d", 1L)),
            null);

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
        assertThat(preparedUrl, equalTo("{% wrap \"0\" %}{{someLink}}{% endwrap %}"));
    }

    @Test
    public void testUnwrapUrlInPlaceholder() {
        String preparedUrl = ctx.url("!{{someLink}}");
        assertThat(preparedUrl, equalTo("{{someLink}}"));
    }

    private void assertUtms(String preparedUrl) {
        assertThat(preparedUrl, containsString("utm_campaign=a"));
        assertThat(preparedUrl, containsString("utm_medium=c"));
        assertThat(preparedUrl, containsString("utm_source=b"));
        assertThat(preparedUrl, containsString("utm_referrer=d"));
        assertThat(preparedUrl, containsString("clid=1"));
    }
}

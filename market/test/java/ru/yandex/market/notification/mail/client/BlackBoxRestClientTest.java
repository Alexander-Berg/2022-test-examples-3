package ru.yandex.market.notification.mail.client;

import org.junit.Test;

import ru.yandex.market.notification.mail.client.BlackBoxRestClient.EmailType;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link BlackBoxRestClient}.
 *
 * @author Vladislav Bauer
 */
public class BlackBoxRestClientTest {

    @Test
    public void testEmailTypes() {
        assertThat(EmailType.values(), arrayWithSize(4));

        assertThat(EmailType.ALL.getHttpParam(), equalTo("getall"));
        assertThat(EmailType.YANDEX.getHttpParam(), equalTo("getyandex"));
        assertThat(EmailType.DEFAULT.getHttpParam(), equalTo("getdefault"));
        assertThat(EmailType.TEST.getHttpParam(), equalTo("testone"));
    }

}

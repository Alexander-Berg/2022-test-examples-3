package ru.yandex.market.notification.mail.converter;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.notification.common.converter.AbstractXmlResponseConverter;
import ru.yandex.market.notification.mail.dto.BlackBoxEmailResponse;
import ru.yandex.market.notification.test.converter.AbstractBaseConverterTest;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;


/**
 * Юнит тест для проверки корректной работы {@link BlackBoxEmailResponseHttpConverter}.
 *
 * @author avetokhin 15/06/16.
 */
public class BlackBoxEmailResponseHttpConverterTest extends AbstractBaseConverterTest<BlackBoxEmailResponse> {

    private static final String BLACKBOX_EMAIL_VALID_XML = "blackbox-email-valid.xml";
    private static final String BLACKBOX_EMAIL_ERROR_XML = "blackbox-email-error.xml";


    private final BlackBoxEmailResponseHttpConverter converter = new BlackBoxEmailResponseHttpConverter();


    @Test
    public void readValidResponseTest() throws IOException {
        final BlackBoxEmailResponse response = getResponse(getClass(), BLACKBOX_EMAIL_VALID_XML);

        assertThat(response, notNullValue());
        assertThat(response.getError(), nullValue());
        assertThat(response.getErrorCode(), nullValue());

        final List<String> emails = response.getEmails();
        assertThat(emails, notNullValue());
        assertThat(emails, hasSize(2));
        assertThat(emails.get(0), equalTo("testmk28@yandex.ru"));
        assertThat(emails.get(1), equalTo("testmk29@yandex.ru"));
    }

    @Test
    public void readErrorResponseTest() throws IOException {
        final BlackBoxEmailResponse response = getResponse(getClass(), BLACKBOX_EMAIL_ERROR_XML);

        assertThat(response, notNullValue());
        assertThat(response.getError(), equalTo("BlackBox error: invalid uid value"));
        assertThat(response.getErrorCode(), equalTo("INVALID_PARAMS"));
        assertThat(response.getEmails(), empty());
    }


    @Override
    protected Class<BlackBoxEmailResponse> getType() {
        return BlackBoxEmailResponse.class;
    }

    @Override
    protected AbstractXmlResponseConverter<BlackBoxEmailResponse> getConverter() {
        return converter;
    }

}

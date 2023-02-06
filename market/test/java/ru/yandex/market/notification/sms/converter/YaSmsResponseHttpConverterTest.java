package ru.yandex.market.notification.sms.converter;

import java.io.IOException;

import org.junit.Test;

import ru.yandex.market.notification.common.converter.AbstractXmlResponseConverter;
import ru.yandex.market.notification.sms.dto.YaSmsResponse;
import ru.yandex.market.notification.test.converter.AbstractBaseConverterTest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Юнит тест для проверки корректной работы {@link YaSmsResponseHttpConverter}.
 *
 * @author avetokhin 15/06/16.
 */
public class YaSmsResponseHttpConverterTest extends AbstractBaseConverterTest<YaSmsResponse> {

    private final static String RESPONSE_VALID_XML = "response-valid.xml";
    private final static String RESPONSE_ERROR_XML = "response-error.xml";


    private final YaSmsResponseHttpConverter converter = new YaSmsResponseHttpConverter();


    @Test
    public void readValidResponseTest() throws IOException {
        final YaSmsResponse response = getResponse(getClass(), RESPONSE_VALID_XML);

        assertThat(response, notNullValue());
        assertThat(response.getError(), nullValue());
        assertThat(response.getErrorCode(), nullValue());
        assertThat(response.getId(), equalTo("127000000003456"));
    }

    @Test
    public void readErrorResponseTest() throws IOException {
        final YaSmsResponse response = getResponse(getClass(), RESPONSE_ERROR_XML);

        assertThat(response, notNullValue());
        assertThat(response.getError(), equalTo("User does not have an active phone to recieve messages"));
        assertThat(response.getErrorCode(), equalTo("NOCURRENT"));
        assertThat(response.getId(), nullValue());
    }


    @Override
    protected Class<YaSmsResponse> getType() {
        return YaSmsResponse.class;
    }

    @Override
    protected AbstractXmlResponseConverter<YaSmsResponse> getConverter() {
        return converter;
    }

}

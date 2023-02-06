package ru.yandex.market.notification.sms.converter;

import org.junit.Test;

import ru.yandex.market.notification.sms.dto.YaSmsResponse;
import ru.yandex.market.notification.sms.model.result.SmsFailure;
import ru.yandex.market.notification.sms.model.result.SmsSuccess;
import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.notification.sms.converter.YaSmsResponseConverter.getStatus;

/**
 * Unit-тесты для {@link YaSmsResponseConverter}.
 *
 * @author Vladislav Bauer
 */
public class YaSmsResponseConverterTest {

    private static final String ID = "id";
    private static final String ERROR = "error";
    private static final String ERROR_CODE = "errorCode";


    @Test
    public void testConstructorContract() {
        ClassUtils.checkConstructor(YaSmsResponseConverter.class);
    }

    @Test
    public void testGetStatus() {
        assertThat(getStatus(null), instanceOf(SmsFailure.class));
        assertThat(getStatus(createResponse(ID, "", ERROR_CODE)), instanceOf(SmsFailure.class));
        assertThat(getStatus(createResponse(ID, null, ERROR_CODE)), instanceOf(SmsFailure.class));

        assertThat(getStatus(createResponse(ID, ERROR, ERROR_CODE)), equalTo(new SmsFailure(ERROR)));

        assertThat(getStatus(createResponse(ID, ERROR, "")), equalTo(new SmsFailure(ERROR)));
        assertThat(getStatus(createResponse(ID, ERROR, null)), equalTo(new SmsFailure(ERROR)));

        assertThat(getStatus(createResponse(ID, "", "")), equalTo(new SmsSuccess(ID)));
        assertThat(getStatus(createResponse(ID, null, null)), equalTo(new SmsSuccess(ID)));
    }


    private YaSmsResponse createResponse(final String id, final String error, final String errorCode) {
        final YaSmsResponse response = new YaSmsResponse();
        response.setId(id);
        response.setError(error);
        response.setErrorCode(errorCode);
        return response;
    }

}

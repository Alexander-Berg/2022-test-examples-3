package ru.yandex.direct.yasms;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apache.commons.io.IOUtils;
import org.asynchttpclient.Response;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.http.smart.converter.XmlResponseConverter;
import ru.yandex.direct.yasms.model.SendSmsErrorCode;
import ru.yandex.direct.yasms.model.YaSmsSendSmsResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.http.smart.reflection.Utils.getCallResponseType;
import static ru.yandex.direct.yasms.YaSmsTestUtils.getFailedSendSmsResponse;
import static ru.yandex.direct.yasms.YaSmsTestUtils.getSuccessfulSendSmsResponse;

public class YaSmsClientModelsTest {

    private static final String SEND_SMS_SUCCESS_BODY_FILENAME = "send_sms_success_response.xml";
    private static final String SEND_SMS_FAIL_BODY_FILENAME = "send_sms_fail_response.xml";

    private XmlResponseConverter xmlResponseConverter;
    private Response response;
    private Type responseType;

    @Before
    public void before() {
        xmlResponseConverter = new XmlResponseConverter();
        response = mock(Response.class);

        Method[] apiMethods = YaSmsApi.class.getMethods();
        assertThat(apiMethods.length, is(1));

        responseType = getCallResponseType(apiMethods[0].getGenericReturnType());
    }

    @Test
    public void sendSms_SuccessResponse_ResponseParsed() throws IOException {
        String successBody = IOUtils.toString(getClass().getResourceAsStream(SEND_SMS_SUCCESS_BODY_FILENAME), UTF_8);
        doReturn(successBody).when(response).getResponseBody();

        YaSmsSendSmsResponse parsedResponse = (YaSmsSendSmsResponse) xmlResponseConverter.convert(response, responseType);

        assertThat(parsedResponse, beanDiffer(getSuccessfulSendSmsResponse("127000000003456")));
    }

    @Test
    public void sendSms_FailResponse_ResponseParsed() throws IOException {
        String failBody = IOUtils.toString(getClass().getResourceAsStream(SEND_SMS_FAIL_BODY_FILENAME), UTF_8);
        doReturn(failBody).when(response).getResponseBody();

        YaSmsSendSmsResponse parsedResponse = (YaSmsSendSmsResponse) xmlResponseConverter.convert(response, responseType);

        assertThat(parsedResponse, beanDiffer(getFailedSendSmsResponse(SendSmsErrorCode.NOCURRENT,
                "User does not have an active phone to receive messages")));
    }
}

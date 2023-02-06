package ru.yandex.direct.yasms;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.asynchttp.Result;
import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.yasms.model.YaSmsSendSmsResponse;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.yasms.YaSmsTestUtils.getDefaultFailedSendSmsResponse;
import static ru.yandex.direct.yasms.YaSmsTestUtils.getDefaultSuccessfulSendSmsResponse;

public class YaSmsClientTest {

    private YaSmsApi yaSmsApi;
    private YaSmsClient yaSmsClient;

    @Before
    public void before() throws Exception {
        yaSmsApi = mock(YaSmsApi.class);
        yaSmsClient = new YaSmsClientCommon(new YaSmsClientImpl(yaSmsApi, true));
    }

    @Test
    public void sendSms_Success() {
        YaSmsSendSmsResponse expectedResponse = getDefaultFailedSendSmsResponse();
        mockResponse(expectedResponse);

        YaSmsSendSmsResponse actualResponse = yaSmsClient.sendSms(1L, 1L, "qwerty").getSuccess();

        assertThat(actualResponse, beanDiffer(expectedResponse));
    }

    @Test
    public void sendSms_Fail() {
        YaSmsSendSmsResponse expectedResponse = getDefaultSuccessfulSendSmsResponse();
        mockResponse(expectedResponse);

        YaSmsSendSmsResponse actualResponse = yaSmsClient.sendSms(1L, 1L, "qwerty").getSuccess();

        assertThat(actualResponse, beanDiffer(expectedResponse));
    }

    private void mockResponse(YaSmsSendSmsResponse response) {
        Call<YaSmsSendSmsResponse> call = mockCall(response);

        doReturn(call).when(yaSmsApi).sendSms(anyLong(), anyLong(), anyString(), anyString(), anyInt(), anyMap());
    }

    private <R> Call<R> mockCall(R result) {
        Result<R> response = mock(Result.class);
        doReturn(result).when(response).getSuccess();
        Call<R> call = mock(Call.class);
        doReturn(response).when(call).execute();
        return call;
    }
}

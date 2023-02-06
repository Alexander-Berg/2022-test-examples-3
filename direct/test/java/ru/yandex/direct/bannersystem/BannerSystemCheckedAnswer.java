package ru.yandex.direct.bannersystem;

import io.netty.handler.codec.http.HttpMethod;
import org.asynchttpclient.Request;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.direct.asynchttp.ParsableStringRequest;
import ru.yandex.direct.asynchttp.Result;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assume.assumeTrue;
import static ru.yandex.direct.bannersystem.BannerSystemClient.REQUEST_UUID_HEADER_NAME;

class BannerSystemCheckedAnswer implements Answer<Result<String>> {
    private final String expectedUrl;
    private final String expectedBody;
    private final String successValue;
    private final Throwable error;
    private boolean checkUuid = true;

    BannerSystemCheckedAnswer(String expectedUrl, String successValue) {
        this(expectedUrl, null, successValue, null);
    }

    BannerSystemCheckedAnswer(String expectedUrl, String expectedBody, String successValue) {
        this(expectedUrl, expectedBody, successValue, null);
    }

    BannerSystemCheckedAnswer(String expectedUrl, Throwable error) {
        this(expectedUrl, null, null, error);
    }

    private BannerSystemCheckedAnswer(String expectedUrl, String expectedBody, String successValue, Throwable error) {
        this.expectedUrl = expectedUrl;
        this.expectedBody = expectedBody;
        this.successValue = successValue;
        this.error = error;

        assumeTrue("Error or value must not be null", error != null || successValue != null);
    }

    BannerSystemCheckedAnswer withCheckUuid(boolean checkUuid) {
        this.checkUuid = checkUuid;
        return this;
    }

    @Override
    public Result<String> answer(InvocationOnMock invocation) throws Throwable {
        ParsableStringRequest stringRequest = (ParsableStringRequest) invocation.getArguments()[0];
        Request request = stringRequest.getAHCRequest();

        assertThat("адрес запроса верный", request.getUrl(), equalTo(expectedUrl));
        if (checkUuid) {
            assertThat("в запросе есть request uuid", request.getHeaders().contains(REQUEST_UUID_HEADER_NAME));
        }

        if (expectedBody == null) {
            assertThat("метод запроса GET", request.getMethod(), equalTo(HttpMethod.GET.name()));
            assertThat("тело запроса отсутствует", request.getStringData(), nullValue());
        } else {
            assertThat("метод запроса POST", request.getMethod(), equalTo(HttpMethod.POST.name()));
            assertThat("тело запроса равно ожидаемому", request.getStringData(), equalTo(expectedBody));
        }

        Result<String> result = new Result<>(0);

        if (error != null) {
            result.addError(error);
        } else {
            result.setSuccess(successValue);
        }
        return result;
    }
}

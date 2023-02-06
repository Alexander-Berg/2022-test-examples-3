package ru.yandex.direct.api.v5.service.accelinfo;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.misc.web.servlet.mock.MockHttpServletResponse;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AccelInfoHeaderSetterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApiContextHolder apiContextHolder;

    @InjectMocks
    private AccelInfoHeaderSetter headerSetter;

    private HttpServletResponse httpResponse;

    private final int appCode = 4839;

    @Before
    public void init() {
        initMocks(this);
        httpResponse = new MockHttpServletResponse();

        when(apiContextHolder.get().getAppErrorCode()).thenReturn(appCode);
        when(apiContextHolder.get().getApiResponse()).thenReturn(httpResponse);
    }

    @Test
    public void shouldAddHeaderOfProperFormatToHttpResponse() {
        checkState(getActualHeaderValue() == null);

        headerSetter.setAccelInfoHeaderToHttpResponse();

        assertThat(getActualHeaderValue()).matches("reqid:.*,cmd:.*,appcode:" + appCode);
    }

    private String getActualHeaderValue() {
        return httpResponse.getHeader("X-Accel-Info");
    }

}

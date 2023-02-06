package ru.yandex.direct.api.v5.ws;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.misc.web.servlet.mock.MockHttpServletRequest;
import ru.yandex.misc.web.servlet.mock.MockHttpServletResponse;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SetApiResponseToContextFilterTest {

    @Mock
    private ApiContextHolder apiContextHolder;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ApiContext apiContext;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SetApiResponseToContextFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void init() {
        initMocks(this);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        when(apiContextHolder.get()).thenReturn(apiContext);
    }

    @Test
    public void shouldSetHttpResponseToApiContext() throws Exception {
        checkState(apiContext.getApiResponse() == null);

        callFilter();

        assertThat(apiContext.getApiResponse()).isSameAs(response);
    }

    @Test
    public void shouldTransmitRequestAndResponseFurtherDownTheChain() throws Exception {
        callFilter();

        verify(filterChain).doFilter(same(request), same(response));
    }

    private void callFilter() throws Exception {
        filter.doFilter(request, response, filterChain);
    }

}

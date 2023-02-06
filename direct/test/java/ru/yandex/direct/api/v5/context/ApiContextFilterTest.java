package ru.yandex.direct.api.v5.context;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.misc.web.servlet.mock.MockHttpServletRequest;
import ru.yandex.misc.web.servlet.mock.MockHttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@ParametersAreNonnullByDefault
public class ApiContextFilterTest {

    @Mock
    private ApiContextHolder apiContextHolder;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private ApiContextFilter filter;

    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void init() {
        initMocks(this);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void shouldCreateApiContextAndSetItToApiContextHolder() throws Exception {
        verify(apiContextHolder, never()).set(any());

        callFilter();

        verify(apiContextHolder).set(any(ApiContext.class));
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

package ru.yandex.market.fmcg.bff.filter;

import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fmcg.bff.util.Const;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.fmcg.bff.test.TestUtil.requestWithHeadersMock;

/**
 * @author valter
 */
public class ExperimentWebFilterTest {

    @Test
    public void headersMirroring() throws Exception {
        checkHeader(ExperimentWebFilter.CONFIG_VERSION_HEADER, "my_config");
        checkHeader(ExperimentWebFilter.BOXES_HEADER, "my_boxes");
        checkHeader(Const.ABT_FLAGS_HEADER, "my_flags");
        checkHeader(ExperimentWebFilter.SPLIT_PARAMS_HEADER, "my_split_params");
    }

    private void checkHeader(String name, String value) throws Exception {
        ExperimentWebFilter filter = new ExperimentWebFilter();
        HttpServletResponse responseMock = mock(HttpServletResponse.class);
        filter.doFilter(
            requestWithHeadersMock(Collections.singletonMap(name, value)),
            responseMock,
            mock(FilterChain.class)
        );
        verify(responseMock, atLeastOnce()).addHeader(eq(name), eq(value));
    }
}

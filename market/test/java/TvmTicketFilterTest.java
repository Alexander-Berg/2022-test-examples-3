package ru.yandex.market.starter.tvm;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ru.yandex.market.starter.tvm.filters.ServiceTicketFilter;
import ru.yandex.market.starter.tvm.filters.UserTicketFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TvmTicketFilterTest {
    @Test
    void tvmClientSettings_IgnoreUserTicketAbsence_True_Test() throws ServletException, IOException {
        TvmClientAutoConfigurationTest.TestTvmClient testTvmClient = new TvmClientAutoConfigurationTest.TestTvmClient();
        FilterChain chain = mock(FilterChain.class);
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpServletResponse.SC_OK);
        UserTicketFilter userTicketFilter = new UserTicketFilter(testTvmClient, null, true) {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                return false;
            }
        };

        userTicketFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void tvmClientSettings_IgnoreUserTicketAbsence_False_Test() throws ServletException, IOException {
        TvmClientAutoConfigurationTest.TestTvmClient testTvmClient = new TvmClientAutoConfigurationTest.TestTvmClient();
        FilterChain chain = mock(FilterChain.class);
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpServletResponse.SC_OK);
        UserTicketFilter userTicketFilter = new UserTicketFilter(testTvmClient, null, false) {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                return false;
            }
        };

        userTicketFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void tvmClientSettings_IgnoreServiceTicketAbsence_True_Test() throws ServletException, IOException {
        TvmClientAutoConfigurationTest.TestTvmClient testTvmClient = new TvmClientAutoConfigurationTest.TestTvmClient();
        FilterChain chain = mock(FilterChain.class);
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpServletResponse.SC_OK);
        ServiceTicketFilter serviceTicketFilter = new ServiceTicketFilter(testTvmClient, null, true);

        serviceTicketFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void tvmClientSettings_IgnoreServiceTicketAbsence_False_Test() throws ServletException, IOException {
        TvmClientAutoConfigurationTest.TestTvmClient testTvmClient = new TvmClientAutoConfigurationTest.TestTvmClient();
        FilterChain chain = mock(FilterChain.class);
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpServletResponse.SC_OK);
        ServiceTicketFilter serviceTicketFilter = new ServiceTicketFilter(testTvmClient, null, false);

        serviceTicketFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }
}

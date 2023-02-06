package ru.yandex.market.logistics.management.service.achievery;

import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.achievery.AchieveryResponse;
import ru.yandex.market.logistics.management.service.UserActivityLogService;
import ru.yandex.market.logistics.management.util.LoggableDispatcherServlet;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_RATING_EDIT;

public class AchieveryServiceTest extends AbstractContextualTest {

    private static final String USER_1 = "lmsUser";
    private static final String USER_2 = "dev.1";
    private static final String USER_3 = "dev.3";
    private static final String USER_4 = "dev.4";

    @MockBean
    private AchieveryClient achieveryClient;

    @Autowired
    private MockServletContext servletContext;

    @Autowired
    private LoggableDispatcherServlet servlet;

    @MockBean
    private UserActivityLogService userActivityLogService;

    @BeforeEach
    void setup() throws ServletException {
        Mockito.when(userActivityLogService.getEditSuccessRequestCountByUserLogin(USER_1)).thenReturn(1L);
        Mockito.when(userActivityLogService.getEditSuccessRequestCountByUserLogin(USER_2)).thenReturn(101L);
        Mockito.when(userActivityLogService.getEditSuccessRequestCountByUserLogin(USER_3)).thenReturn(200L);
        Mockito.when(userActivityLogService.getEditSuccessRequestCountByUserLogin(USER_4)).thenReturn(10000L);

        Mockito.when(achieveryClient.getLMSAchievementForUser(USER_1))
            .thenReturn(new AchieveryResponse(1, 0, 1, 1, Collections.emptyList()));
        Mockito.when(achieveryClient.getLMSAchievementForUser(USER_2))
            .thenReturn(new AchieveryResponse(1, 1, 1, 1, List.of(
                new AchieveryResponse.Content(1, 1, 1)
            )));
        Mockito.when(achieveryClient.getLMSAchievementForUser(USER_3))
            .thenReturn(new AchieveryResponse(1, 1, 1, 1, List.of(
                new AchieveryResponse.Content(1, 2, 1)
            )));
        Mockito.when(achieveryClient.getLMSAchievementForUser(USER_4))
            .thenReturn(new AchieveryResponse(1, 1, 1, 1, List.of(
                new AchieveryResponse.Content(1, 5, 1)
            )));

        MockServletConfig servletConfig = new MockServletConfig(servletContext, "loggableServlet");
        servlet.init(servletConfig);
    }

    @Test
    @WithBlackBoxUser(login = USER_1, uid = 1, authorities = {AUTHORITY_ROLE_RATING_EDIT})
    void createNewAchievementTest() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = createRequest();

        servlet.service(request, response);

        Mockito.verify(achieveryClient, Mockito.times(1)).getLMSAchievementForUser(USER_1);
        Mockito.verify(achieveryClient, Mockito.times(1)).createLMSAchievement(USER_1, 1);
    }

    @Test
    @WithBlackBoxUser(login = USER_2, uid = 1, authorities = {AUTHORITY_ROLE_RATING_EDIT})
    void updateAchievementLevelTest() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = createRequest();

        servlet.service(request, response);

        Mockito.verify(achieveryClient, Mockito.times(1)).getLMSAchievementForUser(USER_2);
        Mockito.verify(achieveryClient, Mockito.times(1)).updateLMSAchievement(1, 1, 2);
    }

    @Test
    @WithBlackBoxUser(login = USER_3, uid = 1, authorities = {AUTHORITY_ROLE_RATING_EDIT})
    void doNotUpdateAchievementLevelTest() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = createRequest();

        servlet.service(request, response);

        Mockito.verify(achieveryClient, Mockito.times(1)).getLMSAchievementForUser(USER_3);
        Mockito.verifyNoMoreInteractions(achieveryClient);
    }

    @Test
    @WithBlackBoxUser(login = USER_3, uid = 1, authorities = {AUTHORITY_ROLE_PARTNER})
    void doNotUpdateAchievementAfterNonEditMethod() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = createRequest();
        request.setMethod("GET");
        request.setRequestURI("/admin/lms/partner/3");

        servlet.service(request, response);

        Mockito.verifyNoMoreInteractions(achieveryClient);
    }

    @Test
    @WithBlackBoxUser(login = USER_4, uid = 1, authorities = {AUTHORITY_ROLE_RATING_EDIT})
    void doNotUpdateAchievementToNonExistingLevel() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = createRequest();

        servlet.service(request, response);

        Mockito.verify(achieveryClient, Mockito.times(1)).getLMSAchievementForUser(USER_4);
        Mockito.verifyNoMoreInteractions(achieveryClient);
    }

    private static MockHttpServletRequest createRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setPathInfo("/admin/lms/rating/3");
        request.setRequestURI("/admin/lms/rating/3");
        request.setRemoteAddr("0:0:0:0:0:0:0:1");
        request.setParameter("page", "0");
        request.setParameter("size", "10");
        request.setMethod("DELETE");

        return request;
    }
}

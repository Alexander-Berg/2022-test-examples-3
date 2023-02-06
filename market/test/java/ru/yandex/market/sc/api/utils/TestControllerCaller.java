package ru.yandex.market.sc.api.utils;

import java.text.MessageFormat;
import java.util.Objects;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.route.model.FinishRouteRequestDto;
import ru.yandex.market.sc.core.domain.route.model.RouteCategory;
import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.scan.model.AcceptOrderRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogContext;
import ru.yandex.market.sc.core.domain.task.model.RouteTaskType;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.userPassword.ValidatePasswordRequestDto;
import ru.yandex.market.sc.core.domain.user_auth_log.model.LoginUserDto;
import ru.yandex.market.sc.core.domain.user_auth_log.model.LogoutUserDto;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * @author: dbryndin
 * @date: 7/15/21
 */
public class TestControllerCaller {

    private static final String API_ORDERS_ACCEPT_URL_PUT = "/api/orders/accept";
    private static final String API_ORDERS_ACCEPT_RETURN_URL_PUT = "/api/orders/acceptReturn";
    private static final String API_ORDERS_ACCEPT_UTILIZATION_URL_PUT = "/api/orders/acceptUtilization";
    private static final String API_ORDERS_URL_GET = "/api/orders";
    private static final String API_ORDERS_LIST_URL_GET = "/api/orders/list";
    private static final String API_ROUTES_SHIP_URL_PUT = "/api/routes/{0}";
    private static final String API_ROUTES_BY_ID_GET = "/api/v2/routes/{0}";
    private static final String API_SORTABLE_BETA_SORT = "/api/sortable/beta/sort";
    private static final String API_GET_CELL_FOR_ROUTE = "/api/cells/{0}/forRoute?routeId={1}";

    private static final String API_USER_PWD_GENERATE = "/api/userPassword/generate";
    private static final String API_USER_PWD_VALIDATE = "/api/userPassword/validate";
    private static final String API_USER_CHECK = "/api/users/check";

    private static final String API_USER_LOGIN = "/api/users/login";

    private static final String API_USER_LOGOUT = "/api/users/logout";


    private static final String API_ROUTE_TASK_GET_ROUTES_LIST = "/api/routeTask/list";
    private static final String API_ROUTE_TASK_GET_NEXT_CELL = "/api/routeTask/nextCell";
    private static final String API_ROUTE_TASK_FINISH_CELL = "/api/routeTask/finishCell";

    private static final String API_PUT_MARK_FILLED_CELL_STATUS = "/api/cells/{0}/markFilledStatus";

    private static final String API_PUT_START_ACCEPTANCE = "/api/acceptance/{0}/start";
    private static final String API_PUT_FINISH_ACCEPTANCE = "/api/acceptance/{0}/finish";


    private static final long UID = 123L;

    private final MockMvc mockMvc;
    private final HttpHeaders authHeaderUID;
    private final ObjectMapper objectMapper;

    public TestControllerCaller(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.authHeaderUID = new HttpHeaders();
        this.authHeaderUID.add(HttpHeaders.AUTHORIZATION, "OAuth uid-" + UID);
        this.objectMapper = new ObjectMapper();
    }

    public TestControllerCaller(MockMvc mockMvc, long uid) {
        this.mockMvc = mockMvc;
        this.authHeaderUID = new HttpHeaders();
        this.authHeaderUID.add(HttpHeaders.AUTHORIZATION, "OAuth uid-" + uid);
        this.objectMapper = new ObjectMapper();
    }

    public static TestControllerCaller createCaller(MockMvc mockMvc) {
        return new TestControllerCaller(mockMvc);
    }

    public static TestControllerCaller createCaller(MockMvc mockMvc, long uid) {
        return new TestControllerCaller(mockMvc, uid);
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.OrderController#acceptOrder}
     */
    public ResultActions acceptOrder(AcceptOrderRequestDto request) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.put(API_ORDERS_ACCEPT_URL_PUT)
                        .headers(authHeaderUID)
                        .header("SC-Application-Context", ScanLogContext.INITIAL_ACCEPTANCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.OrderController#acceptReturnedOrder}
     */
    public ResultActions acceptReturn(AcceptOrderRequestDto request) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.put(API_ORDERS_ACCEPT_RETURN_URL_PUT)
                        .headers(authHeaderUID)
                        .header("SC-Application-Context", ScanLogContext.RETURN_SORT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.OrderController#acceptUtilizationOrder}
     */
    public ResultActions acceptUtilization(AcceptOrderRequestDto request) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.put(API_ORDERS_ACCEPT_UTILIZATION_URL_PUT)
                        .headers(authHeaderUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.OrderController#getOrder}
     */
    public ResultActions getOrder(String externalId, String placeExternalId, Long cellId) throws Exception {
        Objects.requireNonNull(externalId);
        var requestBuilder = MockMvcRequestBuilders.get(API_ORDERS_URL_GET)
                .headers(authHeaderUID)
                .param("externalId", externalId);
        if (!Strings.isNullOrEmpty(placeExternalId)) {
            requestBuilder = requestBuilder.param("placeExternalId", placeExternalId);
        }
        if (cellId != null) {
            requestBuilder = requestBuilder.param("cellId", String.valueOf(cellId));
        }
        return mockMvc.perform(requestBuilder)
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.SortableController#sort}
     */
    public ResultActions sortableBetaSort(SortableSortRequestDto request) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.put(API_SORTABLE_BETA_SORT)
                        .headers(authHeaderUID)
                        .header("SC-Application-Context", ScanLogContext.SORT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.OrderController#getRouteOrderIds}
     */
    public ResultActions getOrderIdList(@Nullable Long routeId, @Nullable Long cellId, @Nullable RouteTaskType taskType) throws Exception {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(API_ORDERS_LIST_URL_GET)
                .headers(authHeaderUID);
        if (routeId != null) {
            builder = builder.param("routeId", String.valueOf(routeId));
        }
        if (cellId != null) {
            builder = builder.param("cellId", String.valueOf(cellId));
        }
        if (taskType != null) {
            builder = builder.param("taskType", taskType.name());
        }
        return mockMvc.perform(builder)
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.RouteTaskController#getRouteTaskList}
     */
    public ResultActions getRouteTaskList(RouteType routeType, RouteCategory routeCategory) throws Exception {

        return mockMvc.perform(
                MockMvcRequestBuilders.get(API_ROUTE_TASK_GET_ROUTES_LIST)
                        .param("routeType", routeType.name())
                        .param("category", routeCategory.name())
                        .headers(authHeaderUID))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.RouteTaskController#getNextCell}
     */
    public ResultActions getNextCell(long routeId) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.get(API_ROUTE_TASK_GET_NEXT_CELL)
                        .param("routeId", String.valueOf(routeId))
                        .headers(authHeaderUID))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.RouteTaskController#finishCell}
     */
    public ResultActions finishCell(long routeId, long cellId) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.put(API_ROUTE_TASK_FINISH_CELL)
                        .param("routeId", String.valueOf(routeId))
                        .param("cellId", String.valueOf(cellId))
                        .headers(authHeaderUID))
                .andDo(print());
    }


    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.UserController#loginUser}
     */
    public ResultActions loginUser(User user, LoginUserDto request) throws Exception {
        var curAuthHeaderUID = new HttpHeaders();
        curAuthHeaderUID.add(HttpHeaders.AUTHORIZATION, "OAuth uid-" + user.getUid());
        return mockMvc.perform(
                        MockMvcRequestBuilders.post(API_USER_LOGIN)
                                .headers(curAuthHeaderUID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.UserController#logoutUser}
     */
    public ResultActions logoutUser(User user, LogoutUserDto request) throws Exception {
        return logoutUser(user, request, new HttpHeaders());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.UserController#logoutUser}
     */
    public ResultActions logoutUser(User user, LogoutUserDto request, HttpHeaders headers) throws Exception {
        headers.add(HttpHeaders.AUTHORIZATION, "OAuth uid-" + user.getUid());
        return mockMvc.perform(
                        MockMvcRequestBuilders.post(API_USER_LOGOUT)
                                .headers(headers)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.UserPasswordController#generatePassword}
     */
    public ResultActions generatePassword(User user) throws Exception {
        var curAuthHeaderUID = new HttpHeaders();
        curAuthHeaderUID.add(HttpHeaders.AUTHORIZATION, "OAuth uid-" + user.getUid());
        return mockMvc.perform(
                MockMvcRequestBuilders.put(API_USER_PWD_GENERATE)
                        .headers(curAuthHeaderUID))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.UserPasswordController#generatePassword}
     */
    public ResultActions validatePassword(ValidatePasswordRequestDto request, User user) throws Exception {
        var curAuthHeaderUID = new HttpHeaders();
        curAuthHeaderUID.add(HttpHeaders.AUTHORIZATION, "OAuth uid-" + user.getUid());
        return mockMvc.perform(
                MockMvcRequestBuilders.put(API_USER_PWD_VALIDATE)
                        .headers(curAuthHeaderUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.RouteController#finishRoute}
     */
    public ResultActions ship(Long routeId, FinishRouteRequestDto request) throws Exception {
        Objects.requireNonNull(routeId);
        return mockMvc.perform(
                MockMvcRequestBuilders.put(MessageFormat.format(API_ROUTES_SHIP_URL_PUT, Long.toString(routeId)))
                        .headers(authHeaderUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.RouteController#getOutgoingRouteBaseDto}
     */
    public ResultActions getOutgoingRoute(long routeId) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.get(MessageFormat.format(API_ROUTES_BY_ID_GET, Long.toString(routeId)))
                        .header("Authorization", "OAuth uid-" + UID))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.CellController#markFilledStatus}
     */
    public ResultActions markCellFilled(long cellId, boolean isFull) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.put(MessageFormat.format(API_PUT_MARK_FILLED_CELL_STATUS, cellId))
                        .headers(authHeaderUID)
                        .param("isFull", String.valueOf(isFull))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.CellController#getCellForRoute(User, long, Long)}
     */
    public ResultActions getCellForRoute(long cellId, long routeId) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.get(MessageFormat.format(API_GET_CELL_FOR_ROUTE, cellId, Long.toString(routeId)))
                        .headers(authHeaderUID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.UserController#checkUser}
     */
    public ResultActions checkUser() throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.get(API_USER_CHECK)
                        .headers(authHeaderUID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.AcceptanceController#startAcceptance}
     */
    public ResultActions startAcceptance(String ticketId) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.put(MessageFormat.format(API_PUT_START_ACCEPTANCE, ticketId))
                        .headers(authHeaderUID))
                .andDo(print());
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.AcceptanceController#finishAcceptance}
     */
    public ResultActions finishAcceptance(String ticketId) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.put(MessageFormat.format(API_PUT_FINISH_ACCEPTANCE, ticketId))
                        .headers(authHeaderUID))
                .andDo(print());
    }

}

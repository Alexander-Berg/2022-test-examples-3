package ru.yandex.market.checkout.checkouter.returns;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.StringUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ReturnableCategoryRule;
import ru.yandex.market.checkout.checkouter.balance.xmlrpc.TrustException;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.controllers.oms.ReturnController;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.json.Names;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.history.OrderHistoryEventsRequest;
import ru.yandex.market.checkout.checkouter.pay.BankDetailsNotAvailableException;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.request.BasicOrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.storage.returns.ReturnsDao;
import ru.yandex.market.checkout.checkouter.viewmodel.NonReturnableReasonType;
import ru.yandex.market.checkout.checkouter.viewmodel.ReturnableItemViewModel;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.rest.InvalidRequestException;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ResponseVariable;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams.X_EXPERIMENTS;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.EVENT_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.IS_SIMPLE_RETURN_BY_POST_AVAILABLE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.ORDER_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.PAGE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.PAGE_SIZE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.REGION_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SHOP_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.STATUS;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.UID;
import static ru.yandex.market.checkout.checkouter.returns.ReturnDecisionType.OTHER_DECISION;
import static ru.yandex.market.checkout.checkouter.returns.ReturnDecisionType.REFUND_MONEY;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.DECISION_MADE;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.FAILED;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.REFUNDED;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.REFUNDED_BY_SHOP;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.REFUNDED_WITH_BONUSES;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.REFUND_IN_PROGRESS;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.STARTED_BY_USER;
import static ru.yandex.market.checkout.checkouter.returns.ReturnStatus.WAITING_FOR_DECISION;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParametersWithItems;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceResponseVariant.FAIL;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceResponseVariant.FOR_RETURN;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceResponseVariant.NO_MATCH;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceResponseVariant.WITH_PURPOSE;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceXMLRPCMethod.CreatePerson;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceXMLRPCMethod.GetPerson;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;
import static ru.yandex.market.util.FormatUtils.toJson;


/**
 * @author sergeykoles
 * Created on: 22.02.18
 */
public class ReturnControllerTest extends AbstractWebTestBase {

    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private ReturnsDao dao;
    @Autowired
    private ReturnService returnService;
    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        returnHelper.mockShopInfo();
    }

    @Test
    @DisplayName("Проверяем получение информации по id заказа и id возврата без банковских реквизитов")
    public void testGetWithoutBankDetails() throws Exception {
        Pair<Order, Return> orderAndReturn = returnHelper.createOrderAndReturn(new Parameters(), null);
        Return ret = orderAndReturn.getSecond();
        Order order = orderAndReturn.getFirst();
        ReturnItem retItem = ret.getItems().iterator().next();
        ResultActionsContainer resultMatchers = new ResultActionsContainer();
        resultMatchers.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ret.getId()))
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.userCompensationSum")
                        .value(
                                closeTo(
                                        ret.getUserCompensationSum().doubleValue(),
                                        0.01D
                                )
                        ))
                .andExpect(jsonPath("$.comment").value(ret.getComment()))
                .andExpect(jsonPath("$.status").value(ret.getStatus().toString()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].itemId").value(retItem.getItemId()))
                .andExpect(jsonPath("$.items[0].isDeliveryService").value(retItem.isDeliveryService()))
                .andExpect(jsonPath("$.items[0].count").value(retItem.getCount()))
                .andExpect(jsonPath("$.items[0].quantity")
                        .value(numberEqualsTo(retItem.getQuantityIfExistsOrCount())))
                .andExpect(jsonPath("$.items[0].supplierCompensationSum").value(
                        closeTo(
                                retItem.getSupplierCompensation().doubleValue(), 0.01D
                        )))
                .andExpect(jsonPath("$.items[0].bankDetails").doesNotExist());
        returnHelper.getReturnById(order.getId(), ret.getId(), resultMatchers);
    }

    @Test
    public void testGetReturnsByOrderId() throws Exception {
        Parameters orderParameters = new Parameters();
        // создадим хороший такой заказ на 20 позиций

        orderParameters.getOrder().setItems(
                OrderItemProvider.stream("testGetReturnsByOrderId")
                        .limit(20)
                        .collect(toList())
        );
        Order order = orderCreateHelper.createOrder(orderParameters);

        // далее создаём 10 возвратов на 2 позиции
        ArrayList<Return> rets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int idx = i;
            rets.add(
                    returnHelper.createReturnForOrder(order,
                            (r, o) -> {
                                r.setItems(
                                        r.getItems().subList(idx * 2, idx * 2 + 2)
                                );
                                return r;
                            }
                    )
            );
        }

        // сортируем на всякий
        List<Return> sortedById = rets.stream()
                .sorted(Comparator.comparing(Return::getId).reversed())
                .collect(toList());

        // догадываемся, какие должны получить возвраты
        List<Return> pageReturns = sortedById.subList(3, 6);

        // просим страницу и проверяем, те ли id получили (упорядочивание по id)
        // тут может стоит и убрать проверку конкретных id. некоторое закладывание на реализацию
        mockMvc.perform(
                get("/orders/{orderId}/returns",
                        order.getId())
                        .param(PAGE, "2")
                        .param(PAGE_SIZE, "3")
                        .param("clientRole", "SYSTEM")
                        .param("uid", "3331")
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.pager.total").value(10))
                .andExpect(jsonPath("$.pager.from").value(4))
                .andExpect(jsonPath("$.pager.to").value(6))
                .andExpect(jsonPath("$.pager.pagesCount").value(4))
                .andExpect(jsonPath("$.pager.page").value(2))
                .andExpect(jsonPath("$.returns").isArray())
                .andExpect(jsonPath("$.returns.length()").value(3))
                .andExpect(jsonPath("$.returns[*].id").value(Matchers.equalTo(
                        pageReturns.stream()
                                .map(r -> r.getId().intValue())
                                .collect(toList())
                )));
    }

    @Test
    public void testGetReturnsForShop() throws Exception {
        Parameters firstShopOrderParameters = new Parameters();

        firstShopOrderParameters.getOrder().setItems(
                OrderItemProvider.stream("testGetReturnsByOrderId")
                        .limit(20)
                        .collect(toList())
        );
        firstShopOrderParameters.setShopId(700L);
        Order firstShopOrder = orderCreateHelper.createOrder(firstShopOrderParameters);

        // далее создаём 10 возвратов на 2 позиции
        ArrayList<Return> rets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int idx = i;
            rets.add(
                    returnHelper.createReturnForOrder(firstShopOrder,
                            (r, o) -> {
                                r.setItems(
                                        r.getItems().subList(idx * 2, idx * 2 + 2)
                                );
                                r.setDelivery(returnHelper.getDefaultReturnDelivery());
                                return r;
                            }
                    )
            );
        }

        Parameters secondShopOrderParameters = new Parameters();

        secondShopOrderParameters.setShopId(800L);
        Order secondShopOrder = orderCreateHelper.createOrder(secondShopOrderParameters);

        returnHelper.createReturnForOrder(secondShopOrder,
                (ret, ord) -> {
                    ret.setStatus(STARTED_BY_USER);
                    ReturnItem returnItem = createReturnItem(null, null, secondShopOrder);
                    ret.setItems(List.of(returnItem, returnItem, returnItem));
                    ret.setDelivery(returnHelper.getDefaultReturnDelivery());
                    return ret;
        });

        List<Return> sortedById = rets.stream()
                .sorted(Comparator.comparing(Return::getId).reversed())
                .collect(toList());

        List<Return> pageReturns = sortedById.subList(0, 5);

        mockMvc.perform(
                get("/returns")
                        .param(SHOP_ID, "700")
                        .param(PAGE, "1")
                        .param(PAGE_SIZE, "5")
                        .param("clientRole", "SHOP")
                        .param("uid", "3331")
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.pager.total").value(10))
                .andExpect(jsonPath("$.pager.from").value(1))
                .andExpect(jsonPath("$.pager.to").value(5))
                .andExpect(jsonPath("$.pager.pagesCount").value(2))
                .andExpect(jsonPath("$.pager.page").value(1))
                .andExpect(jsonPath("$.returns[0].delivery.type").value("PICKUP"))
                .andExpect(jsonPath("$.returns").isArray())
                .andExpect(jsonPath("$.returns.length()").value(5))
                .andExpect(jsonPath("$.returns[*].id").value(Matchers.equalTo(
                        pageReturns.stream()
                                .map(r -> r.getId().intValue())
                                .collect(toList())
                )));
    }

    @Test
    public void testGetReturnsForShopByOrderIdAndStatuses() throws Exception {
        Parameters firstOrderParameters = new Parameters();

        firstOrderParameters.getOrder().setItems(
                OrderItemProvider.stream("testGetReturnsForShop1")
                        .limit(2)
                        .collect(toList())
        );
        firstOrderParameters.setShopId(700L);
        Order firstOrder = orderCreateHelper.createOrder(firstOrderParameters);

        Parameters secondOrderParameters = new Parameters();

        secondOrderParameters.getOrder().setItems(
                OrderItemProvider.stream("testGetReturnsForShop2")
                        .limit(2)
                        .collect(toList())
        );
        secondOrderParameters.setShopId(700L);
        Order secondOrder = orderCreateHelper.createOrder(secondOrderParameters);

        Parameters thirdOrderParameters = new Parameters();

        thirdOrderParameters.getOrder().setItems(
                OrderItemProvider.stream("testGetReturnsForShop2")
                        .limit(2)
                        .collect(toList())
        );
        thirdOrderParameters.setShopId(700L);
        Order thirdOrder = orderCreateHelper.createOrder(thirdOrderParameters);

        returnHelper.createReturnForOrder(firstOrder, (r, o) -> {
            r.setStatus(ReturnStatus.WAITING_FOR_DECISION);
            return r;
        });

        returnHelper.createReturnForOrder(secondOrder, (r, o) -> {
            r.setStatus(DECISION_MADE);
            return r;
        });

        List<Return> resultReturns = new ArrayList<>();

        resultReturns.add(
                returnHelper.createReturnForOrder(firstOrder, (r, o) -> {
                            r.setStatus(STARTED_BY_USER);
                            return r;
                        }
                ));

        resultReturns.add(
                returnHelper.createReturnForOrder(firstOrder, (r, o) -> {
                    r.setStatus(DECISION_MADE);
                    return r;
                })
        );

        resultReturns.add(
                returnHelper.createReturnForOrder(thirdOrder, (r, o) -> {
                    r.setStatus(STARTED_BY_USER);
                    return r;
                })
        );

        resultReturns = resultReturns.stream()
                .sorted(Comparator.comparing(Return::getId).reversed())
                .collect(toList());

        mockMvc.perform(
                get("/returns")
                        .param(SHOP_ID, "700")
                        .param(ORDER_ID, firstOrder.getId().toString())
                        .param(ORDER_ID, thirdOrder.getId().toString())
                        .param(PAGE, "1")
                        .param(PAGE_SIZE, "100")
                        .param(ORDER_ID, firstOrder.getId().toString())
                        .param(STATUS, STARTED_BY_USER.name())
                        .param(STATUS, DECISION_MADE.name())
                        .param("clientRole", "SHOP")
                        .param("uid", "3331")
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.pager.total").value(3))
                .andExpect(jsonPath("$.pager.from").value(1))
                .andExpect(jsonPath("$.pager.to").value(3))
                .andExpect(jsonPath("$.pager.pagesCount").value(1))
                .andExpect(jsonPath("$.pager.page").value(1))
                .andExpect(jsonPath("$.returns").isArray())
                .andExpect(jsonPath("$.returns.length()").value(3))
                .andExpect(jsonPath("$.returns[*].id").value(Matchers.equalTo(
                        resultReturns.stream()
                                .map(r -> r.getId().intValue())
                                .collect(toList())
                )));
    }

    @Test
    public void testGetReturnsForShopInvalidRole() throws Exception {
        Parameters orderParameters = new Parameters();

        orderParameters.setShopId(700L);
        Order order = orderCreateHelper.createOrder(orderParameters);

        returnHelper.createReturnForOrder(order, (r, o) -> {
            r.setStatus(STARTED_BY_USER);
            return r;
        });

        mockMvc.perform(
                        get("/returns")
                                .param(SHOP_ID, "700")
                                .param("clientRole", "USER")
                                .param("uid", "3331")
                ).andExpect(status().is4xxClientError());
    }

    @Test
    public void testGetReturnsByOrderListAndStatuses() throws Exception {
        Parameters orderParameters = new Parameters();
        // создадим хороший такой заказ на 20 позиций

        orderParameters.getOrder().setItems(
                OrderItemProvider.stream("testGetReturnsByOrderId")
                        .limit(20)
                        .collect(toList())
        );
        Order order = orderCreateHelper.createOrder(orderParameters);

        // далее создаём 10 возвратов на 2 позиции
        ArrayList<Return> rets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int idx = i;
            rets.add(
                    returnHelper.createReturnForOrder(order,
                            (r, o) -> {
                                r.setItems(
                                        r.getItems().subList(idx * 2, idx * 2 + 2)
                                );
                                r.setStatus(ReturnStatus.REFUNDED);
                                return r;
                            }
                    )
            );
        }

        // сортируем на всякий
        List<Return> sortedById = rets.stream()
                .sorted(Comparator.comparing(Return::getId).reversed())
                .collect(toList());

        // догадываемся, какие должны получить возвраты
        List<Return> pageReturns = sortedById.subList(3, 6);

        // просим страницу и проверяем, те ли id получили (упорядочивание по id)
        // тут может стоит и убрать проверку конкретных id. некоторое закладывание на реализацию
        mockMvc.perform(
                get("/orders/{orderId}/returns",
                        order.getId())
                        .param("status", "REFUNDED")
                        .param("status", "FAILED")
                        .param(PAGE, "2")
                        .param(PAGE_SIZE, "3")
                        .param("clientRole", "SYSTEM")
                        .param("uid", "3331")
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.pager.total").value(10))
                .andExpect(jsonPath("$.pager.from").value(4))
                .andExpect(jsonPath("$.pager.to").value(6))
                .andExpect(jsonPath("$.pager.pagesCount").value(4))
                .andExpect(jsonPath("$.pager.page").value(2))
                .andExpect(jsonPath("$.returns").isArray())
                .andExpect(jsonPath("$.returns.length()").value(3))
                .andExpect(jsonPath("$.returns[*].id").value(Matchers.equalTo(
                        pageReturns.stream()
                                .map(r -> r.getId().intValue())
                                .collect(toList())
                )));
    }

    @Test
    @DisplayName("Сохранение банковских данных пользователя")
    public void getReturnByIdWithBankDetailsTest() {
        Return initial = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null).getSecond();
        Return entity = dao.findReturnById(initial.getId())
                .orElseThrow(() -> new RuntimeException("Return not found!"));
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, FOR_RETURN,
                makeParamsForBalanceResponse(entity.getCompensationClientId(), entity.getCompensationPersonId()));

        Return result = client.returns()
                .getReturn(initial.getOrderId(), initial.getId(), true, ClientRole.SYSTEM, 0);

        assertThat(result.getBankDetails().getAccount(), equalTo("00000000000000000007"));
        assertThat(result.getBankDetails().getCorraccount(), equalTo("54321"));
        assertThat(result.getBankDetails().getBik(), equalTo("000000007"));
        assertThat(result.getBankDetails().getBank(), equalTo("Банк России"));
        assertThat(result.getBankDetails().getBankCity(), equalTo("Москва"));
        assertThat(result.getBankDetails().getFirstName(), equalTo("Иван"));
        assertThat(result.getBankDetails().getLastName(), equalTo("Рюриков"));
        assertThat(result.getBankDetails().getMiddleName(), equalTo("Васильевич"));
    }

    @Test
    @DisplayName("Сохранение банковских данных пользователя c полем назначение платежа")
    public void getReturnByIdWithBankDetailsWithPaymentPurposeTest() {
        Return initial = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null).getSecond();
        Return entity = dao.findReturnById(initial.getId())
                .orElseThrow(() -> new RuntimeException("Return not found!"));
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, WITH_PURPOSE,
                makeParamsForBalanceResponse(entity.getCompensationClientId(), entity.getCompensationPersonId()));

        Return result = client.returns()
                .getReturn(initial.getOrderId(), initial.getId(), true, ClientRole.SYSTEM, 0);

        assertThat(result.getBankDetails().getAccount(), equalTo("00000000000000000008"));
        assertThat(result.getBankDetails().getCorraccount(), equalTo("54321"));
        assertThat(result.getBankDetails().getBik(), equalTo("000000008"));
        assertThat(result.getBankDetails().getBank(), equalTo("Банк России"));
        assertThat(result.getBankDetails().getBankCity(), equalTo("Москва"));
        assertThat(result.getBankDetails().getFirstName(), equalTo("Иван"));
        assertThat(result.getBankDetails().getLastName(), equalTo("Рюриков"));
        assertThat(result.getBankDetails().getMiddleName(), equalTo("Васильевич"));
        assertThat(result.getBankDetails().getPaymentPurpose(), equalTo("Хочу денег"));
    }

    @Test
    public void updateBankDetailsSuccessTest() throws Exception {
        Return initial = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null).getSecond();
        Return entity = dao.findReturnById(initial.getId())
                .orElseThrow(() -> new RuntimeException("Return not found!"));

        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, FOR_RETURN,
                makeParamsForBalanceResponse(entity.getCompensationClientId(), entity.getCompensationPersonId()));
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(CreatePerson, null,
                Collections.singletonMap(ResponseVariable.PERSON_ID, entity.getCompensationPersonId()));
        //test happy path
        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/bank-details", initial.getOrderId(), initial.getId())
                .param(CLIENT_ROLE, ClientRole.REFEREE.name())
                .param(UID, String.valueOf(0))
                .contentType("application/json")
                .content(getValidBankDetailsAsJson())
        ).andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    public void updateBankDetailsInvalidInputDataTest() throws Exception {
        Return initial = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null).getSecond();
        Return entity = dao.findReturnById(initial.getId())
                .orElseThrow(() -> new RuntimeException("Return not found!"));

        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, FOR_RETURN,
                makeParamsForBalanceResponse(entity.getCompensationClientId(), entity.getCompensationPersonId()));
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(CreatePerson, null,
                Collections.singletonMap(ResponseVariable.PERSON_ID, entity.getCompensationPersonId()));

        //test if bank details have invalid format
        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/bank-details", initial.getOrderId(), initial.getId())
                .param(CLIENT_ROLE, ClientRole.REFEREE.name())
                .param(UID, String.valueOf(0))
                .contentType("application/json")
                .content(getInvalidBankDetailsAsJson())
        ).andExpect(status().is4xxClientError());
    }


    @Test
    public void updateBankDetailsExternalServerErrorTest() throws Exception {
        Return initial = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null).getSecond();
        Return entity = dao.findReturnById(initial.getId())
                .orElseThrow(() -> new RuntimeException("Return not found!"));
        //test if test if request to xml-rpc failed
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, FAIL,
                makeParamsForBalanceResponse(entity.getCompensationClientId(), entity.getCompensationPersonId()));

        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/bank-details", initial.getOrderId(), initial.getId())
                .param(CLIENT_ROLE, ClientRole.REFEREE.name())
                .param(UID, String.valueOf(0))
                .contentType("application/json")
                .content(getValidBankDetailsAsJson())
        ).andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Ошибка при проблемах в балансе")
    public void errorIfBalanceFailedToReturn() {
        Assertions.assertThrows(TrustException.class, () -> {
            Return initial = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null).getSecond();
            Return entity = dao.findReturnById(initial.getId())
                    .orElseThrow(() -> new RuntimeException("Return not found!"));
            trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, FAIL,
                    makeParamsForBalanceResponse(entity.getCompensationClientId(), entity.getCompensationPersonId()));

            returnService.findReturnByIdAndOrderId(initial.getId(), initial.getOrderId(), true, ClientInfo.SYSTEM);
        });
    }

    @Test
    @DisplayName("Не удалось получить банковские данные из баланса")
    public void errorIfNoBankDetailsAvailableTest() {
        Assertions.assertThrows(BankDetailsNotAvailableException.class, () -> {
            Return initial = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null).getSecond();
            Return entity = dao.findReturnById(initial.getId())
                    .orElseThrow(() -> new RuntimeException("Return not found!"));
            trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, NO_MATCH,
                    makeParamsForBalanceResponse(entity.getCompensationClientId(), entity.getCompensationPersonId()));

            returnService.findReturnByIdAndOrderId(initial.getId(), initial.getOrderId(), true, ClientInfo.SYSTEM);
        });
    }

    @Test
    @DisplayName("Получение возврата (роль SYSTEM)")
    public void testGetReturnByIdAsSystem() {
        Return initial = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null).getSecond();
        Return entity = dao.findReturnById(initial.getId())
                .orElseThrow(() -> new RuntimeException("Return not found!"));
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, FOR_RETURN,
                makeParamsForBalanceResponse(entity.getCompensationClientId(), entity.getCompensationPersonId()));

        Return result = client.returns()
                .getReturn(initial.getOrderId(), initial.getId(), true, ClientRole.SYSTEM, 0);
        assertThat(result, notNullValue());
    }

    @Test
    @DisplayName("Получение возврата (роль REFEREE)")
    public void testGetReturnByIdAsReferee() {
        Return initial = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null).getSecond();
        Return entity = dao.findReturnById(initial.getId())
                .orElseThrow(() -> new RuntimeException("Return not found!"));
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, FOR_RETURN,
                makeParamsForBalanceResponse(entity.getCompensationClientId(), entity.getCompensationPersonId()));

        Return result = client.returns()
                .getReturn(initial.getOrderId(), initial.getId(), true, ClientRole.REFEREE, 131415L);
        assertThat(result, notNullValue());
    }

    @Test
    @DisplayName("Получение возврата (роль USER)")
    public void testGetReturnByIdAsUser() {
        Pair<Order, Return> orderAndReturn = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null);
        Return initial = orderAndReturn.getSecond();
        Order order = orderAndReturn.getFirst();
        Return entity = dao.findReturnById(initial.getId())
                .orElseThrow(() -> new RuntimeException("Return not found!"));
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, FOR_RETURN,
                makeParamsForBalanceResponse(entity.getCompensationClientId(), entity.getCompensationPersonId()));

        Return result = client.returns()
                .getReturn(initial.getOrderId(), initial.getId(), true, ClientRole.USER, order.getBuyer().getUid());
        assertThat(result, notNullValue());
    }

    @Test
    @DisplayName("Получение возврата (роль неправильный USER)")
    public void testGetReturnByIdAsWrongUser() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            Return initial = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null).getSecond();
            Return entity = dao.findReturnById(initial.getId())
                    .orElseThrow(() -> new RuntimeException("Return not found!"));
            trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, FOR_RETURN,
                    makeParamsForBalanceResponse(entity.getCompensationClientId(), entity.getCompensationPersonId()));

            client.returns()
                    .getReturn(initial.getOrderId(), initial.getId(), true, ClientRole.USER, 131415L);
        });
    }

    @Test
    @DisplayName("Ручка /returns/items фильтрует невозвратные категории")
    public void testReturnableItemsHandle() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.addOtherItem();
        parameters.addAnotherItem();

        returnHelper.setNonReturnableItemsViaHttp(
                Arrays.asList(
                        new ReturnableCategoryRule(999, 0),
                        new ReturnableCategoryRule(123, 15)
                )
        );

        parameters.getOrder().getItems().stream()
                .findAny()
                .orElseThrow(() -> new RuntimeException("Nema"))
                .setCategoryId(999);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        ReturnableItemsResponse returnableItems = client.returns()
                .getReturnableItems(order.getId(), ClientRole.USER, order.getBuyer().getUid());
        assertThat(returnableItems.getReturnableItems(), hasSize(2));
        assertThat(returnableItems.getNonReturnableItems(), hasSize(1));
        assertThat(returnableItems.getNonReturnableReasonSet(),
                hasItem(NonReturnableReasonType.NOT_RETURNABLE_CATEGORY));

        returnHelper.setNonReturnableItemsViaHttp(Collections.emptyList());
    }

    @Test
    @DisplayName("Ручка /returns/items вовзращает причину невозврата цифровых товаров")
    public void testReturnableItemsReturnDigitalItemReason() {
        Parameters parameters = WhiteParametersProvider.digitalOrderPrameters();

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        ReturnableItemsResponse returnableItems = client.returns()
                .getReturnableItems(order.getId(), ClientRole.USER, order.getBuyer().getUid());
        assertThat(returnableItems.getReturnableItems(), hasSize(0));
        assertThat(returnableItems.getNonReturnableReasonSet(),
                contains(NonReturnableReasonType.NON_RETURNABLE_DIGITAL_ITEM));
    }

    @Test
    @DisplayName("Ручка /returns/items вовзращает цену товаров")
    public void testReturnableItemsReturnItemPrices() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        ReturnableItemsResponse returnableItems = client.returns()
                .getReturnableItems(order.getId(), ClientRole.SYSTEM, 1L);
        assertThat(returnableItems.getReturnableItems(), hasSize(1));
        order = orderService.getOrder(order.getId());
        OrderItem orderItem = order.getItems().iterator().next();
        ReturnableItemViewModel returnableItem = returnableItems.getReturnableItems().iterator().next();
        assertEquals(0, orderItem.getBuyerPrice().compareTo(returnableItem.getBuyerPrice()));
        assertEquals(0, orderItem.getPrice().compareTo(returnableItem.getPrice()));
    }

    @Test
    @DisplayName("Ручка /returns/items возвращает все айтемы как возвратные, для постоплатных C&C заказов")
    public void testNoPaymentOrderReturnableItems() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());
        assertThat(order.getPayment(), nullValue());
        ReturnableItemsResponse returnableItems = client.returns()
                .getReturnableItems(order.getId(), ClientRole.SYSTEM, 1L);
        assertThat(returnableItems.getNonReturnableReasonSet(), empty());
        assertThat(returnableItems.getReturnableItems(), hasSize(1));
        assertThat(returnableItems.getNonReturnableItems(), empty());
    }

    @Test
    @DisplayName("Ручка /returns/items фильтрует айтемы для постоплатных C&C заказов")
    public void testNoPaymentOrderReturnableItemsNoCategory() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        returnHelper.setNonReturnableItemsViaHttp(
                Arrays.asList(
                        new ReturnableCategoryRule(998, 0)
                )
        );

        parameters.getOrder().getItems().stream()
                .findAny()
                .orElseThrow(() -> new RuntimeException("Nema"))
                .setCategoryId(998);

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        order = orderService.getOrder(order.getId());
        assertThat(order.getPayment(), nullValue());
        ReturnableItemsResponse returnableItems = client.returns()
                .getReturnableItems(order.getId(), ClientRole.USER, order.getBuyer().getUid());
        assertThat(returnableItems.getNonReturnableReasonSet(), hasSize(1));
        assertThat(returnableItems.getNonReturnableReasonSet(),
                hasItem(NonReturnableReasonType.NOT_RETURNABLE_CATEGORY));
        assertThat(returnableItems.getReturnableItems(), empty());
        assertThat(returnableItems.getNonReturnableItems(), hasSize(1));
    }

    @Test
    @DisplayName("Получение PDF возврата")
    public void testGetReturnPdfByOrderIdAndReturnId() throws Exception {
        Pair<Order, Return> orderAndReturn = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null);

        Order order = orderAndReturn.getFirst();
        Long orderId = order.getId();

        Return initialReturn = orderAndReturn.getSecond();
        Return testReturn = dao.findReturnById(initialReturn.getId())
                .orElseThrow(() -> new RuntimeException("Return not found!"));
        Long returnId = testReturn.getId();
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, FOR_RETURN,
                makeParamsForBalanceResponse(testReturn.getCompensationClientId(),
                        testReturn.getCompensationPersonId()));

        mockMvc.perform(get("/orders/{orderId}/returns/{returnId}/pdf",
                orderId, returnId)
                .param("clientRole", "USER")
                .param("uid", order.getBuyer().getUid().toString())
        )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        equalTo("attachment; filename="
                                + ReturnController.RETURN_REQUEST_PDF + returnId + ".pdf")))
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    @DisplayName("Возврат не создается при длине описания больше 1000 символов")
    public void shouldNotCreateReturnWithTooLongReturnReason() {
        Order order = orderCreateHelper.createOrder(defaultBlueOrderParameters());

        Return aReturn = ReturnProvider.generateReturn(order);
        aReturn.getItems().forEach(ri -> ri.setReturnReason(StringUtils.multiply("a", 1001, "")));
        Assertions.assertThrows(InvalidRequestException.class,
                () -> returnService.validateReturnRequestBody(aReturn, false, ClientRole.SYSTEM),
                "Return reason is too long. Max length is 1000"
        );
    }

    @Test
    @DisplayName("Возврат создается при длине описания меньше 1000 символов")
    public void shouldCreateReturnWithLongReturnReason() {
        Order order = orderCreateHelper.createOrder(defaultBlueOrderParameters());

        Return aReturn = ReturnProvider.generateReturn(order);
        aReturn.getItems().forEach(ri -> ri.setReturnReason(StringUtils.multiply("a", 800, "")));
        Assertions.assertDoesNotThrow(
                () -> returnService.validateReturnRequestBody(aReturn, false, ClientRole.SYSTEM)
        );
    }

    @Test
    @DisplayName("Проверяем, что возможно создать возврат по постоплатному заказу без текущего платежа")
    public void shouldCreatePostPaidReturnWithNoPayment() {
        Order order = OrderProvider.getBlueOrder();
        order.setPaymentType(PaymentType.POSTPAID);
        order.setItems(OrderItemProvider.getDefaultItems());
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        Return aReturn = ReturnProvider.generateReturn(order);
        aReturn.setBankDetails(null);
        aReturn.setId(null);
        returnHelper.initReturn(order.getId(), aReturn);
    }

    @Test
    @DisplayName("Проверяем, что курьерские опции отдаются, при их наличии в ответе репорта")
    public void shouldGetCourierOptions() throws Exception {
        Order order = OrderProvider.getBlueOrder();
        order.setPaymentType(PaymentType.POSTPAID);
        order.setItems(OrderItemProvider.getDefaultItems());
        order.setFulfilment(true);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        returnHelper.mockActualDelivery(order, 345L);
        MvcResult result = mockMvc.perform(post("/orders/{orderId}/returns/options",
                        orderId)
                        .param(CLIENT_ROLE, "USER")
                        .param(UID, order.getBuyer().getUid().toString())
                        .param(REGION_ID, String.valueOf(100L))
                        .header(X_EXPERIMENTS, Experiments.empty().toExperimentString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(
                                order.getItems().stream().map(item -> {
                                    ReturnItem returnItem = new ReturnItem();
                                    returnItem.setCount(item.getCount());
                                    returnItem.setQuantity(item.getQuantityIfExistsOrCount());
                                    returnItem.setItemId(item.getId());
                                    return returnItem;
                                }).collect(Collectors.toUnmodifiableList())
                        ))
                )
                .andExpect(status().isOk())
                .andReturn();
        List<ReturnDelivery> courierOptions = extractCourierOptions(result);
        assertThat(courierOptions, hasSize(1));
        ReturnDelivery courierOption = courierOptions.get(0);
        assertThat(courierOption.getDeliveryServiceId(), is(345L));
        assertNotNull(courierOption.getDates());
        assertNotNull(courierOption.getDates().getFromDate());
        assertNotNull(courierOption.getDates().getToDate());
        assertNotNull(courierOption.getDates().getFromTime());
        assertNotNull(courierOption.getDates().getToTime());
    }

    private List<ReturnDelivery> extractCourierOptions(MvcResult result) {
        try {
            return (mapper
                    .readValue(result.getResponse().getContentAsString(), ReturnOptionsResponse.class)
                    .getDeliveryOptions().stream()
                    .filter(option -> option.getType() == DeliveryType.DELIVERY)
                    .collect(Collectors.toList()));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    @DisplayName("Проверяем, что почтовые опции добавляются")
    public void shouldGetPostOptions() throws Exception {
        reportConfigurer.mockActualDeliveryFromString(getReportCombinatorResponseAsJson());
        Order order = OrderProvider.getBlueOrder();
        order.setPaymentType(PaymentType.POSTPAID);
        order.setItems(OrderItemProvider.getDefaultItems());
        order.setFulfilment(false);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        mockMvc.perform(post("/orders/{orderId}/returns/options",
                orderId)
                .param(CLIENT_ROLE, "USER")
                .param(UID, order.getBuyer().getUid().toString())
                .param(REGION_ID, String.valueOf(100L))
                .header(X_EXPERIMENTS, Experiments.empty().toExperimentString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(
                        order.getItems().stream().map(item -> {
                            ReturnItem returnItem = new ReturnItem();
                            returnItem.setCount(item.getCount());
                            returnItem.setQuantity(item.getQuantityIfExistsOrCount());
                            returnItem.setItemId(item.getId());
                            returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
                            return returnItem;
                        }).collect(Collectors.toUnmodifiableList())
                ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryOptions[?(@.type=='POST')]").exists());
    }

    @Test
    @DisplayName("Проверяем, что ПВЗ опции добавляются для FBS")
    public void shouldNotGetPickupOptionsWithoutExperiment() throws Exception {
        reportConfigurer.mockActualDeliveryFromString(getReportCombinatorResponseAsJson());
        Order order = OrderProvider.getBlueOrder();
        order.setPaymentType(PaymentType.POSTPAID);
        order.setItems(OrderItemProvider.getDefaultItems());
        //FBS
        order.setFulfilment(false);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        mockMvc.perform(post("/orders/{orderId}/returns/options",
                orderId)
                .param(CLIENT_ROLE, "USER")
                .param(UID, order.getBuyer().getUid().toString())
                .param(REGION_ID, String.valueOf(100L))
                .header(X_EXPERIMENTS, Experiments.empty().toExperimentString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(
                        order.getItems().stream().map(item -> {
                            ReturnItem returnItem = new ReturnItem();
                            returnItem.setCount(item.getCount());
                            returnItem.setQuantity(item.getQuantityIfExistsOrCount());
                            returnItem.setItemId(item.getId());
                            returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
                            return returnItem;
                        }).collect(Collectors.toUnmodifiableList())
                ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryOptions[?(@.type=='PICKUP')]").exists());
    }

    @Test
    @DisplayName("Проверяем, что почтовые опции не добавляются без флага")
    public void shouldNotGetPostOptionsWithoutFlag() throws Exception {
        reportConfigurer.mockActualDeliveryFromString(getReportCombinatorResponseAsJson());
        Order order = OrderProvider.getBlueOrder();
        order.setPaymentType(PaymentType.POSTPAID);
        order.setItems(OrderItemProvider.getDefaultItems());
        order.setFulfilment(false);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        long orderId = orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        mockMvc.perform(post("/orders/{orderId}/returns/options",
                orderId)
                .param(CLIENT_ROLE, "USER")
                .param(UID, order.getBuyer().getUid().toString())
                .param(REGION_ID, String.valueOf(100L))
                .param(IS_SIMPLE_RETURN_BY_POST_AVAILABLE, String.valueOf(false))
                .header(X_EXPERIMENTS, Experiments.empty().toExperimentString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(
                        order.getItems().stream().map(item -> {
                            ReturnItem returnItem = new ReturnItem();
                            returnItem.setCount(item.getCount());
                            returnItem.setQuantity(item.getQuantityIfExistsOrCount());
                            returnItem.setItemId(item.getId());
                            returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
                            return returnItem;
                        }).collect(Collectors.toUnmodifiableList())
                ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryOptions[?(@.type=='POST')]").doesNotExist());
    }

    @Test
    @DisplayName("Проверяем, что доставку можно добавить к возврату при включенном эксперименте")
    public void shouldAddReturnDeliveryToReturnUnderExp() throws Exception {
        reportConfigurer.mockActualDeliveryFromString(getReportCombinatorResponseAsJson());
        Order order = OrderProvider.getBlueOrder();
        order.setPaymentType(PaymentType.POSTPAID);
        order.setItems(OrderItemProvider.getDefaultItems());
        order.setFulfilment(false);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        Return aReturn = ReturnProvider.generateReturn(order);
        aReturn.setBankDetails(null);
        aReturn.setId(null);
        returnHelper.initReturn(order.getId(), aReturn);
        ReturnDelivery delivery = ReturnDelivery.newReturnDelivery(DeliveryType.POST, 1005486L);
        HttpHeaders headers = new HttpHeaders();
        headers.set(X_EXPERIMENTS, Experiments.of("enable_simple_return_by_post", "1").toExperimentString());
        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/delivery",
                order.getId(), aReturn.getId())
                .content(toJson(delivery))
                .contentType("application/json")
                .param("clientRole", "USER")
                .param("uid", order.getBuyer().getUid().toString())
                .headers(headers)
        ).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Проверяем, что в доставку можно добавить тикет единого окна")
    public void shouldSetOwTicketId() throws Exception {
        reportConfigurer.mockActualDeliveryFromString(getReportCombinatorResponseAsJson());
        Order order = OrderProvider.getBlueOrder();
        order.setPaymentType(PaymentType.POSTPAID);
        order.setItems(OrderItemProvider.getDefaultItems());
        order.setFulfilment(false);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        orderCreateService.createOrder(order, ClientInfo.SYSTEM);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        Return aReturn = ReturnProvider.generateReturn(order);
        aReturn.setBankDetails(null);
        aReturn.setId(null);
        returnHelper.initReturn(order.getId(), aReturn);
        ReturnDelivery delivery = ReturnDelivery.newReturnDelivery(DeliveryType.POST, 1005486L);
        delivery.setPostTrackNeeded(true);
        returnService.addReturnDelivery(order.getId(), aReturn.getId(), delivery, ClientInfo.SYSTEM,
                Experiments.empty());
        String owTicketId = "someTicketId";
        mockMvc.perform(put("/orders/{orderId}/returns/{returnId}/owTicket/{owTicketId}",
                order.getId(), aReturn.getId(), owTicketId)
                .contentType("application/json")
                .param("clientRole", "SYSTEM"))
                .andExpect(status().isOk());
        Return returnAfter = returnService.findReturnById(aReturn.getId(), false, ClientInfo.SYSTEM);
        assertEquals(returnAfter.getDelivery().getOwTicketId(), owTicketId);
    }

    public static Stream<Arguments> parameterizedChangeReturnStatus() {
        return Stream.of(
                // ok
                new Object[]{STARTED_BY_USER, DECISION_MADE, REFUND_MONEY, ClientRole.SHOP_USER, true},
                new Object[]{STARTED_BY_USER, WAITING_FOR_DECISION, null, ClientRole.SYSTEM, true},
                new Object[]{WAITING_FOR_DECISION, DECISION_MADE, REFUND_MONEY, ClientRole.SHOP_USER, true},
                new Object[]{DECISION_MADE, REFUND_IN_PROGRESS, REFUND_MONEY, ClientRole.SYSTEM, true},
                new Object[]{DECISION_MADE, REFUNDED_BY_SHOP, OTHER_DECISION, ClientRole.SYSTEM, true},
                new Object[]{DECISION_MADE, REFUNDED_WITH_BONUSES, OTHER_DECISION, ClientRole.SYSTEM, true},
                // не проставлены решения по items
                new Object[]{STARTED_BY_USER, DECISION_MADE, null, ClientRole.SHOP_USER, false},
                new Object[]{WAITING_FOR_DECISION, DECISION_MADE, null, ClientRole.SHOP_USER, false},
                // неверная роль
                new Object[]{WAITING_FOR_DECISION, DECISION_MADE, REFUND_MONEY, ClientRole.REFEREE, false},
                new Object[]{DECISION_MADE, REFUND_IN_PROGRESS, REFUND_MONEY, ClientRole.SHOP_USER, false},
                new Object[]{DECISION_MADE, REFUNDED_BY_SHOP, OTHER_DECISION, ClientRole.SHOP_USER, false},
                new Object[]{DECISION_MADE, REFUNDED_WITH_BONUSES, OTHER_DECISION, ClientRole.SHOP_USER, false},
                // неверный переход
                new Object[]{DECISION_MADE, WAITING_FOR_DECISION, REFUND_MONEY, ClientRole.SYSTEM, false},
                new Object[]{REFUNDED, FAILED, REFUND_MONEY, ClientRole.SYSTEM, false},
                new Object[]{REFUNDED_WITH_BONUSES, FAILED, OTHER_DECISION, ClientRole.SYSTEM, false},
                new Object[]{REFUNDED_BY_SHOP, FAILED, OTHER_DECISION, ClientRole.SYSTEM, false}
        ).map(Arguments::of);
    }

    @ParameterizedTest(name = "Изменение статуса возврата из {0} в {1} под ролью {3}")
    @MethodSource("parameterizedChangeReturnStatus")
    public void testChangeReturnStatus(ReturnStatus statusBefore, ReturnStatus statusAfter,
                                       ReturnDecisionType decisionType,
                                       ClientRole role, boolean shouldPass) throws Exception {
        Pair<Order, Return> initialPair = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(),
                (ret, ord) -> {
                    ret.setStatus(statusBefore);
                    ret.setItems(List.of(createReturnItem(decisionType, "comment", ord)));
                    return ret;
                });
        Order order = initialPair.getFirst();
        Return returnBefore = initialPair.getSecond();

        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/status",
                order.getId(), returnBefore.getId())
                .contentType("application/json")
                .param(CLIENT_ROLE, role.name())
                .param(UID, "3331")
                .param(SHOP_ID, "774")
                .param(STATUS, statusAfter.name()))
                .andExpect(shouldPass ? status().isOk() : status().is4xxClientError());

        Return returnAfter = returnService.findReturnById(returnBefore.getId(), false, ClientInfo.SYSTEM);
        assertEquals(shouldPass ? statusAfter : statusBefore, returnAfter.getStatus());
    }

    @Test
    @DisplayName("Попытка перевести терминальный статус")
    public void testChangeReturnStatusTerminalStatus() throws Exception {
        Pair<Order, Return> initialPair = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(),
                (ret, ord) -> {
                    ret.setStatus(DECISION_MADE);
                    return ret;
                });
        Order order = initialPair.getFirst();
        Return returnBefore = initialPair.getSecond();
        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/status",
                order.getId(), returnBefore.getId())
                .contentType("application/json")
                .param(CLIENT_ROLE, ClientRole.SHOP.name())
                .param(UID, "3331")
                .param(SHOP_ID, "700")
                .param(STATUS, ReturnStatus.WAITING_FOR_DECISION.name()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Перевод в статуc WAITING_FOR_DECISION с правильной ролью")
    public void testChangeReturnToWaitingForDecisionOk() throws Exception {
        Pair<Order, Return> initialPair = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(),
                (ret, ord) -> {
                    ret.setStatus(STARTED_BY_USER);
                    return ret;
                });
        Order order = initialPair.getFirst();
        Return returnBefore = returnHelper.addReturnDelivery(order, initialPair.getSecond(), null);

        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/status",
                order.getId(), returnBefore.getId())
                .contentType("application/json")
                .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(UID, "3331")
                .param(STATUS, ReturnStatus.WAITING_FOR_DECISION.name()))
                .andExpect(status().isOk());

        Return returnAfter = returnService.findReturnById(returnBefore.getId(), false, ClientInfo.SYSTEM);
        assertEquals(ReturnStatus.WAITING_FOR_DECISION, returnAfter.getStatus());
    }

    @Test
    @DisplayName("Перемещение возвратной доставки по статусу")
    public void testChangeReturnDeliveryStatus() throws Exception {
        Pair<Order, Return> initialPair = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(),
                (ret, ord) -> {
                    ret.setStatus(STARTED_BY_USER);
                    return ret;
                });
        Order order = initialPair.getFirst();
        Return returnBefore = returnHelper.addReturnDelivery(order, initialPair.getSecond(), null);
        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/delivery/status",
                order.getId(), returnBefore.getId())
                .contentType("application/json")
                .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(UID, "3331")
                .param(Names.ReturnDelivery.STATUS, ReturnDeliveryStatus.SENDER_SENT.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery").isNotEmpty());
        Return returnAfter = returnService.findReturnById(returnBefore.getId(), false, ClientInfo.SYSTEM);
        assertEquals(ReturnDeliveryStatus.SENDER_SENT, returnAfter.getDelivery().getStatus());
        assertNotNull(returnAfter.getDelivery().getStatusUpdatedAt());
    }


    @Test
    @DisplayName("Недопустимое перемещение возвратной доставки по статусу")
    public void testInvalidChangeReturnDeliveryStatus() throws Exception {
        Pair<Order, Return> initialPair = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(),
                (ret, ord) -> {
                    ret.setStatus(STARTED_BY_USER);
                    return ret;
                });
        Order order = initialPair.getFirst();
        ReturnDelivery delivery = returnHelper.getDefaultReturnDelivery();
        delivery.setStatus(ReturnDeliveryStatus.DELIVERY);
        Return returnBefore = returnHelper.addReturnDelivery(order, initialPair.getSecond(), delivery);
        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/delivery/status",
                        order.getId(), returnBefore.getId())
                        .contentType("application/json")
                        .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(UID, "3331")
                        .param(Names.ReturnDelivery.STATUS, ReturnDeliveryStatus.CREATED.name()))
                .andExpect(status().is4xxClientError());
        Return returnAfter = returnService.findReturnById(returnBefore.getId(), false, ClientInfo.SYSTEM);
        assertEquals(ReturnDeliveryStatus.DELIVERY, returnAfter.getDelivery().getStatus());
        assertNotNull(returnAfter.getDelivery().getStatusUpdatedAt());
    }

    @Test
    @DisplayName("Перевод возвратной доставки в тот же статус не провоцирует ошибку")
    public void testIdempotentChangeReturnDeliveryStatus() throws Exception {
        Pair<Order, Return> initialPair = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(),
                (ret, ord) -> {
                    ret.setStatus(ReturnStatus.STARTED_BY_USER);
                    return ret;
                });
        Order order = initialPair.getFirst();
        ReturnDelivery delivery = returnHelper.getDefaultReturnDelivery();
        delivery.setStatus(ReturnDeliveryStatus.DELIVERY);
        Return returnBefore = returnHelper.addReturnDelivery(order, initialPair.getSecond(), delivery);
        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/delivery/status",
                        order.getId(), returnBefore.getId())
                        .contentType("application/json")
                        .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(UID, "3331")
                        .param(Names.ReturnDelivery.STATUS, ReturnDeliveryStatus.DELIVERY.name()))
                .andExpect(status().isOk());
        Return returnAfter = returnService.findReturnById(returnBefore.getId(), false, ClientInfo.SYSTEM);
        assertEquals(ReturnDeliveryStatus.DELIVERY, returnAfter.getDelivery().getStatus());
        assertNotNull(returnAfter.getDelivery().getStatusUpdatedAt());
    }

    @Test
    @DisplayName("Обновление решения позиции возврата")
    public void testAddReturnItemDecision() throws Exception {
        Order order = orderCreateHelper.createOrder(
                defaultBlueOrderParametersWithItems(
                        OrderItemProvider.defaultOrderItem(),
                        OrderItemProvider.defaultOrderItem(),
                        OrderItemProvider.defaultOrderItem()
                )
        );

        Return returnBefore = returnHelper.createReturnForOrder(order,
                (ret, ord) -> {
                    ret.setStatus(ReturnStatus.WAITING_FOR_DECISION);
                    ReturnItem returnItem = createReturnItem(null, null, order);
                    ret.setItems(List.of(returnItem, returnItem, returnItem));
                    return ret;
                });

        List<ReturnItem> itemsBefore = returnService
                .findReturnById(returnBefore.getId(), false, ClientInfo.SYSTEM).getItems();

        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/returnitems/decisions",
                order.getId(), returnBefore.getId())
                .contentType("application/json")
                .param(CLIENT_ROLE, ClientRole.SHOP_USER.name())
                .param(UID, "3331")
                .param(SHOP_ID, "700")
                .content(createItemDecisionsRequest(
                        itemsBefore.get(0).getId(),
                        itemsBefore.get(1).getId()
                )))
                .andExpect(status().isOk());

        Return returnAfter = returnService.findReturnById(returnBefore.getId(), false, ClientInfo.SYSTEM);
        List<ReturnItem> itemsAfter = returnAfter.getItems();

        assertThat(itemsAfter, hasSize(3));

        List<ReturnDecisionType> decisions = itemsAfter.stream()
                .map(ReturnItem::getDecisionType)
                .collect(Collectors.toList());
        assertThat(decisions, containsInAnyOrder(ReturnDecisionType.REPAIR, REFUND_MONEY, null));
    }

    @Test
    @DisplayName("Отмена возврата СД через логистический статус успешна и вызывает отмену родительского возврата")
    public void shouldCancelReturnViaLogisticStatus() throws Exception {
        Order order = orderCreateHelper.createOrder(
                defaultBlueOrderParametersWithItems(
                        OrderItemProvider.defaultOrderItem()
                )
        );

        Return returnBefore = returnHelper.createReturnForOrder(order,
                (ret, ord) -> {
                    ret.setStatus(STARTED_BY_USER);
                    return ret;
                });
        returnBefore = returnHelper.addReturnDelivery(order, returnBefore, returnHelper.getDefaultReturnDelivery());
        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/delivery/status",
                        order.getId(), returnBefore.getId())
                        .contentType("application/json")
                        .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(UID, "3331")
                        .param(Names.ReturnDelivery.STATUS, ReturnDeliveryStatus.CANCELLED.name())
                        .param(Names.Return.CANCEL_REASON, ReturnCancelReason.ITEMS_MISMATCH.name()))
                .andExpect(status().isOk());

        Return returnAfter = returnService.findReturnById(returnBefore.getId(), false, ClientInfo.SYSTEM);
        assertEquals(CANCELLED, returnAfter.getStatus());
        assertEquals(ReturnCancelReason.ITEMS_MISMATCH, returnAfter.getCancelReason());
    }

    @Test
    @DisplayName("Получение истории возврата")
    public void shouldGetReturnHistoryEvent() throws Exception {
        Order order = orderCreateHelper.createOrder(
                defaultBlueOrderParametersWithItems(
                        OrderItemProvider.defaultOrderItem(),
                        OrderItemProvider.defaultOrderItem(),
                        OrderItemProvider.defaultOrderItem()
                )
        );
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        returnHelper.initReturn(order.getId(), ReturnProvider.generateReturn(order));

        List<OrderHistoryEvent> events =
                eventService.getOrderHistoryEvents(OrderHistoryEventsRequest.builder(0L).build());
        events.sort(Comparator.comparing(OrderHistoryEvent::getId).reversed());
        OrderHistoryEvent event = events.get(0);


        mockMvc.perform(get("/returns/history")
                .contentType("application/json")
                .param(CLIENT_ROLE, ClientRole.USER.name())
                .param(UID, "3331")
                .param(EVENT_ID, event.getId().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Получение исторической сущности delivery в истории возврата")
    public void shouldGetReturnHistoricalDeliveryInHistoryEvent() throws Exception {
        Pair<Order, Return> initialPair = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(),
                (ret, ord) -> {
                    ret.setStatus(STARTED_BY_USER);
                    return ret;
                });
        Order order = initialPair.getFirst();
        Return returnBefore = returnHelper.addReturnDelivery(order, initialPair.getSecond(), null);

        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/delivery/status",
                        order.getId(), returnBefore.getId())
                        .contentType("application/json")
                        .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(UID, "3331")
                        .param(Names.ReturnDelivery.STATUS, ReturnDeliveryStatus.SENDER_SENT.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery").isNotEmpty());

        Return returnAfter = returnService.findReturnById(returnBefore.getId(), false, ClientInfo.SYSTEM);
        assertEquals(ReturnDeliveryStatus.SENDER_SENT, returnAfter.getDelivery().getStatus());
        assertNotNull(returnAfter.getDelivery().getStatusUpdatedAt());

        var events = eventService.getOrderHistoryEvents(OrderHistoryEventsRequest.builder(0L).build());
        var returnCreatedEvent = events.stream()
                .filter(e -> e.getType().equals(HistoryEventType.ORDER_RETURN_DELIVERY_UPDATED))
                .findAny()
                .orElseThrow();

        var returnDeliveryUpdatedEvent = events.stream()
                .filter(e -> e.getType().equals(HistoryEventType.ORDER_RETURN_DELIVERY_STATUS_UPDATED))
                .sorted(Comparator.comparing(OrderHistoryEvent::getId).reversed())
                .findFirst().orElseThrow();

        mockMvc.perform(get("/returns/history")
                        .contentType("application/json")
                        .param(CLIENT_ROLE, ClientRole.USER.name())
                        .param(UID, "3331")
                        .param(EVENT_ID, returnCreatedEvent.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.status").value(ReturnDeliveryStatus.CREATED.name()));

        mockMvc.perform(get("/returns/history")
                        .contentType("application/json")
                        .param(CLIENT_ROLE, ClientRole.USER.name())
                        .param(UID, "3331")
                        .param(EVENT_ID, returnDeliveryUpdatedEvent.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.status").value(ReturnDeliveryStatus.SENDER_SENT.name()));
    }

    private ReturnItem createReturnItem(ReturnDecisionType decisionType, String decisionComment, Order order) {
        ReturnItem returnItem = new ReturnItem();

        returnItem.setDecisionType(decisionType);
        returnItem.setDecisionComment(decisionComment);
        returnItem.setItemId(order.getItems().iterator().next().getId());

        return returnItem;
    }

    @Test
    @DisplayName("Попытка добавить решение по возврату с неправильной ролью")
    public void testAddReturnItemDecisionRoleFail() throws Exception {
        Pair<Order, Return> initialPair = returnHelper.createOrderAndReturn(
                defaultBlueOrderParametersWithItems(
                        OrderItemProvider.defaultOrderItem(),
                        OrderItemProvider.defaultOrderItem()
                ),
                (ret, ord) -> {
                    ret.setStatus(ReturnStatus.WAITING_FOR_DECISION);
                    ReturnItem item = createReturnItem(null, null, ord);
                    ret.setItems(List.of(item, item));
                    return ret;
                });
        Order order = initialPair.getFirst();
        Return returnBefore = initialPair.getSecond();

        List<ReturnItem> itemsBefore = returnService
                .findReturnById(returnBefore.getId(), false, ClientInfo.SYSTEM).getItems();

        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/returnitems/decisions",
                order.getId(), returnBefore.getId())
                .contentType("application/json")
                .param(CLIENT_ROLE, ClientRole.USER.name())
                .param(UID, "3331")
                .content(createItemDecisionsRequest(itemsBefore.get(0).getId(), itemsBefore.get(1).getId())))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Попытка добавить решение по возврату с очень длинным комментарием")
    public void testAddReturnItemDecisionCommentFail() throws Exception {
        Pair<Order, Return> initialPair = returnHelper.createOrderAndReturn(
                defaultBlueOrderParametersWithItems(
                        OrderItemProvider.defaultOrderItem(),
                        OrderItemProvider.defaultOrderItem()
                ),
                (ret, ord) -> {
                    ret.setStatus(ReturnStatus.WAITING_FOR_DECISION);
                    ReturnItem item = createReturnItem(null, null, ord);
                    ret.setItems(List.of(item, item));
                    return ret;
                });
        Order order = initialPair.getFirst();
        Return returnBefore = initialPair.getSecond();

        List<ReturnItem> itemsBefore = returnService
                .findReturnById(returnBefore.getId(), false, ClientInfo.SYSTEM).getItems();

        mockMvc.perform(post("/orders/{orderId}/returns/{returnId}/returnitems/decisions",
                        order.getId(), returnBefore.getId())
                        .contentType("application/json")
                        .param(CLIENT_ROLE, ClientRole.SHOP_USER.name())
                        .param(UID, "3331")
                        .param(SHOP_ID, "700")
                        .content(createItemDecisionsRequest(itemsBefore.get(0).getId(),
                                StringUtils.multiply(".", 501, ""),
                                itemsBefore.get(1).getId())))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("При двух одинаковых запросах на создание возвратов, должен вернутся один и тот же возврат")
    public void withSameReturnRequestMustResponseEqualsReturn() {
        Order order = orderCreateHelper.createOrder(defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        Return returnTemplate = ReturnProvider.generateCourierReturn(order);
        returnHelper.mockActualDelivery(order, returnTemplate.getDelivery().getDeliveryServiceId());
        Long userUid = order.getBuyer().getUid();
        //для чистоты эксперимента - в вебе item.type не указывают, он должен сам определяться на основе других полей
        returnTemplate.getItems().forEach(item -> item.setType(null));

        Return firstReturn = client.returns().initReturn(order.getId(), ClientRole.USER, userUid, returnTemplate);
        Return secondReturn = client.returns().initReturn(order.getId(), ClientRole.USER, userUid, returnTemplate);

        assertEquals(firstReturn.getId(), secondReturn.getId());
    }

    @Test
    @DisplayName("Ручка /returns/items для уникального заказа не дает возвращать товары")
    public void testNonReturnableUniqueOrder() {
        Parameters parameters = defaultBlueOrderParametersWithItems(OrderItemProvider.defaultOrderItem());
        parameters.getReportParameters().setUniqueOffer(true);

        Order order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        ReturnableItemsResponse returnableItems = client.returns()
                .getReturnableItems(RequestClientInfo.builder(ClientRole.USER)
                        .withClientId(order.getBuyer().getUid())
                        .build(),
                        BasicOrderRequest.builder(order.getId()).build());

        assertThat(returnableItems.getReturnableItems(), hasSize(0));
        assertThat(returnableItems.getNonReturnableReasonSet(),
                contains(NonReturnableReasonType.NOT_RETURNABLE_CATEGORY));
    }

    private String createItemDecisionsRequest(Long id1, Long id2) {
        return createItemDecisionsRequest(id1, "Repairing comment", id2);
    }

    private String createItemDecisionsRequest(Long id1, @Nonnull String comment1, Long id2) {
        return String.format(
                "[" +
                        "{" +
                        "\"returnItemId\": \"%s\"," +
                        "\"decisionType\": \"REPAIR\"," +
                        "\"decisionComment\": \"%s\"" +
                        "}," +
                        "{" +
                        "\"returnItemId\": \"%s\"," +
                        "\"decisionType\": \"REFUND_MONEY\"" +
                        "}" +
                        "]", id1, comment1.replace("\"", "\\\""), id2);
    }

    @Nonnull
    private Map<ResponseVariable, Object> makeParamsForBalanceResponse(Long clientId, Long
            personId) {
        Map<ResponseVariable, Object> res = new HashMap<>();
        res.put(ResponseVariable.CLIENT_ID, clientId);
        res.put(ResponseVariable.PERSON_ID, personId);
        return res;
    }

    private String getValidBankDetailsAsJson() {
        return "{" +
                "\"account\": \"00000000000000000003\"," +
                "\"corrAccount\": \"00000300\"," +
                "\"bik\": \"123456789\"," +
                "\"bank\": \"bank\"," +
                "\"bankCity\": \"bankCity\"," +
                "\"firstName\": \"lastName\"," +
                "\"lastName\": \"firstName\"," +
                "\"middleName\": \"middleName\"" +
                "}";
    }

    private String getInvalidBankDetailsAsJson() {
        return "{" +
                "\"account\": \"00000000000000000003\"," +
                "\"corrAccount\": \"00000300\"" +
                "}";
    }

    private String getReportCombinatorResponseAsJson() throws IOException {
        return IOUtils.readInputStream(Objects.requireNonNull(getClass()
                .getResourceAsStream("/json/reportResponse.json")));
    }
}

package ru.yandex.market.checkout.checkouter.pay.returns;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.qameta.allure.Epic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.pay.BankDetails;
import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.util.balance.BalanceMockHelper;
import ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceXMLRPCMethod;
import ru.yandex.market.checkout.util.balance.ResponseVariable;

import static java.lang.Boolean.FALSE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.util.GenericMockHelper.servedEvents;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceResponseVariant.IS_ACTIVE_WITH_ORDER_ID;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceResponseVariant.IS_NOT_ACTIVE;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceResponseVariant.NO_MATCH;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.checkBalanceCall;

public class ReturnBalanceTest extends AbstractPaymentTestBase {

    private static final BankDetails BANK_DETAILS = ReturnHelper.createDummyBankDetails();
    private static final ClientInfo REFEREE = new ClientInfo(ClientRole.REFEREE, 1L);
    private static final String CLIENT_ID = ResponseVariable.CLIENT_ID.defaultValue().toString();
    private static final String PASSPORT_ID = ResponseVariable.PASSPORT_ID.defaultValue().toString();

    @Autowired
    ReturnService returnService;
    @Autowired
    private ReturnHelper returnHelper;

    @BeforeEach
    public void mockBalance() {
        trustMockConfigurer.mockWholeTrust();
        returnHelper.mockSupplierInfo();
        returnHelper.mockShopInfo();
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @DisplayName("Проверяем создание возврата без компенсаций")
    public void testCreateReturnWithoutCompensation() {
        Order order = orderServiceTestHelper.createDeliveredBlueOrder();
        trustMockConfigurer.balanceHelper().resetRequests();
        Return returnRequest = prepareReturn(order, null);
        Return ret = returnService.initReturn(order.getId(), REFEREE, returnRequest, Experiments.empty());
        returnService.resumeReturn(order.getId(), REFEREE, ret.getId(), ret, true);
        Iterator<ServeEvent> callIterator = servedEvents(trustMockConfigurer.balanceMock()).iterator();
        assertFalse(callIterator.hasNext());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @DisplayName("Проверяем создание возврата для постоплатного заказа")
    public void testCreateReturnForPostpaidOrder() throws Exception {
        Order order = orderServiceTestHelper.createDeliveredBluePostPaidOrder();
        trustMockConfigurer.balanceHelper().resetRequests();
        Return returnRequest = prepareReturn(order, null);
        returnRequest.setBankDetails(BANK_DETAILS);
        Return ret = returnService.initReturn(order.getId(), REFEREE, returnRequest, Experiments.empty());
        returnService.resumeReturn(order.getId(), REFEREE, ret.getId(), ret, true);
        Iterator<ServeEvent> callIterator = servedEvents(trustMockConfigurer.balanceMock()).iterator();
        BalanceXMLRPCMethod[] methods = {
                BalanceXMLRPCMethod.FindClient,
                BalanceXMLRPCMethod.CreateUserClientAssociation,
                BalanceXMLRPCMethod.GetClientContracts,
                BalanceXMLRPCMethod.CreatePerson,
                BalanceXMLRPCMethod.CreateOffer
        };
        Map<String, String> variablesForExpectedValues = new HashMap<>();
        variablesForExpectedValues.put("external_id", order.getId().toString());
        variablesForExpectedValues.put("client_id", CLIENT_ID);
        variablesForExpectedValues.put("passport_id", PASSPORT_ID);
        checkBalanceCalls(callIterator, methods, variablesForExpectedValues);
        assertFalse(callIterator.hasNext());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @DisplayName("Проверяем создание возврата для постоплатного заказа для нового клиента")
    public void testCreateReturnForPostpaidOrderForNewClient() throws Exception {
        Order order = orderServiceTestHelper.createDeliveredBluePostPaidOrder();
        trustMockConfigurer.balanceHelper().resetRequests();
        Return returnRequest = prepareReturn(order, null);
        returnRequest.setBankDetails(BANK_DETAILS);
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(BalanceXMLRPCMethod.FindClient, NO_MATCH);
        Return ret = returnService.initReturn(order.getId(), REFEREE, returnRequest, Experiments.empty());
        returnService.resumeReturn(order.getId(), REFEREE, ret.getId(), ret, true);
        Iterator<ServeEvent> callIterator = servedEvents(trustMockConfigurer.balanceMock()).iterator();
        BalanceXMLRPCMethod[] methods = {
                BalanceXMLRPCMethod.FindClient,
                BalanceXMLRPCMethod.CreateClient,
                BalanceXMLRPCMethod.CreateUserClientAssociation,
                BalanceXMLRPCMethod.GetClientContracts,
                BalanceXMLRPCMethod.CreatePerson,
                BalanceXMLRPCMethod.CreateOffer
        };
        Map<String, String> variablesForExpectedValues = new HashMap<>();
        variablesForExpectedValues.put("external_id", order.getId().toString());
        variablesForExpectedValues.put("client_id", CLIENT_ID);
        variablesForExpectedValues.put("passport_id", PASSPORT_ID);
        checkBalanceCalls(callIterator, methods, variablesForExpectedValues);
        assertFalse(callIterator.hasNext());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @DisplayName("Проверяем создание возврата для предоплатного заказа с компесацией")
    public void testCreateReturnForPrepaidOrderWithCompensation() throws Exception {
        Order order = orderServiceTestHelper.createDeliveredBlueOrder();
        trustMockConfigurer.balanceHelper().resetRequests();
        Return returnRequest = prepareReturn(order, 10L);
        Return ret = returnService.initReturn(order.getId(), REFEREE, returnRequest, Experiments.empty());
        returnService.resumeReturn(order.getId(), REFEREE, ret.getId(), ret, true);
        Iterator<ServeEvent> callIterator = servedEvents(trustMockConfigurer.balanceMock()).iterator();
        BalanceXMLRPCMethod[] methods = {
                BalanceXMLRPCMethod.FindClient,
                BalanceXMLRPCMethod.CreateUserClientAssociation,
                BalanceXMLRPCMethod.GetClientContracts,
                BalanceXMLRPCMethod.CreatePerson,
                BalanceXMLRPCMethod.CreateOffer
        };
        Map<String, String> variablesForExpectedValues = new HashMap<>();
        variablesForExpectedValues.put("external_id", order.getId().toString());
        variablesForExpectedValues.put("client_id", CLIENT_ID);
        variablesForExpectedValues.put("passport_id", PASSPORT_ID);
        checkBalanceCalls(callIterator, methods, variablesForExpectedValues);
        assertFalse(callIterator.hasNext());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @DisplayName("Проверяем создание возврата с компенсацией для нового клиента")
    public void testCreateReturnForOrderWithCompensationForNewClient() throws Exception {
        Order order = orderServiceTestHelper.createDeliveredBlueOrder();
        trustMockConfigurer.balanceHelper().resetRequests();
        Return returnRequest = prepareReturn(order, 10L);
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(BalanceXMLRPCMethod.FindClient, NO_MATCH);
        Return ret = returnService.initReturn(order.getId(), REFEREE, returnRequest, Experiments.empty());
        returnService.resumeReturn(order.getId(), REFEREE, ret.getId(), ret, true);
        Iterator<ServeEvent> callIterator = servedEvents(trustMockConfigurer.balanceMock()).iterator();
        BalanceXMLRPCMethod[] methods = {
                BalanceXMLRPCMethod.FindClient,
                BalanceXMLRPCMethod.CreateClient,
                BalanceXMLRPCMethod.CreateUserClientAssociation,
                BalanceXMLRPCMethod.GetClientContracts,
                BalanceXMLRPCMethod.CreatePerson,
                BalanceXMLRPCMethod.CreateOffer
        };
        Map<String, String> variablesForExpectedValues = new HashMap<>();
        variablesForExpectedValues.put("external_id", order.getId().toString());
        variablesForExpectedValues.put("client_id", CLIENT_ID);
        variablesForExpectedValues.put("passport_id", PASSPORT_ID);
        checkBalanceCalls(callIterator, methods, variablesForExpectedValues);
        assertFalse(callIterator.hasNext());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @DisplayName("Проверяем создание возврата заказа с компенсацией и договором")
    public void testCreateReturnForWithCompensationAndContract() throws Exception {
        Order order = orderServiceTestHelper.createDeliveredBlueOrder();
        trustMockConfigurer.balanceHelper().resetRequests();
        Return returnRequest = prepareReturn(order, 10L);
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(BalanceXMLRPCMethod.GetClientContracts,
                IS_NOT_ACTIVE);
        Return ret = returnService.initReturn(order.getId(), REFEREE, returnRequest, Experiments.empty());
        returnService.resumeReturn(order.getId(), REFEREE, ret.getId(), ret, true);
        Iterator<ServeEvent> callIterator = servedEvents(trustMockConfigurer.balanceMock()).iterator();
        BalanceXMLRPCMethod[] methods = {
                BalanceXMLRPCMethod.FindClient,
                BalanceXMLRPCMethod.CreateUserClientAssociation,
                BalanceXMLRPCMethod.GetClientContracts,
                BalanceXMLRPCMethod.CreatePerson,
                BalanceXMLRPCMethod.CreateOffer
        };
        Map<String, String> variablesForExpectedValues = new HashMap<>();
        variablesForExpectedValues.put("external_id", order.getId().toString());
        variablesForExpectedValues.put("client_id", CLIENT_ID);
        variablesForExpectedValues.put("passport_id", PASSPORT_ID);
        checkBalanceCalls(callIterator, methods, variablesForExpectedValues);
        assertFalse(callIterator.hasNext());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @DisplayName("Проверяем создание возврата заказа с компенсацией, айди заказа и договором")
    public void testCreateReturnForWithCompensationOrderIdAndContract() throws Exception {
        Order order = orderServiceTestHelper.createDeliveredBlueOrder();
        trustMockConfigurer.balanceHelper().resetRequests();
        Return returnRequest = prepareReturn(order, 10L);
        BankDetails bd = returnRequest.getBankDetails();
        returnRequest.setBankDetails(null);
        Map<ResponseVariable, Object> variables = new HashMap<>();
        variables.put(ResponseVariable.ORDER_ID, order.getId());
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(BalanceXMLRPCMethod.GetClientContracts,
                IS_ACTIVE_WITH_ORDER_ID, variables);
        Return ret = returnService.initReturn(order.getId(), REFEREE, returnRequest, Experiments.empty());
        ret.setBankDetails(bd);
        returnService.resumeReturn(order.getId(), REFEREE, ret.getId(), ret, true);
        Iterator<ServeEvent> callIterator = servedEvents(trustMockConfigurer.balanceMock()).iterator();
        BalanceXMLRPCMethod[] methods = {
                BalanceXMLRPCMethod.FindClient,
                BalanceXMLRPCMethod.CreateUserClientAssociation,
                BalanceXMLRPCMethod.GetClientContracts,
                BalanceXMLRPCMethod.CreatePerson,
        };
        Map<String, String> variablesForExpectedValues = new HashMap<>();
        variablesForExpectedValues.put("external_id", order.getId().toString());
        variablesForExpectedValues.put("client_id", CLIENT_ID);
        variablesForExpectedValues.put("passport_id", PASSPORT_ID);
        checkBalanceCalls(callIterator, methods, variablesForExpectedValues);
        assertFalse(callIterator.hasNext());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @DisplayName("Проверяем изменение банковских данных пользователя")
    public void updateBankDetailsTest() throws Exception {
        Order order = orderServiceTestHelper.createDeliveredBluePostPaidOrder();
        trustMockConfigurer.balanceHelper().resetRequests();
        Return returnRequest = prepareReturn(order, null);
        returnRequest.setBankDetails(BANK_DETAILS);
        Return ret = returnService.initReturn(order.getId(), REFEREE, returnRequest, Experiments.empty());
        returnService.resumeReturn(order.getId(), REFEREE, ret.getId(), ret, true);
        returnService.updateBankDetails(ret.getOrderId(), ret.getId(), BANK_DETAILS, REFEREE);
        Iterator<ServeEvent> callIterator = servedEvents(trustMockConfigurer.balanceMock()).iterator();
        //Обновление банковских данных пользователя по сути является вызовом
        //метода создания/обновления нового пользователя (CreatePerson)
        BalanceXMLRPCMethod[] methods = {
                BalanceXMLRPCMethod.FindClient,
                BalanceXMLRPCMethod.CreateUserClientAssociation,
                BalanceXMLRPCMethod.GetClientContracts,
                BalanceXMLRPCMethod.CreatePerson,
                BalanceXMLRPCMethod.CreateOffer,
                BalanceXMLRPCMethod.CreatePerson //Собственно обновление
        };
        Map<String, String> variablesForExpectedValues = new HashMap<>();
        variablesForExpectedValues.put("external_id", order.getId().toString());
        variablesForExpectedValues.put("client_id", CLIENT_ID);
        variablesForExpectedValues.put("passport_id", PASSPORT_ID);
        checkBalanceCalls(callIterator, methods, variablesForExpectedValues);
        assertFalse(callIterator.hasNext());
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @DisplayName("Проверяем, что user_email записывается в lowercase")
    public void userEmailCaseTest() {
        String emailWithUppercaseChars = "emailWithUppercaseChars@m.ru";
        Order order = orderServiceTestHelper.createDeliveredBlueOrder();
        trustMockConfigurer.balanceHelper().resetRequests();
        Return returnRequest = prepareReturn(order, null);
        returnRequest.setUserEmail(emailWithUppercaseChars);
        Return ret = returnService.initReturn(order.getId(), REFEREE, returnRequest, Experiments.empty());
        ret = returnService.resumeReturn(order.getId(), REFEREE, ret.getId(), ret, true);
        assertEquals(ret.getUserEmail(), emailWithUppercaseChars.toLowerCase());
    }

    @Test
    public void resumeReturnWithNullDeliveryId() {
        Order order = orderServiceTestHelper.createDeliveredBlueOrder();
        trustMockConfigurer.balanceHelper().resetRequests();
        Return returnRequest = prepareReturn(order, null);
        Return ret = returnService.initReturn(order.getId(), REFEREE, returnRequest, Experiments.empty());
        ret.getItems().add(getDeliveryReturnItem());
        ret = returnService.resumeReturn(order.getId(), REFEREE, ret.getId(), ret, true);
        assertTrue(ret.getItems().stream().allMatch(returnItem ->
                returnItem.getItemId() != null || returnItem.getDeliveryServiceId() != null));
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @DisplayName("Проверяем, что в balance уходит подмененная информация от Personal")
    public void personalBankReturnUserInfoTest() {
        personalMockConfigurer.mockV1MultiTypesRetrieve();

        Order order = orderServiceTestHelper.createDeliveredBluePostPaidOrder();
        trustMockConfigurer.balanceHelper().resetRequests();
        Return returnRequest = prepareReturn(order, null);

        BankDetails bankDetails = ReturnHelper.createDummyBankDetails();
        bankDetails.setPersonalFullNameId("jknsvj4-kjefckj3234b-kbb4");

        returnRequest.setBankDetails(bankDetails);

        Return ret = returnService.initReturn(order.getId(), REFEREE, returnRequest, Experiments.empty());
        returnService.resumeReturn(order.getId(), REFEREE, ret.getId(), ret, true);

        List<ServeEvent> serveEvents = servedEvents(trustMockConfigurer.balanceMock());

        String createPerson = serveEvents.stream()
                .map(it -> it.getRequest().getBodyAsString())
                .filter(it -> it.contains("CreatePerson"))
                .findFirst().get();

        // подмена от Personal
        assertThat(createPerson, containsString("fName1"));
        assertThat(createPerson, containsString("sName1"));
        assertThat(createPerson, containsString("tName1"));

        assertNotEquals(returnRequest.getBankDetails().getFirstName(), "fName1");
    }

    private ReturnItem getDeliveryReturnItem() {
        ReturnItem returnItem = new ReturnItem();
        returnItem.setCount(1);
        returnItem.setQuantity(BigDecimal.ONE);
        returnItem.setDeliveryService(true);
        returnItem.setItemId(null);
        returnItem.setSupplierCompensation(null);
        returnItem.setDeliveryServiceId(null);
        return returnItem;
    }

    private void checkBalanceCalls(Iterator<ServeEvent> callIterator, BalanceXMLRPCMethod[] methods,
                                   Map<String, String> variablesForExpectedValues) throws Exception {
        for (BalanceXMLRPCMethod method : methods) {
            assertTrue(callIterator.hasNext());
            LoggedRequest request = callIterator.next().getRequest();
            assertThat(BalanceMockHelper.extractBalanceRequestMethodName(request), equalTo(method.fullName()));
            checkBalanceCall(method, request, variablesForExpectedValues);
        }
    }

    private void convertItems(List<OrderItem> items, List<ReturnItem> returnItems) {
        for (OrderItem item : items) {
            ReturnItem returnItem = new ReturnItem();
            returnItem.setCount(item.getCount());
            returnItem.setQuantity(item.getQuantityIfExistsOrCount());
            returnItem.setDeliveryService(FALSE);
            returnItem.setItemId(item.getId());
            returnItem.setSupplierCompensation(null);
            returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
            returnItems.add(returnItem);
        }
    }

    private Return prepareReturn(Order order, Long compensation) {
        Return returnRequest = new Return();
        List<OrderItem> items = new ArrayList<>(order.getItems());
        List<ReturnItem> returnItems = new ArrayList<>();
        convertItems(items, returnItems);
        returnRequest.setComment("test");
        returnRequest.setOrderId(order.getId());
        returnRequest.setItems(returnItems);
        if (compensation != null) {
            returnRequest.setUserCompensationSum(BigDecimal.valueOf(compensation));
            returnRequest.getItems().get(0).setSupplierCompensation(BigDecimal.valueOf(compensation));
            returnRequest.setBankDetails(BANK_DETAILS);

        } else {
            returnRequest.setUserCompensationSum(null);
            returnRequest.setBankDetails(null);
        }
        returnHelper.mockActualDelivery(returnRequest, order);
        return returnRequest;
    }
}

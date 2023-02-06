package ru.yandex.market.checkout.checkouter.returns;

import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.BankDetails;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class ReturnClientBalanceTest extends AbstractReturnTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(ReturnClientBalanceTest.class);

    private Order order;

    @BeforeEach
    public void createOrder() {
        Parameters params = defaultBlueOrderParameters();
        params.getOrder().getItems().forEach(item -> item.setCount(10));
        order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Создание клиента и покупателя в балансе при предоставлении банковских данных")
    public void checkBalanceCallsOnReturnInit() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        printAllBalanceCalls();
        checkBalanceCalls(returnResp, request.getBankDetails(), false);
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.RETURN_CREATE)
    @DisplayName("Отсутствие создания клиента и покупателя в балансе без банковских данных")
    public void checkNoBalanceCallsOnReturnInitWithOutBankDetails() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        request.setBankDetails(null);
        client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        printAllBalanceCalls();
        List<String> createClientRequests = getRequestsByBalanceMethod("FindClient");
        assertThat(createClientRequests, empty());
        List<String> createPartnerRequests = getRequestsByBalanceMethod("CreatePerson");
        assertThat(createPartnerRequests, empty());
        List<String> createContractsRequests = getRequestsByBalanceMethod("CreateOffer");
        assertThat(createContractsRequests, empty());

    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.RETURN_RESUME)
    @DisplayName("Создание клиента и покупателя в балансе при передаче банковских данных в resume")
    public void checkBalanceCallsOnReturnResume() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        request.setBankDetails(null);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        printAllBalanceCalls();
        request = ReturnHelper.copy(returnResp);
        addBankDetails(request);
        trustMockConfigurer.balanceMock().resetRequests();
        returnResp = client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, 3331L, request);
        printAllBalanceCalls();
        checkBalanceCalls(returnResp, request.getBankDetails(), false);
    }

    @Test
    @Epic(ru.yandex.market.checkout.allure.Epics.RETURN)
    @Story(Stories.RETURN_RESUME)
    @DisplayName("Пересоздание плательщика (переданы новые банковские данные)")
    public void checkBalanceCallsOnReturnResumeRecreatePerson() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        printAllBalanceCalls();
        request = ReturnHelper.copy(returnResp);
        request.setBankDetails(NEW_BANK_DETAILS);
        trustMockConfigurer.balanceMock().resetRequests();
        returnResp = client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, 3331L, request);
        printAllBalanceCalls();
        checkBalanceCalls(returnResp, NEW_BANK_DETAILS);
    }

    @Test
    public void checkSavingContractId() {
        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        Return returnResp = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request);
        printAllBalanceCalls();
        request = ReturnHelper.copy(returnResp);
        request.setCompensationContractId(null);
        request.setBankDetails(NEW_BANK_DETAILS);
        trustMockConfigurer.balanceMock().resetRequests();
        returnResp = client.returns().resumeReturn(order.getId(),
                returnResp.getId(), ClientRole.REFEREE, 3331L, request);
        printAllBalanceCalls();
        checkBalanceCalls(returnResp, NEW_BANK_DETAILS);
        Return returnInDb = returnService.findReturnById(returnResp.getId(), false, ClientInfo.SYSTEM);
        assertNotNull(returnInDb.getCompensationContractId());
    }

    private void checkBalanceCalls(Return returnResp, BankDetails bankDetails) {
        checkBalanceCalls(returnResp, bankDetails, true);
    }

    private void checkBalanceCalls(Return returnResp, BankDetails bankDetails, boolean withoutCreatingContract) {
        List<String> createPartnerRequests = getRequestsByBalanceMethod("CreatePerson");
        assertThat(createPartnerRequests, hasSize(1));
        assertThat(createPartnerRequests.get(0), allOf(
                containsString(returnResp.getCompensationClientId().toString()),
                containsString(bankDetails.getAccount()),
                containsString(bankDetails.getBank()),
                containsString(bankDetails.getBankCity()),
                containsString(bankDetails.getBik()),
                containsString(bankDetails.getCorraccount()),
                containsString(bankDetails.getFirstName()),
                containsString(bankDetails.getLastName()),
                containsString(bankDetails.getMiddleName())
        ));
        List<String> createContractsRequests = getRequestsByBalanceMethod("CreateOffer");
        if (withoutCreatingContract) {
            assertThat(createContractsRequests, empty());
        } else {
            assertThat(createContractsRequests, hasSize(1));
            assertThat(createContractsRequests.get(0), containsString(returnResp.getCompensationPersonId().toString()));
        }
    }

    private void printAllBalanceCalls() {
        LOG.info("Balance calls:");
        trustMockConfigurer.balanceMock().getAllServeEvents().forEach(e -> LOG.info(e.getRequest().getBodyAsString()));
    }

    private List<String> getRequestsByBalanceMethod(String methodName) {
        return trustMockConfigurer.balanceMock().getAllServeEvents()
                .stream()
                .map(ServeEvent::getRequest)
                .map(LoggedRequest::getBodyAsString)
                .filter(b -> b.contains(methodName)).collect(toList());
    }

}

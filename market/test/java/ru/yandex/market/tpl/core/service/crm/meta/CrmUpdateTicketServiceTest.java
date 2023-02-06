package ru.yandex.market.tpl.core.service.crm.meta;

import java.time.LocalDate;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserService;
import ru.yandex.market.tpl.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.external.crm.model.system.CrmField;
import ru.yandex.market.tpl.core.service.crm.CrmUpdateTicketService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


@RequiredArgsConstructor
public class CrmUpdateTicketServiceTest extends TplAbstractTest {
    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper testUserHelper;
    private final CrmUpdateTicketService crmUpdateTicketService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final DsRepository dsRepository;
    private final UserService userService;
    private final PickupPointService pickupPointService;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    public Order order;
    public User user;
    public UserShift userShift;
    public PickupPoint pickupPoint;

    @BeforeEach
    public void init() {
        var now = LocalDate.now();
        order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder().build()
        );

        user = testUserHelper.findOrCreateUser(-123L);
        testUserHelper.findOrCreateSuperCompany(69L, "login");
        userShift = testUserHelper.createOpenedShift(user, order, now);
        String unknownCode = "unknown";
        pickupPoint = pickupPointService.getOrCreate(unknownCode, -111L);

        userService.updateUserSmartphone(UserCommand.UpdateUserSmartphoneCommand.builder()
                .courierAppVersion("app")
                .modelSmartphone("model")
                .osSmartphone("os")
                .userId(user.getId())
                .build());
    }

    @Test
    @DisplayName("Тест для проверки, что все нужные данные отдаем при приемки заказов")
    public void testStatePickupOrders() {
        Map<CrmField.Attribute, Object> responseToCrm =
                crmUpdateTicketService.createAttributeValueMapForActiveUserShift(user.getId());

        assertThat(responseToCrm.size()).isEqualTo(9);
    }

    @Test
    @DisplayName("Тест для проверки, какие данные отдаем при доставке заказов")
    public void testStateDeliveryOrders() {
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        Map<CrmField.Attribute, Object> responseToCrm =
                crmUpdateTicketService.createAttributeValueMapForActiveUserShift(user.getId());

        assertThat(responseToCrm.size()).isEqualTo(13);
    }

    @Test
    @DisplayName("Тест для проверки, какие данные отдаем при инвентаризации постамата с выключенным флагом")
    public void testStateLockerInventoryWhenSenInitialMessageToCrmFlagEnabled() {
        when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.SEND_INITIAL_MESSAGE_TO_CRM))
                .thenReturn(true);
        testUserHelper.addLockerInventoryTask(user, userShift, pickupPoint);
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        Map<CrmField.Attribute, Object> responseToCrm =
                crmUpdateTicketService.createAttributeValueMapForActiveUserShift(user.getId());

        assertThat(responseToCrm).containsKey(CrmField.Attribute.INITIAL_MESSAGE);
    }

    @Test
    @DisplayName("Тест для проверки, какие данные отдаем при инвентаризации постамата с включенным флагом")
    public void testStateLockerInventoryWhenSendInitialMessageToCrmFlagDisabled() {
        when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.SEND_INITIAL_MESSAGE_TO_CRM))
                .thenReturn(false);
        testUserHelper.addLockerInventoryTask(user, userShift, pickupPoint);
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        Map<CrmField.Attribute, Object> responseToCrm =
                crmUpdateTicketService.createAttributeValueMapForActiveUserShift(user.getId());

        assertThat(responseToCrm).doesNotContainKey(CrmField.Attribute.INITIAL_MESSAGE);
    }

    @Test
    @DisplayName("Тест для проверки, что отадем только известные для crm данные")
    public void testStateDeliveryOrders_WithKnownAttributes() {
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        Map<CrmField.Attribute, Object> responseToCrm =
                crmUpdateTicketService.createAttributeValueMapForActiveUserShiftWithKnownAttributes(user.getId());

        assertThat(responseToCrm.size()).isEqualTo(12);
        assertThat(responseToCrm.get(CrmField.Attribute.CLIENT_PHONE)).isEqualTo(user.getPhone());
    }

    @Test
    @DisplayName("Тест для проверки, какие данные отдаем при возврате заказов")
    @Transactional
    public void testStateReturnOrders() {
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        testUserHelper.finishAllDeliveryTasks(userShift);

        Map<CrmField.Attribute, Object> responseToCrm =
                crmUpdateTicketService.createAttributeValueMapForActiveUserShift(user.getId());

        assertThat(responseToCrm.size()).isEqualTo(9);
    }

    @Test
    @DisplayName("Тест, что кладем в очередь dbQueue задание, чтобы сходить в crm")
    public void appendToDbQueue() {
        crmUpdateTicketService.asyncUpdateTicketEntity(user.getUid(), "ticketGid");

        assertThat(dbQueueTestUtil.getQueue(QueueType.PUT_USER_META_INFO_TO_CRM)).hasSize(1);
    }

}

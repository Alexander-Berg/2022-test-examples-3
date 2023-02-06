package ru.yandex.travel.orders.services.suburban;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.SneakyThrows;
import org.javamoney.moneta.Money;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.AuthorizedUser;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.FiscalItemType;
import ru.yandex.travel.orders.entities.FiscalReceipt;
import ru.yandex.travel.orders.entities.GenericOrder;
import ru.yandex.travel.orders.entities.SuburbanOrderItem;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.entities.notifications.Attachment;
import ru.yandex.travel.orders.entities.notifications.AttachmentProviderType;
import ru.yandex.travel.orders.entities.notifications.EmailChannelInfo;
import ru.yandex.travel.orders.entities.notifications.FiscalReceiptAttachmentProviderData;
import ru.yandex.travel.orders.entities.notifications.Notification;
import ru.yandex.travel.orders.entities.notifications.suburban.SuburbanConfirmedMailSenderArgs;
import ru.yandex.travel.orders.entities.notifications.suburban.SuburbanCouponAttachmentProviderData;
import ru.yandex.travel.orders.factories.SuburbanOrderItemEnvProviderFactory;
import ru.yandex.travel.orders.repository.FiscalReceiptRepository;
import ru.yandex.travel.orders.repository.NotificationRepository;
import ru.yandex.travel.orders.repository.OrderItemRepository;
import ru.yandex.travel.orders.repository.OrderRepository;
import ru.yandex.travel.orders.repository.TrustInvoiceRepository;
import ru.yandex.travel.orders.services.orders.OrderCompatibilityUtils;
import ru.yandex.travel.orders.services.payments.InvoicePaymentFlags;
import ru.yandex.travel.orders.workflow.invoice.proto.ETrustInvoiceState;
import ru.yandex.travel.orders.workflow.order.generic.proto.EOrderState;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflows.orderitem.suburban.SuburbanProperties;
import ru.yandex.travel.suburban.model.MovistaReservation;
import ru.yandex.travel.suburban.model.SuburbanReservation;
import ru.yandex.travel.suburban.model.WicketDevice;
import ru.yandex.travel.suburban.model.WicketType;
import ru.yandex.travel.suburban.partners.SuburbanCarrier;
import ru.yandex.travel.suburban.partners.SuburbanProvider;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.entities.WorkflowEntity;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.entities.WellKnownWorkflow.GENERIC_ERROR_SUPERVISOR;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class TestSuburbanNotificationHelper {
    @Autowired
    public WorkflowRepository workflowRepository;
    @Autowired
    public NotificationRepository notificationRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private TrustInvoiceRepository trustInvoiceRepository;
    @Autowired
    private FiscalReceiptRepository fiscalReceiptRepository;

    public Workflow createWorkflowForEntity(WorkflowEntity<?> workflowEntity) {
        Workflow workflow = Workflow.createWorkflowForEntity(workflowEntity);
        return workflowRepository.saveAndFlush(workflow);
    }

    @Test
    @Transactional
    public void testCreateWorkflowForSuburbanOrderConfirmedEmail() {
        runCreateWorkflowForSuburbanOrderConfirmedEmail(createPayload());
    }

    private SuburbanReservation createPayload() {
        return SuburbanReservation.builder()
                .provider(SuburbanProvider.MOVISTA)
                .carrier(SuburbanCarrier.CPPK)
                .price(Money.of(72, ProtoCurrencyUnit.RUB))
                .stationFrom(SuburbanReservation.Station.builder().titleDefault("Одинцово").build())
                .stationTo(SuburbanReservation.Station.builder().titleDefault("Москва (Белорусский вокзал)").build())
                .movistaReservation(MovistaReservation.builder()
                        .date(LocalDate.of(LocalDate.now().getYear(), 10, 13))
                        .wicket(WicketDevice.builder().type(WicketType.VALIDATOR).deviceType("any").build())
                        .ticketNumber(1234)
                        .ticketBody("abcd")
                        .build())
                .build();
    }

    public GenericOrder createSuburbanOrder(EOrderState orderState, EOrderItemState orderItemState,
                                            SuburbanReservation payload) {
        var order = new GenericOrder();
        order.setId(UUID.randomUUID());
        order.setPrettyId("pretty");
        order.setState(orderState);
        order.setWorkflow(createWorkflowForEntity(order));
        order.setCurrency(ProtoCurrencyUnit.RUB);
        order = orderRepository.saveAndFlush(order);

        var orderItem = new SuburbanOrderItem();
        orderItem.setId(UUID.randomUUID());
        orderItem.setState(orderItemState);
        orderItem.setWorkflow(createWorkflowForEntity(orderItem));

        orderItem.setReservation(payload);
        orderItemRepository.saveAndFlush(orderItem);

        order.addOrderItem(orderItem);
        order = orderRepository.saveAndFlush(order);

        var owner = AuthorizedUser.createGuest(order.getId(), "test-key", "test-yuid",
                AuthorizedUser.OrderUserRole.OWNER);
        var fiscalItem = FiscalItem.builder()
                .orderItem(orderItem)
                .moneyAmount(Money.of(72, ProtoCurrencyUnit.RUB))
                .type(FiscalItemType.SUBURBAN_MOVISTA_TICKET).build();
        TrustInvoice invoice = TrustInvoice.createInvoice(
                order, owner, List.of(fiscalItem), InvoicePaymentFlags.builder().build());
        invoice.setState(ETrustInvoiceState.IS_HOLD);
        invoice.initAcquireFiscalReceipt();
        trustInvoiceRepository.saveAndFlush(invoice);

        FiscalReceipt receipt = invoice.getFiscalReceipts().get(0);
        receipt.setReceiptUrl("https://somefiscal.receipt");
        fiscalReceiptRepository.saveAndFlush(receipt);

        order = orderRepository.saveAndFlush(order);

        assertThat(OrderCompatibilityUtils.isSuburbanOrder(order)).isTrue();

        return order;
    }

    @SneakyThrows
    private void runCreateWorkflowForSuburbanOrderConfirmedEmail(SuburbanReservation payload) {
        var props = SuburbanProperties.builder()
                .providers(SuburbanProperties.Providers.builder()
                        .movista(SuburbanProperties.MovistaProps.builder()
                                .common(SuburbanProperties.ProviderProps.builder()
                                        .mail(SuburbanProperties.MailProperties.builder()
                                                .orderConfirmedCampaign("campaign42")
                                                .preparingAttachmentsTimeout(Duration.ofSeconds(33))
                                                .build()).build()).build()).build()).build();

        GenericOrder order = createSuburbanOrder(EOrderState.OS_CONFIRMED, EOrderItemState.IS_CONFIRMED, payload);
        order.setEmail("mail@somemail.net");

        var suburbanNotificationHelper = new SuburbanNotificationHelper(
                SuburbanOrderItemEnvProviderFactory.createEnvProvider(props),
                workflowRepository, notificationRepository);

        UUID emailWorkflowId = suburbanNotificationHelper.createWorkflowForSuburbanOrderConfirmedEmail(order);

        List<Notification> emails = notificationRepository.findAllByOrderId(order.getId());
        assertThat(emails.size()).isEqualTo(1);

        Notification email = emails.get(0);
        assertThat(email.getWorkflow().getId()).isEqualTo(emailWorkflowId);
        assertThat(email.getWorkflow().getSupervisorId()).isEqualTo(GENERIC_ERROR_SUPERVISOR.getUuid());
        workflowRepository.getOne(email.getWorkflow().getId());  // check workflow is saved

        EmailChannelInfo emailInfo = (EmailChannelInfo) email.getChannelInfo();
        assertThat(emailInfo.getCampaign()).isEqualTo("campaign42");
        assertThat(emailInfo.getTarget()).isEqualTo("mail@somemail.net");

        ObjectMapper mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        TreeNode arguments = emailInfo.getArguments();
        SuburbanConfirmedMailSenderArgs emailArgs = mapper.treeToValue(
                arguments, SuburbanConfirmedMailSenderArgs.class);
        assertThat(emailArgs).isEqualTo(SuburbanConfirmedMailSenderArgs.builder()
                .ticketNumber("1234")
                .orderPrice(BigDecimal.valueOf(72))
                .travelDate("13 октября")
                .stationFromTitle("Одинцово")
                .stationToTitle("Москва (Белорусский вокзал)")
                .validatorOnStation(true)
                .build());

        List<Attachment> attachments = email.getAttachments();
        assertThat(attachments.size()).isEqualTo(2);

        Attachment fiscalReceipt = attachments.get(0);
        workflowRepository.getOne(fiscalReceipt.getWorkflow().getId());  // check workflow is saved
        assertThat(fiscalReceipt.getProvider()).isEqualTo(AttachmentProviderType.FISCAL_RECEIPT);
        assertThat(fiscalReceipt.getFilename()).isEqualTo("Check_pretty.pdf");
        assertThat(fiscalReceipt.getMimeType()).isEqualTo("application/pdf");
        FiscalReceiptAttachmentProviderData providerData =
                (FiscalReceiptAttachmentProviderData) fiscalReceipt.getProviderData();
        assertThat(providerData.getFiscalReceiptId()).isEqualTo(
                order.getInvoices().get(0).getFiscalReceipts().get(0).getId());

        Attachment controlCoupon = attachments.get(1);
        workflowRepository.getOne(controlCoupon.getWorkflow().getId());  // check workflow is saved
        assertThat(controlCoupon.getProvider()).isEqualTo(AttachmentProviderType.SUBURBAN_COUPON);
        assertThat(controlCoupon.getFilename()).isEqualTo("Контрольный купон.pdf");
        assertThat(controlCoupon.getMimeType()).isEqualTo("application/pdf");
        SuburbanCouponAttachmentProviderData couponData =
                (SuburbanCouponAttachmentProviderData) controlCoupon.getProviderData();

        SuburbanOrderItem orderItem = OrderCompatibilityUtils.getSuburbanOrderItem(order);
        assertThat(couponData.getOrderItemId()).isEqualTo(orderItem.getId());
    }
}

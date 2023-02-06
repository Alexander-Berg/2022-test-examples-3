package ru.yandex.market.crm.operatorwindow;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.mail.InMailMessageHolder;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService;
import ru.yandex.market.jmf.module.mail.test.impl.MailTestUtils;
import ru.yandex.market.jmf.module.ticket.Channel;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.ocrm.module.order.TicketFirstLine;
import ru.yandex.market.ocrm.module.order.domain.DeliveryService;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

import static org.junit.jupiter.params.provider.Arguments.arguments;

@DirtiesContext
public class LavkaSkipTicketTest extends AbstractModuleOwTest {

    private static final String LAVKA_CHANNEL_CODE = "mail";
    private static final String LAVKA_DELIVERY_CODE = "1005471";
    private static final String BERU_CONNECTION_CODE = "beru";
    private static final String BERU_BRAND_CODE = "beru";
    private static final String BERU_LOGISTIC_BRAND_CODE = "beruLogisticSupport";
    private static final String BERU_LOGISTIC_CONNECTION_CODE = "logisticSupport";
    private static final String RANDOM_BRAND_CODE = "wrong";
    private static final String RANDOM_CONNECTION_CODE = "wrong";
    private static final String BERU_LAVKA_SERVICE_CODE = "beruLavka";
    private static final String BERU_LOGISTIC_SERVICE_CODE = "logisticSupPokupkiCourierOnDemand";
    private static final DeliveryFeature FEATURE_TO_SKIP = DeliveryFeature.ON_DEMAND_MARKET_PICKUP;

    @Inject
    private DbService dbService;
    @Inject
    private BcpService bcpService;
    @Inject
    private OrderTestUtils orderTestUtils;
    @Inject
    private TicketTestUtils ticketTestUtils;
    @Inject
    private ServiceTimeTestUtils serviceTimeTestUtils;
    @Inject
    private MailTestUtils mailTestUtils;
    @Inject
    private MailMessageBuilderService mailMessageBuilderService;

    private static Stream<Arguments> dataForTest() {
        return Stream.of(
                arguments("ShouldNotAssignDueToFeature1",
                        Set.of(FEATURE_TO_SKIP),
                        BERU_BRAND_CODE,
                        BERU_LAVKA_SERVICE_CODE,
                        false,
                        BERU_CONNECTION_CODE),
                arguments("ShouldNotAssignDueToFeature2",
                        Set.of(FEATURE_TO_SKIP, DeliveryFeature.ON_DEMAND_YALAVKA),
                        BERU_LOGISTIC_BRAND_CODE,
                        BERU_LOGISTIC_SERVICE_CODE,
                        false,
                        BERU_LOGISTIC_CONNECTION_CODE),
                arguments("ShouldAssignDueToConnection1",
                        Set.of(DeliveryFeature.ON_DEMAND_YALAVKA),
                        BERU_BRAND_CODE,
                        BERU_LAVKA_SERVICE_CODE,
                        true,
                        BERU_CONNECTION_CODE),
                arguments("ShouldAssignDueToConnection2",
                        Set.of(DeliveryFeature.ON_DEMAND_YALAVKA),
                        BERU_LOGISTIC_BRAND_CODE,
                        BERU_LOGISTIC_SERVICE_CODE,
                        true,
                        BERU_LOGISTIC_CONNECTION_CODE),
                arguments("ShouldAssignEvenOtherBrandIsPresent",
                        Set.of(DeliveryFeature.ON_DEMAND_YALAVKA),
                        RANDOM_BRAND_CODE,
                        Randoms.string(),
                        false,
                        BERU_CONNECTION_CODE),
                arguments("ShouldNotAssignIfBrandIsSuitableButConnectionIsNot",
                        Set.of(DeliveryFeature.ON_DEMAND_YALAVKA),
                        BERU_BRAND_CODE,
                        Randoms.string(),
                        false,
                        RANDOM_CONNECTION_CODE)
        );
    }

    @BeforeEach
    public void setUp() {
        orderTestUtils.clearCheckouterAPI();
        ensureBeruLavkaService(BERU_LAVKA_SERVICE_CODE);
        ensureBeruLavkaService(BERU_LOGISTIC_SERVICE_CODE);
    }

    /**
     * Очередь Лавки НЕ назначается обращению автоматом, если DeliveryFeature.ON_DEMAND_MARKET_PICKUP присутствует в
     * фичах доставки заказа, и назначается иначе (https://st.yandex-team.ru/OCRM-7237)
     * <p>
     * Очередь "Покупки > Обратная связь с Лавкой" или "Логистическая поддержка Покупок > Курьер по запросу"
     * назначается ТОЛЬКО если Бренд обращения beru или beruLogisticSupport
     * и не назначается для других брендов (https://st.yandex-team.ru/OCRM-7608)
     */
    @Transactional
    @MethodSource("dataForTest")
    @ParameterizedTest(name = "{0}")
    public void checkLavkaServiceAssigning(String name,
                                           Set<DeliveryFeature> features,
                                           String brand,
                                           String expectedServiceCode,
                                           boolean shouldEquals,
                                           String connectionCode) {
        Order order = orderTestUtils.createOrder(Map.of(
                Order.DELIVERY_SERVICE, getLavkaDeliveryService(),
                Order.DELIVERY_FEATURES, features));

        var mailConnection = mailTestUtils.createMailConnection(connectionCode);

        var mailMessage = mailMessageBuilderService.getMailMessageBuilder(connectionCode)
                .build();

        InMailMessageHolder.withStoreMail(mailMessage, () -> {
            Ticket ticket = ticketTestUtils.createTicket(TicketFirstLine.FQN, Map.of(
                    TicketFirstLine.ORDER, order,
                    TicketFirstLine.CHANNEL, getMailChanel(),
                    TicketFirstLine.BRAND, ticketTestUtils.createBrand(brand)));

            String actualServiceCode = Optional.ofNullable(ticket.getService()).map(Service::getCode).orElse(null);
            if (shouldEquals) {
                Assertions.assertEquals(expectedServiceCode, actualServiceCode, name);
            } else {
                Assertions.assertNotEquals(expectedServiceCode, actualServiceCode, name);
            }
        });
    }

    private Channel getMailChanel() {
        Supplier<Channel> channelCreator =
                () -> bcpService.create(Channel.FQN, Map.of(Channel.CODE, "mail"));

        Query findChannelQuery =
                Query.of(Channel.FQN).withFilters(Filters.eq(Channel.CODE, LAVKA_CHANNEL_CODE));

        return dbService.<Channel>list(findChannelQuery).stream().findFirst().orElseGet(channelCreator);
    }

    private DeliveryService getLavkaDeliveryService() {
        Query findDeliveryServiceQuery =
                Query.of(DeliveryService.FQN).withFilters(Filters.eq(DeliveryService.CODE, LAVKA_DELIVERY_CODE));

        return dbService.<DeliveryService>list(findDeliveryServiceQuery).stream()
                .findFirst()
                .orElseGet(() -> bcpService.create(DeliveryService.FQN, Map.of(
                        DeliveryService.CODE, LAVKA_DELIVERY_CODE,
                        DeliveryService.TITLE, "Курьер по запросу")));
    }

    private void ensureBeruLavkaService(String serviceCode) {
        Query findServiceQuery =
                Query.of(Service.FQN).withFilters(Filters.eq(DeliveryService.CODE, serviceCode));

        Service service = dbService.<Service>list(findServiceQuery).stream()
                .findFirst()
                .orElseGet(() -> bcpService.create(Service.FQN_DEFAULT, Map.of(Service.CODE, serviceCode)));

        bcpService.edit(service, Service.SERVICE_TIME, serviceTimeTestUtils.createServiceTime24x7());
    }
}

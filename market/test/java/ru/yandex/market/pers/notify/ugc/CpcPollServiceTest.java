package ru.yandex.market.pers.notify.ugc;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.service.MailerNotificationEventService;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;
import ru.yandex.market.pers.notify.ugc.model.OrderDelivery;
import ru.yandex.market.pers.notify.ugc.model.PollType;
import ru.yandex.market.util.db.ConfigurationService;
import ru.yandex.qe.yt.cypress.http.HttpCypress;
import ru.yandex.qe.yt.cypress.ypath.ObjectYPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.notify.ugc.CpcPollService.getSendTime;

public class CpcPollServiceTest extends MarketMailerMockedDbTest {

    @Autowired
    private CpcPollService cpcPollService;

    @Autowired
    private JdbcTemplate ytJdbcTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HttpCypress ytClient;

    @Autowired
    private MailerNotificationEventService mailerNotificationEventService;

    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;

    @Autowired
    ConfigurationService configurationService;

    @BeforeEach
    void clearAntifraudTables() {
        jdbcTemplate.update("truncate table ANTIFRAUD_TABLES");
    }

    @Test
    public void scheduleModelPollNotification() {
        cpcPollService.storeOrderDeliveries(prepareOrderDelivery(od -> od.setPollType(PollType.MODEL)));
        String activeEmail = subscriptionAndIdentityService.getActiveEmail(new Uid(OrderDeliveryParam.UID));
        NotificationEvent event = mailerNotificationEventService.getLastEventByEmail(activeEmail);
        assertNull(event);
    }

    @Test
    public void scheduleShopPollNotification() {
        cpcPollService.storeOrderDeliveries(prepareOrderDelivery(od -> od.setPollType(PollType.SHOP)));
        String activeEmail = subscriptionAndIdentityService.getActiveEmail(new Uid(OrderDeliveryParam.UID));
        NotificationEvent event = mailerNotificationEventService.getLastEventByEmail(activeEmail);
        assertEquals(NotificationSubtype.GRADE_SHOP_AFTER_CPC_ORDER, event.getNotificationSubtype(), "Тип уведомления");
        assertEquals(String.valueOf(OrderDeliveryParam.SHOP_ID), event.getData().get(NotificationEventDataName.SHOP_ID), "id магазина");
        assertEquals(OrderDeliveryParam.SHOP_NAME, event.getData().get(NotificationEventDataName.SHOP_NAME), "Название магазина");
        assertEquals(getSendTime(OrderDeliveryParam.BEGIN_TIME).getTime(), event.getSendTime().getTime(), "Время отправки");
    }

    @Test
    public void scheduleUnauthorizedNotification() {
        cpcPollService.storeOrderDeliveries(prepareOrderDelivery(od -> {
            od.setUid(null);
            od.setBeginTime(new Date().getTime());
        }));
        List<NotificationEvent> eventsForProcessing = mailerNotificationEventService.getEventsForProcessing(Arrays.asList(
                NotificationSubtype.GRADE_MODEL_AFTER_CPC_ORDER,
                NotificationSubtype.GRADE_SHOP_AFTER_CPC_ORDER), NotificationEventStatus.NEW);
        assertThat(eventsForProcessing, empty());
    }

    @Test
    public void scheduleUnknownPollNotification() {
        cpcPollService.storeOrderDeliveries(prepareOrderDelivery(od -> od.setBeginTime(new Date().getTime())));
        List<NotificationEvent> eventsForProcessing = mailerNotificationEventService.getEventsForProcessing(Arrays.asList(
                NotificationSubtype.GRADE_MODEL_AFTER_CPC_ORDER,
                NotificationSubtype.GRADE_SHOP_AFTER_CPC_ORDER), NotificationEventStatus.NEW);
        assertThat(eventsForProcessing, empty());
    }


    private Matcher<String> prepareMatcher(String s) {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object o) {
                return ((String) o).contains(s);
            }

            @Override
            public void describeTo(Description description) {

            }
        };
    }

    /**
     * SELLS_IMPORT_LAST_DAY_KEY - 2019-03-23
     * сначала вгрузим 2019-03-27 и не обновим SELLS_IMPORT_LAST_DAY_KEY
     * когда нам отдадут 2019-03-24 вгрузим её и обновим SELLS_IMPORT_LAST_DAY_KEY - 2019-03-27
     */
    @Test
    public void testSchedule() {
        List<String> dates = new ArrayList<>(Arrays.asList("2019-03-21", "2019-03-22", "2019-03-22_models", "2019-03-23", "2019-03-25", "2019-03-26"));
        jdbcTemplate.batchUpdate(
            "insert into ANTIFRAUD_TABLES(table_name) values (?)",
            dates, dates.size(),
            (ps, name) -> ps.setString(1, name));
        List<String> firstImport = new ArrayList<>(dates);
        firstImport.add("2019-03-27");
        List<String> secondImport = new ArrayList<>(firstImport);
        secondImport.add("2019-03-24");
        when(ytClient.list(any(Optional.class), anyBoolean(), any(ObjectYPath.class))).thenReturn(firstImport, secondImport);
        configurationService.mergeValue(CpcPollService.LAST_TABLE_KEY, "2019-03-23");

        cpcPollService.scheduleNotifications();

        verify(ytJdbcTemplate).query(argThat(prepareMatcher("2019-03-27")), any(ResultSetExtractor.class));

        assertEquals("2019-03-23", configurationService.getValue(CpcPollService.LAST_TABLE_KEY));

        cpcPollService.scheduleNotifications();

        verify(ytJdbcTemplate).query(argThat(prepareMatcher("2019-03-24")), any(ResultSetExtractor.class));
        assertEquals("2019-03-27", configurationService.getValue(CpcPollService.LAST_TABLE_KEY));
    }

    private List<OrderDelivery> prepareOrderDelivery(Consumer<OrderDelivery> modifier) {
        List<OrderDelivery> orderDeliveries = new ArrayList<>();
        OrderDelivery orderDelivery = getDefaultOrderDelivery();
        modifier.accept(orderDelivery);
        orderDeliveries.add(orderDelivery);
        return orderDeliveries;
    }

    private OrderDelivery getDefaultOrderDelivery() {
        OrderDelivery orderDelivery = new OrderDelivery();
        orderDelivery.setUid(OrderDeliveryParam.UID);
        orderDelivery.setModelId(OrderDeliveryParam.MODEL_ID);
        orderDelivery.setShopId(OrderDeliveryParam.SHOP_ID);
        orderDelivery.setBeginTime(OrderDeliveryParam.BEGIN_TIME);
        orderDelivery.setCategoryName(OrderDeliveryParam.CATEGORY_NAME);
        orderDelivery.setModelTitle(OrderDeliveryParam.MODEL_TITLE);
        orderDelivery.setShopName(OrderDeliveryParam.SHOP_NAME);
        orderDelivery.setPollType(OrderDeliveryParam.POLL_TYPE);
        return orderDelivery;
    }

    private static final class OrderDeliveryParam {

        static final long UID = 1L;
        static final long MODEL_ID = 1722193751L;
        static final long SHOP_ID = 155L;
        static final long BEGIN_TIME = getTomorrowDate();
        static final String CATEGORY_NAME = "Мобильные телефоны";
        static final String MODEL_TITLE = "Samsung Galaxy S8";
        static final String SHOP_NAME = "OZON.ru";
        static final PollType POLL_TYPE = PollType.UNKNOWN;

        private static long getTomorrowDate() {
            Calendar c = Calendar.getInstance();
            c.setTime(new Date());
            c.add(Calendar.DATE, 1);
            return c.getTimeInMillis() / 1000;
        }

    }

}

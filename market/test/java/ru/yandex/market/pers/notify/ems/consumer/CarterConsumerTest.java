package ru.yandex.market.pers.notify.ems.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.carter.web.CartViewModel;
import ru.yandex.market.pers.notify.ems.NotificationProcessor;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.event.NotificationEventPayload;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.ems.service.MailerNotificationEventService;
import ru.yandex.market.pers.notify.entity.OfferModel;
import ru.yandex.market.pers.notify.mail.consumer.CarterConsumer_1;
import ru.yandex.market.pers.notify.mail.consumer.CarterConsumer_2;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.SenderTemplate;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author kukabara
 */
public class CarterConsumerTest extends MarketMailerMockedDbTest {
    private static int count = 0;
    @Autowired
    private CarterConsumer_1 carterConsumer_1;
    @Autowired
    private CarterConsumer_2 carterConsumer_2;
    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private MailerNotificationEventService mailerNotificationEventService;
    @Autowired
    @Qualifier("mailProcessor")
    private NotificationProcessor mailProcessor;
    @Autowired
    private ru.yandex.market.pers.notify.external.report.ReportService mailerReportService;

    @Test
    public void cart1Works() {
        NotificationEvent testEvent = notificationEventService.addEvent(NotificationEventSource
            .fromUid(1L, NotificationSubtype.CART_1)
            .build());
        testEvent = mailerNotificationEventService.getEvent(testEvent.getId());
        assertEquals(NotificationEventStatus.SENT, carterConsumer_1.processEvent(testEvent).getStatus());

        mailProcessor.process();

        assertTrue(notificationEventService.mailEventsExist(testEvent.getAddress(),
            NotificationSubtype.CART_2, NotificationEventStatus.NEW));
    }

    @Test
    public void cart2WithoutRecommendations() {
        NotificationEventPayload<CartViewModel> event = notificationEventPayload("mail.run98@yandex.ru");;
        assertEquals(SenderTemplate.ABANDONED_2_RECOMMENDATIONS, carterConsumer_2.resolveTemplate(event));
        assertEquals(NotificationEventStatus.REJECTED_NO_DATA, carterConsumer_2.processEvent(event).getStatus());

    }

    @Test
    public void cart2WorksNoAction() {
        NotificationEventPayload<CartViewModel> event = notificationEventPayload("mail.run70@yandex.ru");
        assertEquals(SenderTemplate.ABANDONED_2_NO_ACTION, carterConsumer_2.resolveTemplate(event));
        assertEquals(NotificationEventStatus.SENT, carterConsumer_2.processEvent(event).getStatus());
    }

    @Test
    public void cart2WorksRecommendations() {
        when(mailerReportService.searchOffersForSimilarModels(anyLong(), anyLong(), anyLong(), anyInt()))
            .then(invocation -> IntStream.range(0, 6).boxed().map(e -> generateNewOfferModel()).collect(Collectors.toList()));

        NotificationEventPayload<CartViewModel> event = notificationEventPayload("mail.run98@yandex.ru");
        assertEquals(SenderTemplate.ABANDONED_2_RECOMMENDATIONS, carterConsumer_2.resolveTemplate(event));
        assertEquals(NotificationEventStatus.SENT, carterConsumer_2.processEvent(event).getStatus());
    }

    @Test
    public void cart2WorksRegular() {
        when(mailerReportService.searchOffersForSimilarModels(anyLong(), anyLong(), anyLong(), anyInt()))
            .then(invocation -> IntStream.range(0, 6).boxed().map(e -> generateNewOfferModel()).collect(Collectors.toList()));

        NotificationEventPayload<CartViewModel> event = notificationEventPayload("mail.run97@yandex.ru");
        assertEquals(SenderTemplate.ABANDONED_2, carterConsumer_2
            .resolveTemplate(event));
        assertEquals(NotificationEventStatus.SENT, carterConsumer_2.processEvent(event).getStatus());
    }

    private NotificationEventPayload<CartViewModel> notificationEventPayload(String email) {
        NotificationEvent event = notificationEventService.addEvent(NotificationEventSource
            .fromUid(1L, NotificationSubtype.CART_2)
            .setEmail(email)
            .build());
        return NotificationEventPayload.from(
            mailerNotificationEventService.getEvent(event.getId())
        );
    }

    public List<OfferModel> getRecommend() {
        List<OfferModel> similar = new ArrayList<>();
        similar.add(generateNewOfferModel());
        similar.add(generateNewOfferModel());
//        similar.add(generateNewOfferModel());
        return similar;
    }

    private OfferModel generateNewOfferModel() {
        OfferModel model = new OfferModel();
        model.setCategory("Мобильные телефоны");
        model.setCategoryId(91491L);
        model.setFromPrice(44990d);
        model.setModelName("Samsung Galaxy Tab S 10.5 SM-T805 16Gb");
        model.setPictureUrl("https://mdata.yandex.net/i?path=b0910230238_img_id6738100464582526458.jpg");
        model.setModelId(11031621L + count++);
        model.setCurrency("RUR");
        return model;
    }
}

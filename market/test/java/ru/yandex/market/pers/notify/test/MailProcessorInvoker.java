package ru.yandex.market.pers.notify.test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.yandex.market.pers.notify.ems.NotificationProcessor;

public class MailProcessorInvoker {
    @Autowired
    @Qualifier("mailProcessor")
    private NotificationProcessor mailProcessor;
    @Autowired
    @Qualifier("priceMailProcessor")
    private NotificationProcessor priceMailProcessor;
    @Autowired
    @Qualifier("fastMailProcessor")
    private NotificationProcessor fastMailProcessor;
    @Autowired
    @Qualifier("orderMailProcessor")
    private NotificationProcessor orderMailProcessor;
    @Autowired
    @Qualifier("receiptMailProcessor")
    private NotificationProcessor receiptMailProcessor;
    @Autowired
    @Qualifier("blueOrderDeliveredProcessor")
    private NotificationProcessor blueOrderDeliveredProcessor;
    @Autowired
    @Qualifier("confirmSubscriptionProcessor")
    private NotificationProcessor confirmSubscriptionProcessor;
    @Autowired
    @Qualifier("transboundaryTradingMailProcessor")
    private NotificationProcessor transboundaryTradingMailProcessor;


    public void processAllMail() {
        fastMailProcessor.process();
        orderMailProcessor.process();
        mailProcessor.process();
        priceMailProcessor.process();
        receiptMailProcessor.process();
        blueOrderDeliveredProcessor.process();
        confirmSubscriptionProcessor.process();
        transboundaryTradingMailProcessor.process();
    }
}

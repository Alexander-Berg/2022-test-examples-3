package ru.yandex.market.core.tmessage;

import java.util.HashMap;

import javax.management.remote.NotificationResult;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.staff.EmployeeService;
import ru.yandex.market.core.telegram.service.TelegramAccountService;
import ru.yandex.market.core.tmessage.dao.TargetMessageDao;
import ru.yandex.market.core.tmessage.service.TargetMessageService;
import ru.yandex.market.core.tmessage.service.impl.TargetMessageServiceImpl;
import ru.yandex.market.notification.service.provider.content.TelegramBotContentProvider;
import ru.yandex.market.notification.service.provider.template.TelegramBotTemplateProvider;
import ru.yandex.market.notification.telegram.bot.client.PartnerBotRestClient;

public class TargetMessageTestConfig {

    @Bean(name = "targetMessageServiceWithMockedPartnerBotRestClient")
    public TargetMessageService targetMessageService(final TargetMessageDao partiallyTargetMessageDao,
                                                     final TelegramAccountService telegramAccountService,
                                                     final EmployeeService employeeService,
                                                     final NotificationService notificationService,
                                                     final TransactionTemplate transactionTemplate,
                                                     final TelegramBotTemplateProvider telegramBotTemplateProvider,
                                                     TelegramBotContentProvider telegramBotContentProvider) {

        PartnerBotRestClient restClient = Mockito.mock(PartnerBotRestClient.class);
        HashMap<Long, NotificationResult> toBeReturned = new HashMap<>();

        Mockito
                .doReturn(toBeReturned)
                .when(restClient)
                .sendMessage(Mockito.anyCollection(), Mockito.anyString());

        TargetMessageServiceImpl targetMessageService = new TargetMessageServiceImpl(
                partiallyTargetMessageDao,
                restClient,
                employeeService,
                telegramAccountService,
                notificationService,
                telegramBotTemplateProvider,
                telegramBotContentProvider);
        targetMessageService.setTransactionTemplate(transactionTemplate);
        return targetMessageService;
    }
}

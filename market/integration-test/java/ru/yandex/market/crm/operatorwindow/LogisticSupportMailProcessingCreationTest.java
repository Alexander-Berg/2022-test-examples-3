package ru.yandex.market.crm.operatorwindow;

import java.util.List;

import jdk.jfr.Description;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.operatorwindow.jmf.entity.BeruLogisticSupportTicket;
import ru.yandex.market.crm.operatorwindow.jmf.entity.YandexDeliveryLogisticSupportTicket;
import ru.yandex.market.crm.operatorwindow.utils.mail.LogisticMailBodyBuilder;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.mail.InMailMessage;

import static ru.yandex.market.crm.operatorwindow.Constants.Service.BERU_LOGISTIC_SUPPORT_GRAN_LOST_CALL;
import static ru.yandex.market.crm.operatorwindow.Constants.Service.BERU_LOGISTIC_SUPPORT_IMPORTANT;
import static ru.yandex.market.crm.operatorwindow.Constants.Service.BERU_LOGISTIC_SUPPORT_QUESTION;
import static ru.yandex.market.crm.operatorwindow.Constants.Service.BERU_LOGISTIC_SUPPORT_TK_IMPORTANT;
import static ru.yandex.market.crm.operatorwindow.Constants.Service.BERU_LOGISTIC_SUPPORT_TK_QUESTION;
import static ru.yandex.market.crm.operatorwindow.Constants.Service.YANDEX_DELIVERY_LOGISTIC_SUPPORT_IMPORTANT;
import static ru.yandex.market.crm.operatorwindow.Constants.Service.YANDEX_DELIVERY_LOGISTIC_SUPPORT_QUESTION;

@Transactional
public class LogisticSupportMailProcessingCreationTest extends AbstractLogisticSupportMailProcessingTest {

    public static List<Arguments> dataForTest() {
        return List.of(
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "Беру: изменить",
                        new LogisticMailBodyBuilder(true)
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_TK_QUESTION,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "БерУ: изменить заказ",
                        new LogisticMailBodyBuilder(true)
                                .setClientType("это_вип_клиент")
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_TK_IMPORTANT,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "БЕРУ: ИзменитЬ заказ",
                        new LogisticMailBodyBuilder(true)
                                .build() + "\n\ntest@dPd.ru\ntest",
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_QUESTION,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "БерУ: изменить заказ",
                        new LogisticMailBodyBuilder(true)
                                .build(),
                        "testtest@gnom.land",
                        BERU_LOGISTIC_SUPPORT_TK_IMPORTANT,
                        BeruLogisticSupportTicket.FQN),
                // Гран Беру > Приоритетные
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "Беру: изменить",
                        new LogisticMailBodyBuilder(true)
                                .setClientProblem("Проблемы с изменением статуса")
                                .setClientType("VIP client")
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_IMPORTANT,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "Последний день доставки",
                        new LogisticMailBodyBuilder(true)
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_IMPORTANT,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "1. задержка доставки подарка и 3-е обращение клиента, test",
                        new LogisticMailBodyBuilder(true)
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_IMPORTANT,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "test",
                        new LogisticMailBodyBuilder(true)
                                .build() + " Канал поступления Соцсети!",
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_IMPORTANT,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "test",
                        new LogisticMailBodyBuilder(true)
                                .build() + "срочно",
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_IMPORTANT,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "test",
                        new LogisticMailBodyBuilder(true)
                                .build() + "Запрос от Рекламаций",
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_IMPORTANT,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "test",
                        new LogisticMailBodyBuilder(true)
                                .build() + "Запрос от Рекламаций",
                        "agentne007@YANDEX.RU",
                        BERU_LOGISTIC_SUPPORT_IMPORTANT,
                        BeruLogisticSupportTicket.FQN),
                // Гран Беру > Недозвоны
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "Запрос дополнительного номера телефона получателя по заказу",
                        new LogisticMailBodyBuilder(true)
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_GRAN_LOST_CALL,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "Запрос дополнительного номера телефона отправителя по заказу",
                        new LogisticMailBodyBuilder(true)
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_GRAN_LOST_CALL,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "test вопросонедозвоне test",
                        new LogisticMailBodyBuilder(true)
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_GRAN_LOST_CALL,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "Уведомление по заказу",
                        new LogisticMailBodyBuilder(true)
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_GRAN_LOST_CALL,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "ХХХ // Недозвон",
                        new LogisticMailBodyBuilder(true)
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_GRAN_LOST_CALL,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "Недозвоны - КСЭ",
                        new LogisticMailBodyBuilder(true)
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_GRAN_LOST_CALL,
                        BeruLogisticSupportTicket.FQN),
                Arguments.of(
                        BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "Недозвоны - Dimex -",
                        new LogisticMailBodyBuilder(true)
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        BERU_LOGISTIC_SUPPORT_GRAN_LOST_CALL,
                        BeruLogisticSupportTicket.FQN),
                // Логистическая поддержка ТК Я.До > Общие
                Arguments.of(
                        YA_DELIVERY_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "test",
                        new LogisticMailBodyBuilder(true).build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        YANDEX_DELIVERY_LOGISTIC_SUPPORT_QUESTION,
                        YandexDeliveryLogisticSupportTicket.FQN),
                // Логистическая поддержка ТК Я.До > Приоритетные
                Arguments.of(
                        YA_DELIVERY_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "test",
                        new LogisticMailBodyBuilder(true
                        ).build() + "Очередь: ASAP",
                        "DREAMSKIN@yandex.ru",
                        YANDEX_DELIVERY_LOGISTIC_SUPPORT_IMPORTANT,
                        YandexDeliveryLogisticSupportTicket.FQN),
                Arguments.of(
                        YA_DELIVERY_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "test",
                        new LogisticMailBodyBuilder(true)
                                .build() + "Очередь: ASAP",
                        "statnikroman@gmail.com",
                        YANDEX_DELIVERY_LOGISTIC_SUPPORT_IMPORTANT,
                        YandexDeliveryLogisticSupportTicket.FQN),
                // Обработка с почты тех поддержки Я.До
                Arguments.of(
                        YA_DELIVERY_LOGISTIC_SUPPORT_MAIL_CONNECTION,
                        "test",
                        new LogisticMailBodyBuilder(true)
                                .build(),
                        LogisticMailBodyBuilder.DEFAULT_CLIENT_EMAIL,
                        YANDEX_DELIVERY_LOGISTIC_SUPPORT_QUESTION,
                        YandexDeliveryLogisticSupportTicket.FQN));
    }

    @ParameterizedTest
    @Description("""
            Проверка создания тикетов в очередь ТК Покупки
            тест-кейс:
            - https://testpalm.yandex-team.ru/testcase/ocrm-501
            - https://testpalm2.yandex-team.ru/testcase/ocrm-502
            """)
    @MethodSource("dataForTest")
    public void testServiceAndTypeAfterCreation(String mailConnection,
                                                String subject,
                                                String body,
                                                String sender,
                                                String expectedService,
                                                Fqn expectedType) {
        var messageBuilder = BERU_LOGISTIC_SUPPORT_MAIL_CONNECTION.equals(mailConnection)
                ? beruMailMessageBuilder
                : yaDeliveryMailMessageBuilder;

        InMailMessage mailMessage = messageBuilder
                .setSubject(subject)
                .setBody(body)
                .setFrom(sender)
                .build();
        processMessage(mailMessage);
        assertTicketTypeAndService(expectedType, expectedService);
    }
}

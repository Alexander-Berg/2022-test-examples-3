package ru.yandex.market.crm.operatorwindow;

import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.crm.operatorwindow.external.platform.PlatformApiClient;
import ru.yandex.market.crm.operatorwindow.jmf.EntityHistoryStorageStrategy;
import ru.yandex.market.crm.platform.commons.Response;
import ru.yandex.market.crm.platform.models.EoEntityHistory;
import ru.yandex.market.crm.platform.profiles.Facts;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.query.SortingOrder;
import ru.yandex.market.jmf.logic.def.EntityHistory;

public class EntityHistoryStorageStrategyTest extends AbstractModuleOwTest {

    @Inject
    EntityHistoryStorageStrategy strategy;
    @Inject
    PlatformApiClient platformApiClient;

    private static String toCmpValue(EntityHistory value) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value.getCreationTime());
    }

    @BeforeEach
    public void setUp() {
        Mockito.reset(platformApiClient);
    }

    /**
     * Воспроизводим на реальных данных ошибку сортировки. История событий была отсортирована не по времени, а в
     * случайном порядке. В процессы выяснилось, что не правильно получалось значение атрибута: значение бралось при
     * помощи {@link ru.yandex.market.jmf.entity.Getter}, и в этом случае терялись переопределения значения атрибутов
     * методом {@link ru.yandex.market.jmf.entity.EntityAdapterService#wrap(Entity, Map)}.
     */
    @Test
    public void checkOrder() {
        EoEntityHistory i1 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"edit\",\"gid\":\"entityHistory@167643378\"," +
                        "\"creationTime\":\"2021-04-12T14:21:08.148+03:00\"," +
                        "\"author\":null,\"authorUid\":1120000000037850," +
                        "\"anImport\":null,\"description\":\"Объект изменен:\\n  Счетчик контроля времени выполнения:" +
                        "  PT0S (NOT_STARTED) -> 2021-04-12T14:51:08+03:00 PT0S (ACTIVE);\\n  Время перехода в " +
                        "статус: 2021-04-12T12:47:05.473+03:00 -> 2021-04-12T14:21:08.148+03:00;\\n  Время обработки " +
                        "задачи: PT0S (PAUSED) -> PT0S (ACTIVE);\\n  Взято в работу: false -> true;\\n  Ответственный" +
                        " отдел: null -> Служба тестирования партнерских интерфейсов;\\n  Ожидает назначенияна " +
                        "оператора: true -> false;\\n  Последний ответственный отдел: null -> Служба тестирования " +
                        "партнерских интерфейсов;\\n  Запас времени взятия в работу перед предупреждением: " +
                        "2021-04-12T12:47:35+03:00 PT0S (ACTIVE) ->  PT0S (NOT_STARTED);\\n  Ответственные " +
                        "сотрудники:  -> Светлана Баканова (sbakanova);\\n  Запас времени взятия в работу: " +
                        "2021-04-13T00:48:05+03:00 PT0S (ACTIVE) ->  PT0S (NOT_STARTED);\\n  Итерация контроля " +
                        "качества: null -> qualityManagementIteration@167643691;\\n  Статус: Переоткрыт (reopened) ->" +
                        " В работе (processing);\\n  Ответственный сотрудник: null -> Светлана Баканова (sbakanova)" +
                        ".\\n\",\"id\":167643378,\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\"," +
                        "\"automationRule\":null}")
                .build();
        EoEntityHistory i2 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"createTicketOnChatInConsultation#addComment\"," +
                        "\"gid\":\"entityHistory@167646068\",\"creationTime\":\"2021-04-12T12:45:52.729+03:00\"," +
                        "\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Добавлен " +
                        "комментарий:\\nДобрый день! какой у вас вопрос?\",\"id\":167646068," +
                        "\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\",\"automationRule\":null}")
                .build();
        EoEntityHistory i3 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"fillCustomerInTicketOnCreate#edit\",\"gid\":\"entityHistory@167646069\"," +
                        "\"creationTime\":\"2021-04-12T12:45:53.282+03:00\",\"author\":null,\"authorUid\":null," +
                        "\"anImport\":null,\"description\":\"Объект изменен:\\n  Запас времени взятия в работу перед " +
                        "предупреждением:  PT0S (NOT_STARTED) -> 2021-04-12T12:46:23+03:00 PT0S (ACTIVE);\\n  Запас " +
                        "времени взятия в работу:  PT0S (NOT_STARTED) -> 2021-04-13T00:46:53+03:00 PT0S (ACTIVE);\\n " +
                        " Таймер бездействия в обращении:  PT0S (NOT_STARTED) -> 2021-05-12T12:45:53+03:00 PT0S " +
                        "(ACTIVE);\\n  Покупатель: null -> Pupkin Vasily.\\n\",\"id\":167646069," +
                        "\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\",\"automationRule\":null}")
                .build();
        EoEntityHistory i4 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"createTicketOnChatInConsultation#create\"," +
                        "\"gid\":\"entityHistory@167646070\",\"creationTime\":\"2021-04-12T12:45:52.729+03:00\"," +
                        "\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Объект создан\"," +
                        "\"id\":167646070,\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\"," +
                        "\"automationRule\":null}")
                .build();
        EoEntityHistory i5 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"createTicketOnChatInConsultation#edit\"," +
                        "\"gid\":\"entityHistory@167646072\",\"creationTime\":\"2021-04-12T12:45:53.567+03:00\"," +
                        "\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Объект изменен:\\n  " +
                        "Таймер ожидания ответа партнера:  PT0S (NOT_STARTED) -> 2021-04-13T00:46:53+03:00 PT0S " +
                        "(ACTIVE);\\n  Время перехода в статус: 2021-04-12T12:45:52.729+03:00 -> 2021-04-12T12:45:53" +
                        ".567+03:00;\\n  Запас времени взятия в работу перед предупреждением: " +
                        "2021-04-12T12:46:22+03:00 PT0S (ACTIVE) ->  PT0S (NOT_STARTED);\\n  Запас времени на решение" +
                        " обращения: 2021-04-13T12:46:52+03:00 PT0S (ACTIVE) ->  PT0S (PAUSED);\\n  Счетчик " +
                        "суммарного времени обработки задачи: PT0S (ACTIVE) -> PT0.837718S (PAUSED);\\n  Запас " +
                        "времени взятия в работу: 2021-04-13T00:46:52+03:00 PT0S (ACTIVE) ->  PT0S (NOT_STARTED);\\n " +
                        " Таймер бездействия в обращении: 2021-05-12T12:45:52+03:00 PT0S (ACTIVE) -> " +
                        "2021-05-12T12:45:53+03:00 PT0S (ACTIVE);\\n  Статус: Новый (registered) -> Ждет ответа " +
                        "партнера (waitingPartner);\\n  Запас времени: 2021-04-13T12:46:52+03:00 PT0S (ACTIVE) ->  " +
                        "PT0S (PAUSED).\\n\",\"id\":167646072,\"metaclass\":\"entityHistory\"," +
                        "\"entity\":\"ticket@167642188\",\"automationRule\":null}")
                .build();
        EoEntityHistory i6 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"createTicketOnChatInConsultation#addComment\"," +
                        "\"gid\":\"entityHistory@167646077\",\"creationTime\":\"2021-04-12T12:46:08.885+03:00\"," +
                        "\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Добавлен " +
                        "комментарий:\\nХочу начать спор\",\"id\":167646077,\"metaclass\":\"entityHistory\"," +
                        "\"entity\":\"ticket@167642188\",\"automationRule\":null}")
                .build();
        EoEntityHistory i7 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"setBeruOrderConsultationArbitrageAvailable#edit\"," +
                        "\"gid\":\"entityHistory@167649713\",\"creationTime\":\"2021-04-12T12:46:29.098+03:00\"," +
                        "\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Объект изменен:\\n  " +
                        "Доступен ли клиенту перевод в арбитраж: true -> false.\\n\",\"id\":167649713," +
                        "\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\",\"automationRule\":null}")
                .build();
        EoEntityHistory i8 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"createTicketOnChatInConsultation#addComment\"," +
                        "\"gid\":\"entityHistory@167649716\",\"creationTime\":\"2021-04-12T12:46:30.235+03:00\"," +
                        "\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Добавлен " +
                        "комментарий:\\nНужна помощь Яндекс.Маркета\",\"id\":167649716," +
                        "\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\",\"automationRule\":null}")
                .build();
        EoEntityHistory i9 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"createTicketOnChatInConsultation#edit\"," +
                        "\"gid\":\"entityHistory@167649718\",\"creationTime\":\"2021-04-12T12:46:30.262+03:00\"," +
                        "\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Объект изменен:\\n  " +
                        "Время перехода в статус: 2021-04-12T12:46:19.182+03:00 -> 2021-04-12T12:46:30.262+03:00;\\n " +
                        " Таймер бездействия в обращении: 2021-05-12T12:46:19+03:00 PT0S (ACTIVE) -> " +
                        "2021-05-12T12:46:30+03:00 PT0S (ACTIVE);\\n  Статус: Ожидает ответа от пользователя " +
                        "(waitingResponse) -> Ждет ответа партнера (waitingPartner).\\n\",\"id\":167649718," +
                        "\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\",\"automationRule\":null}")
                .build();
        EoEntityHistory i10 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"createCommentOnArbitrageRequested#addComment\"," +
                        "\"gid\":\"entityHistory@167649723\",\"creationTime\":\"2021-04-12T12:46:55.281+03:00\"," +
                        "\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Добавлен " +
                        "комментарий:\\nЗапрошено подтверждение перехода в арбитраж\",\"id\":167649723," +
                        "\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\",\"automationRule\":null}")
                .build();
        EoEntityHistory i11 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"sendChatMessageOnChatCommentCreation#editComment\"," +
                        "\"gid\":\"entityHistory@167649724\",\"creationTime\":\"2021-04-12T12:47:11.648+03:00\"," +
                        "\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Комментарий изменен: " +
                        "\\\"Служба поддержки уже начала разбираться в ситуации и ответит здесь в течение суток.\\\" " +
                        "=> \\\"Служба поддержки уже начала разбираться в ситуации и ответит здесь в течение суток" +
                        ".\\\"\",\"id\":167649724,\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\"," +
                        "\"automationRule\":null}")
                .build();
        EoEntityHistory i12 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"sendChatMessagesWhenArbitrageStarted#addComment\"," +
                        "\"gid\":\"entityHistory@167649725\",\"creationTime\":\"2021-04-12T12:47:11.53+03:00\"," +
                        "\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Добавлен " +
                        "комментарий:\\nСлужба поддержки уже начала разбираться в ситуации и ответит здесь в течение " +
                        "суток.\",\"id\":167649725,\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\"," +
                        "\"automationRule\":null}")
                .build();
        EoEntityHistory i13 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"sendChatMessageOnPartnerChatCommentCreation#editComment\"," +
                        "\"gid\":\"entityHistory@167649726\",\"creationTime\":\"2021-04-12T12:47:11.726+03:00\"," +
                        "\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Комментарий изменен: " +
                        "\\\"Покупатель начал спор по заказу. Для его решения к чату в течение рабочего дня " +
                        "подключится арбитр Яндекс.Маркета. Изучение материалов спора и его решение может занять до " +
                        "10 рабочих дней.\\\" => \\\"Покупатель начал спор по заказу. Для его решения к чату в " +
                        "течение рабочего дня подключится арбитр Яндекс.Маркета. Изучение материалов спора и его " +
                        "решение может занять до 10 рабочих дней.\\\"\",\"id\":167649726," +
                        "\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\",\"automationRule\":null}")
                .build();
        EoEntityHistory i14 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"sendChatMessagesWhenArbitrageStarted#addComment\"," +
                        "\"gid\":\"entityHistory@167649727\",\"creationTime\":\"2021-04-12T12:47:11.695+03:00\"," +
                        "\"author\":null,\"authorUid\":null,\"anImport\":null,\"description\":\"Добавлен " +
                        "комментарий:\\nПокупатель начал спор по заказу. Для его решения к чату в течение рабочего " +
                        "дня подключится арбитр Яндекс.Маркета. Изучение материалов спора и его решение может занять " +
                        "до 10 рабочих дней.\",\"id\":167649727,\"metaclass\":\"entityHistory\"," +
                        "\"entity\":\"ticket@167642188\",\"automationRule\":null}")
                .build();
        EoEntityHistory i15 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"ticketAlertTaking#edit\",\"gid\":\"entityHistory@167649728\"," +
                        "\"creationTime\":\"2021-04-12T12:47:35.432+03:00\",\"author\":null,\"authorUid\":null," +
                        "\"anImport\":null,\"description\":\"Объект изменен:\\n  Приоритет: 70 Критичный -> 80 " +
                        "Экстренный;\\n  Приоритет (уровень): 70 -> 80.\\n\",\"id\":167649728," +
                        "\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\",\"automationRule\":null}")
                .build();
        EoEntityHistory i16 = EoEntityHistory.newBuilder()
                .setEntity("{\"process\":\"backTimerExceedSuccessful\",\"gid\":\"entityHistory@167649729\"," +
                        "\"creationTime\":\"2021-04-12T12:47:35.531+03:00\",\"author\":null,\"authorUid\":null," +
                        "\"anImport\":null,\"description\":\"Сработал таймер allowanceAlertTakingTimer\"," +
                        "\"id\":167649729,\"metaclass\":\"entityHistory\",\"entity\":\"ticket@167642188\"," +
                        "\"automationRule\":null}")
                .build();

        Response response = Response.newBuilder()
                .setFacts(Facts.newBuilder()
                        .addEoEntityHistory(i1)
                        .addEoEntityHistory(i2)
                        .addEoEntityHistory(i3)
                        .addEoEntityHistory(i4)
                        .addEoEntityHistory(i5)
                        .addEoEntityHistory(i6)
                        .addEoEntityHistory(i7)
                        .addEoEntityHistory(i8)
                        .addEoEntityHistory(i9)
                        .addEoEntityHistory(i10)
                        .addEoEntityHistory(i11)
                        .addEoEntityHistory(i12)
                        .addEoEntityHistory(i13)
                        .addEoEntityHistory(i14)
                        .addEoEntityHistory(i15)
                        .addEoEntityHistory(i16)
                )
                .build();

        Mockito.when(platformApiClient.getFacts(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                        Mockito.anyInt()))
                .thenReturn(response);

        Query q = Query.of(EntityHistory.FQN)
                .withFilters(Filters.eq("entity", "ticket@167642188"))
                .withSortingOrder(SortingOrder.desc("creationTime"));
        List<EntityHistory> result = (List) strategy.list(q);

        // Время должно быть в порядке убывания
        Assertions.assertEquals(16, result.size());
        Iterator<EntityHistory> it = result.iterator();
        Assertions.assertEquals("2021-04-12T14:21:08.148+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:47:35.531+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:47:35.432+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:47:11.726+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:47:11.695+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:47:11.648+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:47:11.53+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:46:55.281+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:46:30.262+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:46:30.235+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:46:29.098+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:46:08.885+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:45:53.567+03:00", toCmpValue(it.next()));
        Assertions.assertEquals("2021-04-12T12:45:53.282+03:00", toCmpValue(it.next()));
        // У i2 и i4 одинаковое время созания. Их порядок должен определяться gid-ом
        EntityHistory next1 = it.next();
        Assertions.assertEquals("2021-04-12T12:45:52.729+03:00", toCmpValue(next1));
        Assertions.assertEquals("entityHistory@167646070", next1.getGid());

        Assertions.assertEquals("2021-04-12T12:45:52.729+03:00", toCmpValue(it.next()));
    }
}

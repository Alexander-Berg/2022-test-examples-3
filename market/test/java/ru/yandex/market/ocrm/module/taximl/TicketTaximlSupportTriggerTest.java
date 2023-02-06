package ru.yandex.market.ocrm.module.taximl;

import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.crm.serialization.JsonDeserializer;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.module.comment.Comment;
import ru.yandex.market.jmf.module.ticket.Ticket;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.queue.retry.internal.FastRetryTasksQueue;
import ru.yandex.market.jmf.queue.retry.internal.RetryTaskProcessor;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.common.TicketFirstLine;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = ModuleTaximlTestConfiguration.class)
public class TicketTaximlSupportTriggerTest {

    @Inject
    BcpService bcpService;
    @Inject
    TicketTestUtils ticketTestUtils;
    @Inject
    TxService txService;
    @Inject
    RetryTaskProcessor retryTaskProcessor;
    @Inject
    FastRetryTasksQueue queue;
    @Inject
    DbService dbService;
    @Inject
    TaximlClient taximlClient;
    @Inject
    JsonDeserializer jsonDeserializer;


    @Test
    public void doMl() throws Exception {
        String expected = Randoms.string();
        String responseJson = "{\"reply\": {\"text\": \"" + expected + "\"}}";
        JsonNode response = jsonDeserializer.readObject(JsonNode.class, responseJson);
        Mockito.when(taximlClient.support(Mockito.any())).thenReturn(response);

        Entity t = txService.doInTx(() -> {
            Entity ticket = ticketTestUtils.createTicket(TicketFirstLine.FQN, Maps.of(
                    Ticket.CHANNEL, "mail"
            ));

            bcpService.create(Comment.FQN_USER, Map.of(
                    Comment.BODY, "comment body",
                    Comment.ENTITY, ticket
            ));

            return ticket;
        });

        // ожидание нужно для наступления времени первого срабатывания триггера
        Thread.sleep(1500);

        // принудительно вызываем срабатывание ассинхронных триггеров
        retryTaskProcessor.processPendingTasksWithReset(queue);

        Entity result = txService.doInTx(() -> dbService.get(t.getGid()));

        // При добавлении комментария к тикету должен выполнится иссинхронный тикет ticketTaximlSupport
        // который должен сходить во внешний сервис и результат сохранить в атрибуте taximlSuppor
        Object taximlSupport = result.getAttribute("taximlSupport");
        Assertions.assertEquals(expected, taximlSupport);
    }
}

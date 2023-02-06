package ru.yandex.market.mbi.audit.service.logbroker.def;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.core.logbroker.EntityHistoryOuterClass;
import ru.yandex.market.mbi.audit.service.yt.dao.EntityHistoryRepository;
import ru.yandex.market.mbi.audit.service.yt.model.LogEntityHistory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EntityHistoryDataProcessorTest {

    @Captor
    ArgumentCaptor<List<LogEntityHistory>> entityHistoriesCaptor;

    @Mock
    EntityHistoryRepository repository;

    @Test
    void processTest() {
        var dataProcessor = new EntityHistoryDataProcessor(repository);

        dataProcessor.process(new MessageBatch("testTopic", 1, Collections.singletonList(createMsg())));

        verify(repository).saveEntityHistoryAll(entityHistoriesCaptor.capture());
        var val = entityHistoriesCaptor.getValue();

        assertEquals(2,val.size());
    }

    private MessageData createMsg() {
        EntityHistoryOuterClass.EntityHistory entityHistory = EntityHistoryOuterClass.EntityHistory.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setActionId(7)
                .setDatasourceId(3)
                .setTime(DateTime.now().getMillis())
                .setActionTypeId(4)
                .setEditType(5)
                .setEntityName("test")
                .setModuleName("mbi-admin")
                .setActorId(6)
                .setEditType(7)
                .setActorType(8)
                .setHostName("localhost")
                .setActorComment("commetn 1 2 3")
                .setXmlSnapshot("<order-transaction><balance-order-id/><bank-order-id/><bank-order-time/><campaign-id>22019909</campaign-id><cession>false</cession><client-role>USER</client-role><created-at><epoch-second>1634850151</epoch-second><nano>0</nano></created-at><currency>Currency{name=RUR, aliases=[RUB]}</currency><description/><eventtime><epoch-second>1634850152</epoch-second><nano>0</nano></eventtime><mbi-control-enabled>true</mbi-control-enabled><order-id>71021427</order-id><payment-id>82885414</payment-id><paysys-type-cc/><refund-id/><shop-id>1060815</shop-id><status>STARTED</status><sum>4900</sum><trantime><epoch-second>1634850152</epoch-second><nano>0</nano></trantime><trust-payment-id>6171d56894d527f1c44beda4</trust-payment-id><trust-refund-id/><type>SUBSIDY</type><uid>1024605217</uid></order-transaction>")
                .setActorId(9)
                .build();
        EntityHistoryOuterClass.EntityHistory entityHistory1 =
                EntityHistoryOuterClass.EntityHistory.newBuilder(entityHistory)
                .setActorId(7)
                .setTime(DateTime.now().getMillis())
                .build();

        EntityHistoryOuterClass.EntityHistoryMsg.Builder msg = EntityHistoryOuterClass.EntityHistoryMsg.newBuilder();
        EntityHistoryOuterClass.EntityHistoryBatch batch = EntityHistoryOuterClass.EntityHistoryBatch.newBuilder()
                .addEntityHistory(entityHistory)
                .addEntityHistory(entityHistory1)
                .build();
        msg.setEntityHistoryBatch(batch);


        return new MessageData(msg.build().toByteArray(), 0, new MessageMeta("test".getBytes(), 0, 0, 0, "::1",
                CompressionCodec.RAW, Collections.emptyMap()));
    }
}


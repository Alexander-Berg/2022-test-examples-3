package ru.yandex.market.mbo.mdm.common.infrastructure.repository;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.mbo.mdm.common.infrastructure.model.LbDump;
import ru.yandex.market.mbo.mdm.common.infrastructure.model.LogbrokerMessageKey;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGenericMapperRepositoryTestBase;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class LbDumpRepositoryImplTest
    extends MdmGenericMapperRepositoryTestBase<LbDumpRepository, LbDump, LogbrokerMessageKey> {

    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Override
    protected LbDump randomRecord() {
        return random.nextObject(LbDump.class, "readAt");
    }

    @Override
    protected Function<LbDump, LogbrokerMessageKey> getIdSupplier() {
        return LbDump::getKey;
    }

    @Override
    protected List<BiConsumer<Integer, LbDump>> getUpdaters() {
        return List.of(
            (i, record) -> {
                record.setBinaryData(String.valueOf(i).getBytes());
            },
            (i, record) -> {
                record.setBinaryData(String.valueOf(i * 2).getBytes());
            }
        );
    }

    @Override
    protected String[] getFieldsToIgnore() {
        return new String[] {
            "readAt"
        };
    }

    @Test
    public void testDumpWithoutEnabledFlag() {
        repository.deleteAll();
        repository.dumpIfNeeded(
            "mdm--some-topic-name",
            1,
            List.of(new MessageData("data".getBytes(), 0, new MessageMeta(
                "source".getBytes(), 1, 0, 0, "", CompressionCodec.RAW, Map.of()
            )))
        );
        Assertions.assertThat(repository.findAll()).isEmpty();
    }

    @Test
    public void testDumpWithEnabledFlag() {
        repository.deleteAll();
        storageKeyValueService.putValue(MdmProperties.DUMP_INCOMING_LB_PREFIX + "mdm--some-topic-name", true);
        repository.dumpIfNeeded(
            "mdm--some-topic-name",
            1,
            List.of(new MessageData("data".getBytes(), 0, new MessageMeta(
                "source".getBytes(), 1, 0, 0, "", CompressionCodec.RAW, Map.of()
            )))
        );
        Assertions.assertThat(repository.findAll()).containsExactlyInAnyOrder(
            new LbDump()
                .setTopic("mdm--some-topic-name")
                .setGroupId(1)
                .setMsgSeqNo(1)
                .setCodec(CompressionCodec.RAW)
                .setBinaryData("data".getBytes())
        );
    }
}

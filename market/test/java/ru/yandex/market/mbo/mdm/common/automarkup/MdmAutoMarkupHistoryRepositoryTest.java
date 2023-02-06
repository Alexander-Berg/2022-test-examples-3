package ru.yandex.market.mbo.mdm.common.automarkup;

import java.time.Instant;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.automarkup.model.MdmAutoMarkupHistoryEntry;
import ru.yandex.market.mbo.mdm.common.automarkup.model.MdmAutoMarkupYqlQuery;
import ru.yandex.market.mbo.mdm.common.automarkup.repository.MdmAutoMarkupHistoryRepository;
import ru.yandex.market.mbo.mdm.common.automarkup.repository.MdmAutoMarkupYqlQueryRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGenericMapperRepositoryTestBase;

public class MdmAutoMarkupHistoryRepositoryTest
    extends MdmGenericMapperRepositoryTestBase<MdmAutoMarkupHistoryRepository, MdmAutoMarkupHistoryEntry, Long> {

    private Long linkedQueryId;

    @Autowired
    private MdmAutoMarkupYqlQueryRepository mdmAutoMarkupYqlQueryRepository;

    @Override
    public void setup() {
        super.setup();

        linkedQueryId = mdmAutoMarkupYqlQueryRepository.insert(new MdmAutoMarkupYqlQuery()
            .setName("testName")
            .setUserLogin("testUser")
            .setModifiedAt(Instant.now())
            .setRowsLimit(100)
            .setYqlQueryId("testid")
        ).getId();
    }

    @Override
    protected MdmAutoMarkupHistoryEntry randomRecord() {
        return random.nextObject(MdmAutoMarkupHistoryEntry.class)
            .setQueryId(linkedQueryId);
    }

    @Override
    protected boolean isIdGenerated() {
        return true;
    }

    @Override
    protected String[] getFieldsToIgnore() {
        return new String[]{};
    }

    @Override
    protected Function<MdmAutoMarkupHistoryEntry, Long> getIdSupplier() {
        return MdmAutoMarkupHistoryEntry::getId;
    }

    @Override
    protected List<BiConsumer<Integer, MdmAutoMarkupHistoryEntry>> getUpdaters() {
        return List.of(
            (i, record) -> record.setName("testName" + i),
            (i, record) -> record.setMessage("testMessage" + i),
            (i, record) -> record.setQueryId(linkedQueryId),
            (i, record) -> record.setYqlQueryBody("yqlBody" + i),
            (i, record) -> record.setStartedAt(Instant.now()),
            (i, record) -> record.setFinishedAt(Instant.now()),
            (i, record) -> record.setState(MdmAutoMarkupHistoryEntry.State.SUCCESS)
        );
    }

    @Test
    public void testCount() {
        testSimpleInsert();
        int actualCount = repository.getCount();
        Assertions.assertThat(actualCount).isEqualTo(1);
    }
}

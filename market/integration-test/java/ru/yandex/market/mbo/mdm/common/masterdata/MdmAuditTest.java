package ru.yandex.market.mbo.mdm.common.masterdata;

import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.pgaudit.PgAuditRecord;
import ru.yandex.market.mbo.pgaudit.PgAuditRepository;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

public class MdmAuditTest extends MdmBaseIntegrationTestClass {

    private static final int SEED = 786;
    private static final int VALID_SUPPLIER_ID = 12345;

    private EnhancedRandom random;

    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;

    @Autowired
    private PgAuditRepository pgAuditRepository;

    @Before
    public void setUp() throws Exception {
        random = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenInsertDataShouldWriteCurrentThreadToAuditContext() {
        MdmIrisPayload.Item item = random.nextObject(MdmIrisPayload.Item.class);
        MdmIrisPayload.Item itemWithValidSupplier = item.toBuilder()
            .setItemId(item.getItemId().toBuilder().setSupplierId(VALID_SUPPLIER_ID))
            .build();

        List<FromIrisItemWrapper> expectedItems = item.getInformationList()
            .stream()
            .map(info -> itemWithValidSupplier.toBuilder().clearInformation().addInformation(info).build())
            .map(FromIrisItemWrapper::new)
            .collect(Collectors.toList());

        fromIrisItemRepository.insertOrUpdateAll(expectedItems);

        List<FromIrisItemWrapper> unprocessedItems = fromIrisItemRepository.getUnprocessedItemsBatch(100);
        Assertions.assertThat(unprocessedItems)
            .usingElementComparatorIgnoringFields("receivedTs")
            .containsExactlyInAnyOrderElementsOf(expectedItems);

        List<PgAuditRecord> auditRecords = pgAuditRepository.findAll("from_iris_item");
        Assertions.assertThat(auditRecords).hasSameSizeAs(expectedItems);

        String expectedAuditContext = Thread.currentThread().getName();
        Assertions.assertThat(auditRecords).extracting(PgAuditRecord::getContext)
            .allMatch(expectedAuditContext::equals);
    }
}

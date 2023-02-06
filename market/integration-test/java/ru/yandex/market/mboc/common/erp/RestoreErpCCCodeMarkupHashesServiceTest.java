package ru.yandex.market.mboc.common.erp;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MercuryHashDao;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MercuryHashRepository;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.erp.model.ErpCCCodeMarkupChange;
import ru.yandex.market.mboc.common.masterdata.model.cccode.Cis;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static ru.yandex.market.ir.http.MdmIrisPayload.CisHandleMode.ACCEPT_ONLY_DECLARED;
import static ru.yandex.market.ir.http.MdmIrisPayload.CisHandleMode.NO_RESTRICTION;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RestoreErpCCCodeMarkupHashesServiceTest extends MdmBaseIntegrationTestClass {
    @Autowired
    private ErpCCCodeMarkupExporterRepository erpCCCodeMarkupExporterRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private MercuryHashRepository mercuryHashRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private RestoreErpCCCodeMarkupHashesService restoreErpCCCodeMarkupHashesService;

    @Before
    public void setUp() throws Exception {
        restoreErpCCCodeMarkupHashesService = new RestoreErpCCCodeMarkupHashesService(
            mercuryHashRepository,
            erpCCCodeMarkupExporterRepository,
            storageKeyValueService,
            transactionTemplate
        );
    }

    @Test
    public void testRestore() {
        //given
        var change1 =
            new ErpCCCodeMarkupChange("1", "12", Cis.REQUIRED, "12", false, List.of("RU"), ACCEPT_ONLY_DECLARED);
        var change2 = new ErpCCCodeMarkupChange("2", "", Cis.NONE, "", false, List.of(), ACCEPT_ONLY_DECLARED);
        var change3 =
            new ErpCCCodeMarkupChange("3", "123", Cis.REQUIRED, "112", false, List.of("RU", "US"), NO_RESTRICTION);
        var change4 =
            new ErpCCCodeMarkupChange("4", "512", Cis.REQUIRED, "132", true, List.of("UK"), NO_RESTRICTION);
        var change5 = new ErpCCCodeMarkupChange("5", "2112", Cis.REQUIRED, "", false, List.of(), NO_RESTRICTION);
        erpCCCodeMarkupExporterRepository.insertCCCodeMarkupChanges(List.of(
            change1, change2, change3, change4, change5
        ));

        var hash1 = MercuryHashDao.create("1", change1.hashCode(), ErpCCCodeMarkupExporterService.PROCESSOR_NAME);
        var hash2 =
            MercuryHashDao.create(
                "2", change2.hashCode() ^ Integer.MAX_VALUE, ErpCCCodeMarkupExporterService.PROCESSOR_NAME);
        var hash3 =
            MercuryHashDao.create("3", change3.hashCode() ^ 0xFF, ErpCCCodeMarkupExporterService.PROCESSOR_NAME);
        var hash4 = MercuryHashDao.create("4", change4.hashCode(), ErpCCCodeMarkupExporterService.PROCESSOR_NAME);
        var hash6 = MercuryHashDao.create("6", 827, ErpCCCodeMarkupExporterService.PROCESSOR_NAME);
        var hash7 = MercuryHashDao.create("7", 829, "ANOTHER_PROCESSOR");
        mercuryHashRepository.insertOrUpdateAll(List.of(
            hash1, hash2, hash3, hash4, hash6, hash7
        ));

        storageKeyValueService.putValue(MdmProperties.RESTORE_ERP_CCC_MARKUP_HASHES_DRY_RUN, false);
        storageKeyValueService.invalidateCache();

        //when
        restoreErpCCCodeMarkupHashesService.restoreHashes();

        //then
        Assertions.assertThat(mercuryHashRepository.findAll()).containsExactlyInAnyOrder(
            hash1,
            MercuryHashDao.create("2", change2.hashCode(), ErpCCCodeMarkupExporterService.PROCESSOR_NAME),
            MercuryHashDao.create("3", change3.hashCode(), ErpCCCodeMarkupExporterService.PROCESSOR_NAME),
            hash4,
            hash7
        );
        Assertions.assertThat(erpCCCodeMarkupExporterRepository.findAll())
            .containsExactlyInAnyOrder(change1, change2, change3, change4, change5);
        Assertions
            .assertThat(storageKeyValueService.getString(MdmProperties.RESTORE_ERP_CCC_MARKUP_HASHES_OFFSET, null))
            .isNull();
    }
}

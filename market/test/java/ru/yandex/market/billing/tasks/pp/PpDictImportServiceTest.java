package ru.yandex.market.billing.tasks.pp;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.ArgumentMatchers.any;

/**
 * Тесты для {@link PpDictImportService}.
 *
 * @author vbudnev
 */
class PpDictImportServiceTest extends FunctionalTest {

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock
    private PpPublicInfoService ppPublicInfoExportService;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
    }

    @DbUnitDataSet(
            before = {"db/AllPpDictionary.before.csv", "db/Groups.before.csv"},
            after = {"db/AllPpDictionary.after.csv", "db/Groups.after.csv"}
    )
    @Test
    void test_reimportPpData() throws IOException {
        PpDictionariesImportConfiguration configuration = PpDictionariesImportConfiguration
                .builder()
                .withMarketPpPathTranslationsUri(filePathInTest("translation_map.json"))
                .withMarketPpUri(filePathInTest("market_pp.json"))
                .withMarketAndPartnersUri(filePathInTest("market_and_partners.json"))
                .withPlacementUri(filePathInTest("placement.json"))
                .withPlatformsUri(filePathInTest("platforms.json"))
                .withUniversalReportsUri(filePathInTest("universal_reports.json"))
                .build();

        PpDictImportService ppDictImportService = new PpDictImportService(
                configuration,
                new PpDictDao(namedParameterJdbcTemplate),
                ppPublicInfoExportService,
                transactionTemplate
        );
        ppDictImportService.reimportPpData();

        Mockito.verify(ppPublicInfoExportService).refreshPublicInfo(any());
    }

    private String filePathInTest(String fileName) {
        return PpDictImportService.class.getResource(fileName).getFile();
    }
}

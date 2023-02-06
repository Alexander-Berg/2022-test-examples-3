package ru.yandex.market.deepmind.app.controllers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.app.web.ExtendedMskuFilter;
import ru.yandex.market.deepmind.app.web.ExtendedMskuFilterConverter;
import ru.yandex.market.deepmind.common.ExcelFileDownloader;
import ru.yandex.market.deepmind.common.MskuServicesTestUtils;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.background.BackgroundExportService;
import ru.yandex.market.deepmind.common.category.CategoryTree;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonPeriod;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonalDictionary;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonalMsku;
import ru.yandex.market.deepmind.common.exportable.MskuStatusesExportable;
import ru.yandex.market.deepmind.common.mocks.ExcelS3ServiceMock;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryManagerRepository;
import ru.yandex.market.deepmind.common.repository.DeepmindCategoryTeamRepository;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.SeasonalDictionaryRepository;
import ru.yandex.market.deepmind.common.repository.SeasonalMskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.mbo.excel.ExcelFile;

import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.MSKU_ID_KEY;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.SEASONAL_MSKU_KEY;
import static ru.yandex.market.deepmind.common.exportable.MskuStatusesExportable.SEASON_HEADER;
import static ru.yandex.market.deepmind.common.exportable.MskuStatusesExportable.STATUS_HEADER;
import static ru.yandex.market.deepmind.common.exportable.MskuStatusesExportable.convertSeasonToExcelField;
import static ru.yandex.market.deepmind.common.repository.season.SeasonRepository.DEFAULT_ID;

public class MskuStatusesExcelControllerTest extends DeepmindBaseAppDbTestClass {
    private static final String TEST_CATEGORY_NAME = "Test category";
    private static final long TEST_CATEGORY_ID = 1L;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private MskuRepository deepmindMskuRepository;
    @Autowired
    private MskuStatusRepository mskuStatusRepository;
    @Autowired
    private SeasonRepository seasonRepository;
    @Autowired
    private SeasonalDictionaryRepository seasonalDictionaryRepository;
    @Autowired
    private SeasonalMskuRepository seasonalMskuRepository;
    @Autowired
    private SupplierRepository deepmindSupplierRepository;
    @Autowired
    private DeepmindCategoryManagerRepository deepmindCategoryManagerRepository;
    @Autowired
    private DeepmindCategoryTeamRepository deepmindCategoryTeamRepository;

    private MskuStatusesExcelController controller;
    private ExcelFileDownloader excelFileDownloader;
    private long seasonIdGenerator;

    @Before
    public void setUp() {
        seasonIdGenerator = 1;

        var categoryCachingServiceMock = new DeepmindCategoryCachingServiceMock();
        categoryCachingServiceMock.addCategory(TEST_CATEGORY_ID, TEST_CATEGORY_NAME);

        // background & excel
        var backgroundServiceMock = new BackgroundServiceMock();
        var excelS3Service = new ExcelS3ServiceMock();
        var backgroundActionStatusService = new BackgroundExportService(backgroundServiceMock, transactionTemplate,
            excelS3Service);
        excelFileDownloader = new ExcelFileDownloader(backgroundServiceMock, excelS3Service);

        var extendedMskuFilterConverter = new ExtendedMskuFilterConverter(
            deepmindSupplierRepository, deepmindCategoryManagerRepository, deepmindCategoryTeamRepository,
            categoryCachingServiceMock
        );
        controller = new MskuStatusesExcelController(
            deepmindMskuRepository, mskuStatusRepository, extendedMskuFilterConverter, seasonalMskuRepository,
            seasonRepository, categoryCachingServiceMock, seasonalDictionaryRepository,
            backgroundActionStatusService
        );
    }

    @Test
    public void exportShouldNotFindDeletedMkus() {
        deepmindMskuRepository.save(
            TestUtils.newMsku(1111).setCategoryId(CategoryTree.ROOT_CATEGORY_ID),
            TestUtils.newMsku(2222).setCategoryId(CategoryTree.ROOT_CATEGORY_ID)
        );
        deepmindMskuRepository.delete(2222L);

        var actionId = controller.exportToExcelAsync(new ExtendedMskuFilter()
            .setMarketSkuIds(List.of(1111L, 2222L)));

        var excelFile = excelFileDownloader.downloadExport(actionId);
        DeepmindAssertions.assertThat(excelFile)
            .hasLastLine(1)
            .containsValue(1, MSKU_ID_KEY, 1111);
    }

    @Test
    public void exportSimpleExcel() {
        deepmindMskuRepository.save(
            TestUtils.newMsku(1111),
            TestUtils.newMsku(2222),
            TestUtils.newMsku(3333),
            TestUtils.newMsku(4444),
            TestUtils.newMsku(5555)
        );

        var seasonWithPeriods = nextSeason();

        mskuStatusRepository.save(
            nextMskuStatus(1111, MskuStatusValue.NPD, null),
            nextMskuStatus(2222, MskuStatusValue.REGULAR, null),
            nextMskuStatus(3333, MskuStatusValue.SEASONAL, seasonWithPeriods.getId()),
            nextMskuStatus(4444, MskuStatusValue.EMPTY, null)
        );

        seasonalDictionaryRepository.save(
            new SeasonalDictionary().setId(1L).setName("Новогодний"),
            new SeasonalDictionary().setId(2L).setName("Зимний")
        );
        seasonalMskuRepository.save(
            new SeasonalMsku().setMskuId(1111L).setSeasonalId(1L),
            new SeasonalMsku().setMskuId(2222L).setSeasonalId(1L),
            new SeasonalMsku().setMskuId(2222L).setSeasonalId(2L)
        );

        int actionId = controller.exportToExcelAsync(new ExtendedMskuFilter());
        ExcelFile excelFile = excelFileDownloader.downloadExport(actionId);

        DeepmindAssertions.assertThat(excelFile)
            .containsValue(1, MSKU_ID_KEY, 1111L)
            .containsValue(1, STATUS_HEADER, "NPD")
            .containsValue(1, SEASONAL_MSKU_KEY, "Новогодний")
            .containsValue(2, MSKU_ID_KEY, 2222L)
            .containsValue(2, STATUS_HEADER, "REGULAR")
            .containsValue(2, SEASONAL_MSKU_KEY, "Новогодний, Зимний")
            .containsValue(3, MSKU_ID_KEY, 3333L)
            .containsValue(3, STATUS_HEADER, "SEASONAL")
            .containsValue(3, SEASONAL_MSKU_KEY, null)
            .containsValue(3, SEASON_HEADER, convertSeasonToExcelField(seasonWithPeriods))
            .containsValue(4, MSKU_ID_KEY, 4444L)
            .containsValue(4, STATUS_HEADER, "-")
            .containsValue(5, MSKU_ID_KEY, 5555L)
            .containsValue(5, STATUS_HEADER, "-")
            .hasLastLine(5);
    }

    @Test
    public void shouldConvertSeasonsWithPeriods() {
        SeasonRepository.SeasonWithPeriods seasonWithPeriods1 = new SeasonRepository.SeasonWithPeriods(
            new Season(1L, "1 Warehouse", null), Collections.singletonList(
            new SeasonPeriod()
                .setWarehouseId(1L)
                .setFromMmDd("03-08")
                .setToMmDd("03-09")));
        SeasonRepository.SeasonWithPeriods seasonWithPeriods2 = new SeasonRepository.SeasonWithPeriods(
            new Season(2L, "2 8th of march", null), Collections.singletonList(
            new SeasonPeriod()
                .setWarehouseId(DEFAULT_ID)
                .setFromMmDd("03-08")
                .setToMmDd("03-09")));
        SeasonRepository.SeasonWithPeriods seasonWithPeriods3 = new SeasonRepository.SeasonWithPeriods(
            new Season(3L, "3 Olapaploosa", null), Arrays.asList(
            new SeasonPeriod()
                .setWarehouseId(DEFAULT_ID)
                .setFromMmDd("03-23")
                .setToMmDd("03-28"),
            new SeasonPeriod()
                .setWarehouseId(DEFAULT_ID)
                .setFromMmDd("09-08")
                .setToMmDd("09-09"))
        );
        Assertions.assertThat(Stream.of(seasonWithPeriods1, seasonWithPeriods2, seasonWithPeriods3))
            .map(MskuStatusesExportable::convertSeasonToExcelField)
            .containsExactly(
                "1 Warehouse \"периоды не заданы\" [#1]",
                "2 8th of march \"8 марта-9 марта\" [#2]",
                "3 Olapaploosa \"23 марта-28 марта, 8 сентября-9 сентября\" [#3]");
    }

    private SeasonRepository.SeasonWithPeriods nextSeason() {
        Season save = seasonRepository.save(new Season().setName("season#" + seasonIdGenerator++));
        return seasonRepository.findWithPeriods(new SeasonRepository.Filter().setIds(save.getId())).get(0);
    }

    private MskuStatus nextMskuStatus(long mskuId,
                                      MskuStatusValue mskuStatusValue,
                                      @Nullable Long seasonId) {
        return mskuStatusRepository.save(
            MskuServicesTestUtils.nextMskuStatus(mskuId, mskuStatusValue, seasonId)
        );
    }
}

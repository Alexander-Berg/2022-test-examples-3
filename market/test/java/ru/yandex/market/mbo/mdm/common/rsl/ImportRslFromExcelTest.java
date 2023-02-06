package ru.yandex.market.mbo.mdm.common.rsl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.ImportResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.CategoryRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.MskuRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SskuRsl;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.http.MbocCommon.MappingInfoLite;

/**
 * @author dmserebr
 * @date 26/11/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ImportRslFromExcelTest extends RslExcelTestBase {
    @Test
    public void testImport() throws IOException {
        String file = "rsl/rsl-import-test.xlsx";
        byte[] fileBytes = readResource(file);
        var parseResult = importExcelService.parseExcel(file, fileBytes);
        Assertions.assertThat(parseResult.getType()).isEqualTo(AbstractExcelSheetParser.ExcelResultType.SUCCESS);

        List<RslExcelRowData> rslExcelRowData = parseResult.getValue();

        Assertions.assertThat(rslExcelRowData).containsExactly(
            RslExcelRowDataBuilder.start().categoryId(12345L)
                .inRslDays(90).outRslDays(50).inRslPercents(60).outRslPercents(30)
                .startDate(LocalDate.parse("2019-10-15", DateTimeFormatter.ofPattern("yyyy-MM-dd"))).build(),
            RslExcelRowDataBuilder.start().categoryId(67890L)
                .inRslDays(80).outRslDays(45)
                .startDate(LocalDate.parse("2019-11-15", DateTimeFormatter.ofPattern("yyyy-MM-dd"))).build(),
            RslExcelRowDataBuilder.start().categoryId(67890L).mskuId(1234599999L)
                .inRslDays(70).outRslDays(40)
                .startDate(LocalDate.parse("2019-11-15", DateTimeFormatter.ofPattern("yyyy-MM-dd"))).build(),
            RslExcelRowDataBuilder.start().categoryId(99999L)
                .inRslPercents(50).outRslPercents(35).build(),
            RslExcelRowDataBuilder.start().categoryId(444444L).supplierId(321964).shopSku("123456")
                .inRslDays(25).outRslDays(25).build(),
            RslExcelRowDataBuilder.start().categoryId(444444L).mskuId(777888L).deleteFlag(true)
                .startDate(LocalDate.parse("2000-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))).build()
        );
    }

    @Test
    public void testImportAndSave() throws IOException {
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(12345L).setInRslDays(30).setOutRslDays(40)
            .setActivatedAt(LocalDate.parse("15/10/2019", RslExcelSheetConfig.DATE_TIME_FORMATTER)));
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(123456L).setInRslDays(4).setInRslPercents(12)
            .setOutRslPercents(5));
        mskuRslRepository.insert(new MskuRsl().setMskuId(777888L).setInRslDays(20).setOutRslDays(10));

        String file = "rsl/rsl-import-test.xlsx";
        byte[] fileBytes = readResource(file);
        importExcelService.importExcel(file, fileBytes);

        List<CategoryRsl> categoryRsls = categoryRslRepository.findAll();
        List<MskuRsl> mskuRsls = mskuRslRepository.findAll();
        List<SskuRsl> sskuRsls = sskuRslRepository.findAll();

        Assertions.assertThat(categoryRsls).containsExactly(
            new CategoryRsl().setCategoryId(123456L).setInRslDays(4).setInRslPercents(12)
                .setOutRslPercents(5),
            new CategoryRsl().setCategoryId(12345L).setInRslDays(90).setOutRslDays(50)
                .setInRslPercents(60).setOutRslPercents(30)
                .setActivatedAt(LocalDate.parse("15/10/2019", RslExcelSheetConfig.DATE_TIME_FORMATTER)),
            new CategoryRsl().setCategoryId(67890L).setInRslDays(80).setOutRslDays(45)
                .setActivatedAt(LocalDate.parse("15/11/2019", RslExcelSheetConfig.DATE_TIME_FORMATTER)),
            new CategoryRsl().setCategoryId(99999L).setInRslPercents(50).setOutRslPercents(35)
        );
        Assertions.assertThat(mskuRsls).containsExactly(
            new MskuRsl().setMskuId(1234599999L).setInRslDays(70).setOutRslDays(40)
                .setActivatedAt(LocalDate.parse("15/11/2019", RslExcelSheetConfig.DATE_TIME_FORMATTER))
        );
        Assertions.assertThat(sskuRsls).containsExactly(
            new SskuRsl().setSupplierId(321964).setShopSku("123456").setInRslDays(25).setOutRslDays(25)
        );
    }

    @Test
    public void testImportInvalid() throws IOException {
        String file = "rsl/rsl-import-invalid-test.xlsx";
        byte[] fileBytes = readResource(file);
        var importResult = importExcelService.importExcel(file, fileBytes).getErrors();

        Assertions.assertThat(importResult).containsExactly(
            "Не заполнено обязательное поле \"hid\" на строке 6",
            "На строке 3 задан ОСГ, для которого не задано значение на дату " +
                RslExcelSheetConfig.DATE_TIME_FORMATTER.format(LocalDate.now()),
            "На строке 4 должны быть заданы и входящие, и исходящие ОСГ",
            "На строке 7 должны быть заданы и входящие, и исходящие ОСГ"
        );
    }

    @Test
    public void testImportValidWithMultidates() throws IOException {
        String file = "rsl/rsl-import-multidates-test.xlsx";
        byte[] fileBytes = readResource(file);
        importExcelService.importExcel(file, fileBytes);

        List<CategoryRsl> categoryRsls = categoryRslRepository.findAll();
        List<MskuRsl> mskuRsls = mskuRslRepository.findAll();
        List<SskuRsl> sskuRsls = sskuRslRepository.findAll();

        Assertions.assertThat(categoryRsls).containsExactly(
            new CategoryRsl().setCategoryId(1234L)
                .setInRslDays(10)
                .setOutRslDays(20)
                .setActivatedAt(date("01/01/2000")),
            new CategoryRsl().setCategoryId(1234L)
                .setInRslDays(10)
                .setOutRslDays(20)
                .setActivatedAt(date("21/01/2012")),
            new CategoryRsl().setCategoryId(5678L)
                .setInRslDays(5)
                .setOutRslDays(7)
                .setActivatedAt(date("01/01/2000")),
            new CategoryRsl().setCategoryId(91011L)
                .setInRslDays(10)
                .setOutRslDays(20)
                .setActivatedAt(date("01/01/2000")),
            new CategoryRsl().setCategoryId(121314L)
                .setInRslPercents(10)
                .setOutRslPercents(11)
                .setActivatedAt(date("01/01/2000"))
        );
        Assertions.assertThat(mskuRsls).containsExactly(
            new MskuRsl().setMskuId(987654321L)
                .setInRslPercents(15)
                .setOutRslPercents(20)
                .setActivatedAt(date("01/01/2001")),
            new MskuRsl().setMskuId(145L)
                .setInRslDays(20)
                .setOutRslDays(30)
                .setActivatedAt(date("01/01/2000")),
            new MskuRsl().setMskuId(145L)
                .setInRslDays(30)
                .setOutRslDays(40)
                .setActivatedAt(date("01/01/2001"))
        );
        Assertions.assertThat(sskuRsls).containsExactly(
            new SskuRsl().setSupplierId(6666).setShopSku("ololo")
                .setInRslPercents(25)
                .setOutRslPercents(20)
                .setActivatedAt(date("01/01/2002")),
            new SskuRsl().setSupplierId(888).setShopSku("coorly")
                .setInRslPercents(12)
                .setOutRslPercents(13)
                .setActivatedAt(date("01/01/2000")),
            new SskuRsl().setSupplierId(888).setShopSku("coorly")
                .setInRslPercents(16)
                .setOutRslPercents(20)
                .setActivatedAt(date("21/01/2012"))
        );
    }

    @Test
    public void testImportValidWithMultidatesDisabled() throws IOException {
        keyValueService.putValue(MdmProperties.RSL_MULTIDATES_ENABLED_KEY, false);

        prepareMapping(5678, 987654321L, 1, "boom");
        prepareMapping(5678, 100500L, 6666, "ololo");
        prepareMapping(91011, 145L, 100500, "boom");
        prepareMapping(121314, 100500L, 888, "coorly");

        String file = "rsl/rsl-import-no-multidates-test.xlsx";
        byte[] fileBytes = readResource(file);
        var res = importExcelService.importExcel(file, fileBytes);

        List<CategoryRsl> categoryRsls = categoryRslRepository.findAll();
        List<MskuRsl> mskuRsls = mskuRslRepository.findAll();
        List<SskuRsl> sskuRsls = sskuRslRepository.findAll();

        Assertions.assertThat(categoryRsls).containsExactly(
            new CategoryRsl().setCategoryId(1234L)
                .setInRslDays(10)
                .setOutRslDays(20)
                .setActivatedAt(date("01/01/2000")),
            new CategoryRsl().setCategoryId(5678L)
                .setInRslDays(5)
                .setOutRslDays(7)
                .setActivatedAt(date("01/01/2000")),
            new CategoryRsl().setCategoryId(91011L)
                .setInRslDays(10)
                .setOutRslDays(20)
                .setActivatedAt(date("01/01/2000")),
            new CategoryRsl().setCategoryId(121314L)
                .setInRslPercents(10)
                .setOutRslPercents(11)
                .setActivatedAt(date("21/01/2012"))
        );
        Assertions.assertThat(mskuRsls).containsExactly(
            new MskuRsl().setMskuId(987654321L)
                .setInRslPercents(15)
                .setOutRslPercents(20)
                .setActivatedAt(date("01/01/2000")),
            new MskuRsl().setMskuId(145L)
                .setInRslDays(30)
                .setOutRslDays(40)
                .setActivatedAt(date("01/01/2000"))
        );
        Assertions.assertThat(sskuRsls).containsExactly(
            new SskuRsl().setSupplierId(6666).setShopSku("ololo")
                .setInRslPercents(25)
                .setOutRslPercents(20)
                .setActivatedAt(date("01/01/2000")),
            new SskuRsl().setSupplierId(888).setShopSku("coorly")
                .setInRslPercents(12)
                .setOutRslPercents(13)
                .setActivatedAt(date("21/01/2012"))
        );
    }

    @Test
    public void whenNumberOfMskusAndSskusInCategoriesExceedsLimitShouldReturnError() throws IOException {
        keyValueService.putValue(MdmProperties.ACCEPTABLE_AMOUNT_OF_ENTITIES_FOR_GOLDEN_RECALC, 1);

        CategoryRsl categoryRsl = new CategoryRsl().setCategoryId(12345L).setInRslDays(30).setOutRslDays(40)
            .setActivatedAt(LocalDate.parse("15/10/2019", RslExcelSheetConfig.DATE_TIME_FORMATTER));
        categoryRslRepository.insert(categoryRsl);

        String file = "rsl/rsl-import-test.xlsx";
        byte[] fileBytes = readResource(file);
        ImportResult importResult = importExcelService.importExcel(file, fileBytes);

        Assertions.assertThat(importResult.getErrors()).containsExactlyInAnyOrder(
            "Количество msku/ssku на пересчет в загружаемых категориях превышает допустимый порог");

        // провереям, что настройки из загружаемого файла не сохранились, так как возникла ошибка валидации
        Assertions.assertThat(categoryRslRepository.findAll()).containsExactlyInAnyOrder(categoryRsl);
    }

    private LocalDate date(String value) {
        return LocalDate.parse(value, RslExcelSheetConfig.DATE_TIME_FORMATTER);
    }

    private MappingInfoLite mapping(long category, long model, int supplierId, String shopSku) {
        return MappingInfoLite.newBuilder()
            .setCategoryId(category)
            .setModelId(model)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .build();
    }
}

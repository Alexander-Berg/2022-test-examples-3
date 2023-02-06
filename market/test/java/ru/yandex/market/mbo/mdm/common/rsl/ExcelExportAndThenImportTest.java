package ru.yandex.market.mbo.mdm.common.rsl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.CategoryRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.MskuRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SskuRsl;

/**
 * @author dmserebr
 * @date 28/11/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ExcelExportAndThenImportTest extends RslExcelTestBase {
    @Test
    public void testExportAndThenImport() {
        prepareMapping(12345, 3L, 2, "ssku1");

        categoryRslRepository.insert(new CategoryRsl().setCategoryId(12345L).setInRslDays(3).setOutRslPercents(5));
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(123456L).setInRslDays(4).setInRslPercents(12)
            .setOutRslPercents(5));
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(123456L).setInRslDays(1).setOutRslPercents(2)
            .setActivatedAt(LocalDate.now().minus(3, ChronoUnit.DAYS)));
        mskuRslRepository.insert(new MskuRsl().setMskuId(3L).setInRslDays(5).setInRslPercents(40)
            .setOutRslPercents(30));
        mskuRslRepository.insert(new MskuRsl().setMskuId(4L).setInRslDays(5).setOutRslDays(40));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(2).setShopSku("ssku").setInRslDays(40).setOutRslDays(40));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(2).setShopSku("ssku1").setInRslDays(40).setOutRslDays(40));

        byte[] excelAsByteArray = excelExportService.exportExcelAsByteArray();

        categoryRslRepository.deleteAll();
        mskuRslRepository.deleteAll();
        sskuRslRepository.deleteAll();

        importExcelService.importExcel("excelFile", excelAsByteArray);

        Assertions.assertThat(categoryRslRepository.findAll()).containsExactlyInAnyOrder(
            new CategoryRsl().setCategoryId(12345L).setInRslDays(3).setOutRslPercents(5),
            new CategoryRsl().setCategoryId(123456L).setInRslDays(4).setInRslPercents(12).setOutRslPercents(5),
            new CategoryRsl().setCategoryId(123456L).setInRslDays(1).setOutRslPercents(2)
                .setActivatedAt(LocalDate.now().minus(3, ChronoUnit.DAYS))
        );
        Assertions.assertThat(mskuRslRepository.findAll()).containsExactlyInAnyOrder(
            new MskuRsl().setMskuId(3L).setInRslDays(5).setInRslPercents(40).setOutRslPercents(30),
            new MskuRsl().setMskuId(4L).setInRslDays(5).setOutRslDays(40)
        );
        Assertions.assertThat(sskuRslRepository.findAll()).containsExactlyInAnyOrder(
            new SskuRsl().setSupplierId(2).setShopSku("ssku").setInRslDays(40).setOutRslDays(40),
            new SskuRsl().setSupplierId(2).setShopSku("ssku1").setInRslDays(40).setOutRslDays(40)
        );
    }
}

package ru.yandex.market.deepmind.common.services.tracker_approver.excel;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.deepmind.common.DeepmindBaseEnhancedTrackerApproverExecutorTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.services.tracker_strategy.FromUserExcelComposerMock;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;

import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.FIRST_DELIVERY_RELATIVE_AVERAGE_SALES_IN_YEAR_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.MARGIN_3P_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.MARGIN_MINREF_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.PRICE_3P_MED_30DAYS_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.PURCHASE_PRICE_RELATIVE_3P_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.PURCHASE_PRICE_RELATIVE_MINREF_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.SOFINO_AUTOSALE_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.SOFINO_DEADSTOCK_SINCE_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.SOFINO_KGT_AUTOSALE_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.SOFINO_KGT_DEADSTOCK_SINCE_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.TURNOVER_RELATIVE_AVERAGE_SELLS_IN_60_DAYS_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.UE_CALCULATED_KEY;
import static ru.yandex.market.deepmind.common.services.tracker_approver.excel.Headers.UE_PLANNED_KEY;

public class AutoOkRuleExcelValidatorTest extends DeepmindBaseEnhancedTrackerApproverExecutorTestClass {

    private AutoOkRuleExcelValidator autoOkRuleExcelValidator;


    @Before
    public void setUp() {
        super.setUp();

        MasterDataHelperService masterDataHelperService = Mockito.mock(MasterDataHelperService.class);
        Mockito.when(masterDataHelperService.findSskuMasterData(Mockito.any()))
            .thenReturn(List.of(
                new MasterData().setSupplierId(111).setShopSku("shop-sku-111").setMinShipment(1).setQuantumOfSupply(1),
                new MasterData().setSupplierId(222).setShopSku("shop-sku-222").setMinShipment(2).setQuantumOfSupply(2),
                new MasterData().setSupplierId(333).setShopSku("shop-sku-333").setMinShipment(3).setQuantumOfSupply(3),
                new MasterData().setSupplierId(444).setShopSku("shop-sku-444").setMinShipment(4).setQuantumOfSupply(4)
            ));

        sskuStatusRepository.save(
            sskuStatus(111, "shop-sku-111", OfferAvailability.INACTIVE, "comment1"),
            sskuStatus(222, "shop-sku-222", OfferAvailability.DELISTED, "comment2"),
            sskuStatus(333, "shop-sku-333", OfferAvailability.DELISTED, "comment3"),
            sskuStatus(444, "shop-sku-444", OfferAvailability.DELISTED, "comment4")

        );

        autoOkRuleExcelValidator = new AutoOkRuleExcelValidator(FromUserExcelComposerMock.FIRST_ROW_ID);
    }

    @Test
    public void allRulesFail() {
        var list = List.of(
            new ServiceOfferKey(111, "shop-sku-111")
        );
        var excel = createCorrectExcelFile(list, EnrichApproveToPendingExcelComposer.HEADERS);
        var builder = excel.toBuilder();
        builder.setValue(2, FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY, 0.8);
        builder.setValue(2, FIRST_DELIVERY_RELATIVE_AVERAGE_SALES_IN_YEAR_KEY, 1.24);
        builder.setValue(2, MARGIN_3P_KEY, -0.6);
        builder.setValue(2, MARGIN_MINREF_KEY, -0.7);
        builder.setValue(2, PRICE_3P_MED_30DAYS_KEY, "brbrbr");
        builder.setValue(2, PURCHASE_PRICE_RELATIVE_3P_KEY, 0.34);
        builder.setValue(2, PURCHASE_PRICE_RELATIVE_MINREF_KEY, 0.28);
        builder.setValue(2, SOFINO_AUTOSALE_KEY, true);
        builder.setValue(2, SOFINO_DEADSTOCK_SINCE_KEY, LocalDate.now());
        builder.setValue(2, SOFINO_KGT_AUTOSALE_KEY, true);
        builder.setValue(2, SOFINO_KGT_DEADSTOCK_SINCE_KEY, LocalDate.now());
        builder.setValue(2, TURNOVER_RELATIVE_AVERAGE_SELLS_IN_60_DAYS_KEY, 50);
        builder.setValue(2, UE_CALCULATED_KEY, -100);
        builder.setValue(2, UE_PLANNED_KEY, -10);

        var result = autoOkRuleExcelValidator.applyAutoOkRules(builder.build());

        Assertions.assertThat(result).usingElementComparatorIgnoringFields("text", "key").containsExactlyInAnyOrder(
            Error(excel, 2, FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY),
            Error(excel, 2, FIRST_DELIVERY_RELATIVE_AVERAGE_SALES_IN_YEAR_KEY),
            Error(excel, 2, MARGIN_3P_KEY),
            Error(excel, 2, MARGIN_MINREF_KEY),
            Error(excel, 2, PRICE_3P_MED_30DAYS_KEY),
            Error(excel, 2, PURCHASE_PRICE_RELATIVE_3P_KEY),
            Error(excel, 2, PURCHASE_PRICE_RELATIVE_MINREF_KEY),
            Error(excel, 2, SOFINO_AUTOSALE_KEY),
            Error(excel, 2, SOFINO_DEADSTOCK_SINCE_KEY),
            Error(excel, 2, SOFINO_KGT_AUTOSALE_KEY),
            Error(excel, 2, SOFINO_KGT_DEADSTOCK_SINCE_KEY),
            Error(excel, 2, TURNOVER_RELATIVE_AVERAGE_SELLS_IN_60_DAYS_KEY),
            Error(excel, 2, UE_CALCULATED_KEY),
            Error(excel, 2, UE_PLANNED_KEY),
            Error(excel, 2, UE_CALCULATED_KEY),
            Error(excel, 2, UE_PLANNED_KEY)
        );
    }

    @Test
    public void allRulesOk() {
        var list = List.of(
            new ServiceOfferKey(111, "shop-sku-111")
        );
        var excel = createCorrectExcelFile(list, EnrichApproveToPendingExcelComposer.HEADERS);
        var builder = excel.toBuilder();
        builder.setValue(2, FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY, 0.4);
        builder.setValue(2, FIRST_DELIVERY_RELATIVE_AVERAGE_SALES_IN_YEAR_KEY, 0.24);
        builder.setValue(2, MARGIN_3P_KEY, 0.6);
        builder.setValue(2, MARGIN_MINREF_KEY, 0.7);
        builder.setValue(2, PRICE_3P_MED_30DAYS_KEY, "987.5876");
        builder.setValue(2, PURCHASE_PRICE_RELATIVE_3P_KEY, -0.34);
        builder.setValue(2, PURCHASE_PRICE_RELATIVE_MINREF_KEY, -0.28);
        builder.setValue(2, SOFINO_AUTOSALE_KEY, false);
        builder.setValue(2, SOFINO_DEADSTOCK_SINCE_KEY, null);
        builder.setValue(2, SOFINO_KGT_AUTOSALE_KEY, false);
        builder.setValue(2, SOFINO_KGT_DEADSTOCK_SINCE_KEY, null);
        builder.setValue(2, TURNOVER_RELATIVE_AVERAGE_SELLS_IN_60_DAYS_KEY, 29);
        builder.setValue(2, UE_CALCULATED_KEY, 100);
        builder.setValue(2, UE_PLANNED_KEY, 9);

        var result = autoOkRuleExcelValidator.applyAutoOkRules(builder.build());

        Assertions.assertThat(result.size()).isZero();
    }

    @Test
    public void allRulesOkFileWithLegend() {
        var list = List.of(
            new ServiceOfferKey(111, "shop-sku-111")
        );
        var excel = createCorrectExcelFileWithLegend(list, EnrichApproveToPendingExcelComposer.HEADERS);
        var builder = excel.toBuilder();
        builder.setValue(2, FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY, 0.4);
        builder.setValue(2, FIRST_DELIVERY_RELATIVE_AVERAGE_SALES_IN_YEAR_KEY, 0.24);
        builder.setValue(2, MARGIN_3P_KEY, 0.6);
        builder.setValue(2, MARGIN_MINREF_KEY, 0.7);
        builder.setValue(2, PRICE_3P_MED_30DAYS_KEY, "987.5876");
        builder.setValue(2, PURCHASE_PRICE_RELATIVE_3P_KEY, -0.34);
        builder.setValue(2, PURCHASE_PRICE_RELATIVE_MINREF_KEY, -0.28);
        builder.setValue(2, SOFINO_AUTOSALE_KEY, false);
        builder.setValue(2, SOFINO_DEADSTOCK_SINCE_KEY, null);
        builder.setValue(2, SOFINO_KGT_AUTOSALE_KEY, false);
        builder.setValue(2, SOFINO_KGT_DEADSTOCK_SINCE_KEY, null);
        builder.setValue(2, TURNOVER_RELATIVE_AVERAGE_SELLS_IN_60_DAYS_KEY, 29);
        builder.setValue(2, UE_CALCULATED_KEY, 100);
        builder.setValue(2, UE_PLANNED_KEY, 9);

        var result = autoOkRuleExcelValidator.applyAutoOkRules(builder.build());

        Assertions.assertThat(result.size()).isZero();
    }

    @Test
    public void uePlannedCheckFailed() {
        var list = List.of(
            new ServiceOfferKey(111, "shop-sku-111")
        );
        var excel = createCorrectExcelFile(list, EnrichApproveToPendingExcelComposer.HEADERS);
        var builder = excel.toBuilder();
        builder.setValue(2, FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY, 0.4);
        builder.setValue(2, FIRST_DELIVERY_RELATIVE_AVERAGE_SALES_IN_YEAR_KEY, 0.24);
        builder.setValue(2, MARGIN_3P_KEY, 0.6);
        builder.setValue(2, MARGIN_MINREF_KEY, 0.7);
        builder.setValue(2, PRICE_3P_MED_30DAYS_KEY, "987.5876");
        builder.setValue(2, PURCHASE_PRICE_RELATIVE_3P_KEY, -0.34);
        builder.setValue(2, PURCHASE_PRICE_RELATIVE_MINREF_KEY, -0.28);
        builder.setValue(2, SOFINO_AUTOSALE_KEY, false);
        builder.setValue(2, SOFINO_DEADSTOCK_SINCE_KEY, null);
        builder.setValue(2, SOFINO_KGT_AUTOSALE_KEY, false);
        builder.setValue(2, SOFINO_KGT_DEADSTOCK_SINCE_KEY, null);
        builder.setValue(2, TURNOVER_RELATIVE_AVERAGE_SELLS_IN_60_DAYS_KEY, 29);
        builder.setValue(2, UE_CALCULATED_KEY, 100);
        builder.setValue(2, UE_PLANNED_KEY, -5);

        var result = autoOkRuleExcelValidator.applyAutoOkRules(builder.build());

        Assertions.assertThat(result.size()).isOne();
        Error(excel, 2, UE_PLANNED_KEY);
    }

    @Test
    public void uePlannedAllCheckFailed() {
        var list = List.of(
            new ServiceOfferKey(111, "shop-sku-111")
        );
        var excel = createCorrectExcelFile(list, EnrichApproveToPendingExcelComposer.HEADERS);
        var builder = excel.toBuilder();
        builder.setValue(2, FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY, 0.4);
        builder.setValue(2, FIRST_DELIVERY_RELATIVE_AVERAGE_SALES_IN_YEAR_KEY, 0.24);
        builder.setValue(2, MARGIN_3P_KEY, 0.6);
        builder.setValue(2, MARGIN_MINREF_KEY, 0.7);
        builder.setValue(2, PRICE_3P_MED_30DAYS_KEY, "987.5876");
        builder.setValue(2, PURCHASE_PRICE_RELATIVE_3P_KEY, -0.34);
        builder.setValue(2, PURCHASE_PRICE_RELATIVE_MINREF_KEY, -0.28);
        builder.setValue(2, SOFINO_AUTOSALE_KEY, false);
        builder.setValue(2, SOFINO_DEADSTOCK_SINCE_KEY, null);
        builder.setValue(2, SOFINO_KGT_AUTOSALE_KEY, false);
        builder.setValue(2, SOFINO_KGT_DEADSTOCK_SINCE_KEY, null);
        builder.setValue(2, TURNOVER_RELATIVE_AVERAGE_SELLS_IN_60_DAYS_KEY, 29);
        builder.setValue(2, UE_CALCULATED_KEY, -6);
        builder.setValue(2, UE_PLANNED_KEY, -5);

        var result = autoOkRuleExcelValidator.applyAutoOkRules(builder.build());

        Assertions.assertThat(result.size()).isEqualTo(4);
        Error(excel, 2, UE_PLANNED_KEY);
        Error(excel, 2, UE_CALCULATED_KEY);
        Error(excel, 2, UE_PLANNED_KEY);
        Error(excel, 2, UE_CALCULATED_KEY);

    }

    @Test
    public void checkRuleBoundaryTest() {
        var list = List.of(
            new ServiceOfferKey(111, "shop-sku-111")
        );
        var excel = createCorrectExcelFile(list, EnrichApproveToPendingExcelComposer.HEADERS);
        var builder = excel.toBuilder();
        builder.setValue(2, FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY, 0.5);
        builder.setValue(2, FIRST_DELIVERY_RELATIVE_AVERAGE_SALES_IN_YEAR_KEY, 0.5);
        builder.setValue(2, MARGIN_3P_KEY, 0);
        builder.setValue(2, MARGIN_MINREF_KEY, 0);
        builder.setValue(2, PRICE_3P_MED_30DAYS_KEY, "987.5876");
        builder.setValue(2, PURCHASE_PRICE_RELATIVE_3P_KEY, 0);
        builder.setValue(2, PURCHASE_PRICE_RELATIVE_MINREF_KEY, 0);
        builder.setValue(2, SOFINO_AUTOSALE_KEY, false);
        builder.setValue(2, SOFINO_DEADSTOCK_SINCE_KEY, null);
        builder.setValue(2, SOFINO_KGT_AUTOSALE_KEY, false);
        builder.setValue(2, SOFINO_KGT_DEADSTOCK_SINCE_KEY, null);
        builder.setValue(2, TURNOVER_RELATIVE_AVERAGE_SELLS_IN_60_DAYS_KEY, 30);
        builder.setValue(2, UE_CALCULATED_KEY, 0);
        builder.setValue(2, UE_PLANNED_KEY, 0);

        var result = autoOkRuleExcelValidator.applyAutoOkRules(builder.build());

        Assertions.assertThat(result).usingElementComparatorIgnoringFields("text", "key")
            .containsExactlyInAnyOrder(
            Error(excel, 2, FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY),
            Error(excel, 2, FIRST_DELIVERY_RELATIVE_AVERAGE_SALES_IN_YEAR_KEY),
            Error(excel, 2, UE_CALCULATED_KEY),
            Error(excel, 2, UE_PLANNED_KEY)
        );
    }

    @Test
    public void checkExcelReadingTest() {
        var excel = getExcelFrom("excel_files/to_pending_01_06_2022.xlsx");

        var result = autoOkRuleExcelValidator.applyAutoOkRules(excel);

        Assertions.assertThat(result).usingElementComparatorIgnoringFields("text", "key")
            .containsExactlyInAnyOrder(
            Error(excel, 7, FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY),
            Error(excel, 8, FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY),
            Error(excel, 9, FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY),
            Error(excel, 10, FIRST_DELIVERY_RELATIVE_ALL_SALES_IN_60_DAYS_KEY),

            Error(excel, 8, FIRST_DELIVERY_RELATIVE_AVERAGE_SALES_IN_YEAR_KEY),

            Error(excel, 8, TURNOVER_RELATIVE_AVERAGE_SELLS_IN_60_DAYS_KEY),
            Error(excel, 10, TURNOVER_RELATIVE_AVERAGE_SELLS_IN_60_DAYS_KEY),

            Error(excel, 8, PURCHASE_PRICE_RELATIVE_MINREF_KEY),

            Error(excel, 8, MARGIN_MINREF_KEY),

            Error(excel, 7, UE_PLANNED_KEY),
            Error(excel, 8, UE_PLANNED_KEY),
            Error(excel, 9, UE_PLANNED_KEY),
            Error(excel, 10, UE_PLANNED_KEY),

            Error(excel, 7, UE_CALCULATED_KEY),
            Error(excel, 8, UE_CALCULATED_KEY),
            Error(excel, 9, UE_CALCULATED_KEY),
            Error(excel, 10, UE_CALCULATED_KEY)
        );
    }


    private Error Error(ExcelFile excel, int row, String header) {
        return new Error(row, excel.getColumnIndex(header), "");
    }

}

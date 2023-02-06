package ru.yandex.market.wms.receiving.service.report.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.cte.client.FulfillmentCteClientApi;
import ru.yandex.market.logistics.cte.client.dto.GetUnitsRequestDTO;
import ru.yandex.market.logistics.cte.client.dto.GetUnitsResponseDTO;
import ru.yandex.market.logistics.cte.client.dto.QualityAttributeDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemUUIDResponseDTO;
import ru.yandex.market.logistics.cte.client.dto.SupplyItemWithAttributesDTO;
import ru.yandex.market.logistics.cte.client.dto.UnitDTO;
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeType;
import ru.yandex.market.logistics.cte.client.enums.StockType;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.model.dto.report.BaseDiscrepanciesReportData;
import ru.yandex.market.wms.receiving.model.dto.report.DiscrepanciesPerBoxReportData;
import ru.yandex.market.wms.receiving.model.dto.report.DiscrepanciesPerItemReportData;
import ru.yandex.market.wms.receiving.model.dto.report.DiscrepanciesPerPalletReportData;
import ru.yandex.market.wms.receiving.model.dto.report.DiscrepancyReportType;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.receiving.service.report.impl.DiscrepanciesReportDataService.GENERATION_PER_PALLET_CONFIG;

class DiscrepanciesReportDataServiceTest extends ReceivingIntegrationTest {

    private static final String DEFAULT_WH_ADDRESS =
            "!Необходимо добавить в wmwhse1.NSQLCONFIG настройку с адресом склада!";

    @Autowired
    private DiscrepanciesReportDataService discrepanciesReportDataService;
    @MockBean
    @Autowired
    private FulfillmentCteClientApi cteClientApi;
    @MockBean
    @Autowired
    private DbConfigService configService;

    @Test
    @DatabaseSetup("/service/discrepancies/receipt-and-details.xml")
    void getReportDto() {
        //given
        when(cteClientApi.getQualityAttributesForUnitLabels(eq(343722L), any(GetUnitsRequestDTO.class)))
                .thenReturn(unitsResponseDTO(List.of("PLT122", "PLT123", "PLT124")));

        //when
        when(configService.getConfigAsBoolean(GENERATION_PER_PALLET_CONFIG)).thenReturn(false);
        var reportData = (DiscrepanciesPerBoxReportData) discrepanciesReportDataService.getReportData("0000000411");

        //then
        assertThat("Discrepancies", reportData.getDiscrepancyList().size(), equalTo(2));
        var plt122 = extractByBarcode(reportData.getDiscrepancyList(), "PLT122");
        var plt123 = extractByBarcode(reportData.getDiscrepancyList(), "PLT123");
        var plt124 = extractByBarcode(reportData.getDiscrepancyList(), "PLT124");
        assertThat("PLT122", plt122, nullValue());
        assertThat("PLT123", plt123.getDiscrepancyType(), equalTo("Недостача"));
        assertThat("PLT123", plt123.getValue(), equalTo(1));
        assertThat("PLT123", plt123.getExternalNumber(), equalTo("ext_126"));
        assertThat("PLT124", plt124.getDiscrepancyType(), equalTo("Недостача"));
        assertThat("PLT124", plt124.getValue(), equalTo(2));
        assertThat("PLT124", plt124.getExternalNumber(), equalTo("ext_127"));

        var summaries = reportData.getDiscrepancySummaries();
        assertThat("Summaries", summaries.size(), equalTo(1));
        assertThat("Тип недостача", summaries.get(0).getName(), equalTo("Итого: не принято (Недостача)"));
        assertThat("Количество недостач", summaries.get(0).getValue(), equalTo(3));

        var qualityAttributes = reportData.getQualityAttributes();
        assertThat("Quality attributes length", qualityAttributes.size(), equalTo(3));
        BaseDiscrepanciesReportData.QualityAttribute qualityAttribute1 = qualityAttributes.get(0);
        BaseDiscrepanciesReportData.QualityAttribute qualityAttribute2 = qualityAttributes.get(1);
        BaseDiscrepanciesReportData.QualityAttribute qualityAttribute3 = qualityAttributes.get(2);
        assertThat("Quality attributes 1", qualityAttribute1.getDispatchNumber(), equalTo("PLT122"));
        assertThat("Quality attributes 1", qualityAttribute1.getAttributes(), equalTo("title1"));
        assertThat("Quality attributes 2", qualityAttribute2.getDispatchNumber(), equalTo("PLT123"));
        assertThat("Quality attributes 2", qualityAttribute2.getAttributes(), equalTo("title2"));
        assertThat("Quality attributes 3", qualityAttribute3.getDispatchNumber(), equalTo("PLT124"));
        assertThat("Quality attributes 3", qualityAttribute3.getAttributes(), equalTo("title3"));
    }

    @Test
    @DatabaseSetup("/service/discrepancies/receipt-and-details.xml")
    void getReportByPalletDto() {
        //given
        when(cteClientApi.getQualityAttributesForUnitLabels(eq(343722L), any(GetUnitsRequestDTO.class)))
                .thenReturn(unitsResponseDTO(List.of("PLT122", "PLT125")));

        //when
        when(configService.getConfig("YM_WH_LEGAL_ADDRESS", DEFAULT_WH_ADDRESS)).thenReturn(DEFAULT_WH_ADDRESS);
        when(configService.getConfigAsBoolean(GENERATION_PER_PALLET_CONFIG)).thenReturn(true);
        var reportData = (DiscrepanciesPerPalletReportData) discrepanciesReportDataService.getReportData("0000000411");

        //then
        assertSoftly(assertions -> {
            assertions.assertThat(reportData.getDiscrepancyList().size()).as("Discrepancies").isEqualTo(1);
            assertions.assertThat(reportData.getWarehouseAddress()).as("WarehouseAddress")
                    .isEqualTo("!Необходимо добавить в wmwhse1.NSQLCONFIG настройку с адресом склада!");
            assertions.assertThat(reportData.getAgentName()).as("Agent name")
                    .isEqualTo("_________________________");
            assertions.assertThat(reportData.getCarrierName()).as("Carrier name").isEqualTo("Carrier name");
            assertions.assertThat(reportData.getShipFromAddress()).as("Ship from address")
                    .isEqualTo("Ship from address");
            var plt122 = extractByBarcode(reportData.getDiscrepancyList(), "PLT122");
            var plt123 = extractByBarcode(reportData.getDiscrepancyList(), "PLT123");
            var plt124 = extractByBarcode(reportData.getDiscrepancyList(), "PLT124");
            var plt125 = extractByBarcode(reportData.getDiscrepancyList(), "PLT125");
            assertions.assertThat(plt122).as("PLT122").isNull();
            assertions.assertThat(plt123).as("PLT123").isNull();
            assertions.assertThat(plt124).as("PLT124").isNull();
            assertions.assertThat(plt125.getDiscrepancyType()).as("PLT125").isEqualTo("Недостача");
            assertions.assertThat(plt125.getValue()).as("PLT125").isEqualTo(1);
            assertions.assertThat(plt125.getExternalNumber()).as("PLT125").isEqualTo("ext_128");

            var summaries = reportData.getDiscrepancySummaries();
            assertions.assertThat(summaries.size()).as("Summaries").isEqualTo(1);
            assertions.assertThat(summaries.get(0).getName()).as("Тип недостача")
                    .isEqualTo("Итого: не принято (Недостача)");
            assertions.assertThat(summaries.get(0).getValue()).as("Количество недостач").isEqualTo(1);

            var qualityAttributes = reportData.getQualityAttributes();
            assertions.assertThat(qualityAttributes.size()).as("Quality attributes length").isEqualTo(2);
            BaseDiscrepanciesReportData.QualityAttribute qualityAttribute1 = qualityAttributes.get(0);
            BaseDiscrepanciesReportData.QualityAttribute qualityAttribute2 = qualityAttributes.get(1);
            assertions.assertThat(qualityAttribute1.getDispatchNumber()).as("Quality attributes 1").isEqualTo("PLT122");
            assertions.assertThat(qualityAttribute1.getAttributes()).as("Quality attributes 1").isEqualTo("title1");
            assertions.assertThat(qualityAttribute2.getDispatchNumber()).as("Quality attributes 2").isEqualTo("PLT125");
            assertions.assertThat(qualityAttribute2.getAttributes()).as("Quality attributes 2").isEqualTo("title2");
        });
    }


    @Test
    @DatabaseSetup("/service/discrepancies/receipt-and-details-items.xml")
    void getReportByItemDto() {
        //given
        when(cteClientApi.getSupplyItemsBySupplyId(eq(343722L)))
                .thenReturn(itemsResponseDTO());

        //when
        when(configService.getConfig("YM_WH_LEGAL_ADDRESS", DEFAULT_WH_ADDRESS))
                .thenReturn(DEFAULT_WH_ADDRESS);
        when(configService.getConfigAsBoolean(GENERATION_PER_PALLET_CONFIG)).thenReturn(true);
        var reportDto = (DiscrepanciesPerItemReportData) discrepanciesReportDataService.getReportData("0000000411",
                DiscrepancyReportType.SECONDARY);

        //then
        assertSoftly(assertions -> {
            assertions.assertThat(reportDto.getWarehouseAddress()).as("WarehouseAddress")
                    .isEqualTo("!Необходимо добавить в wmwhse1.NSQLCONFIG настройку с адресом склада!");
            assertions.assertThat(reportDto.getAgentName()).as("Agent name")
                    .isEqualTo("_________________________");
            assertions.assertThat(reportDto.getCarrierName()).as("Carrier name").isEqualTo("Carrier name");
            assertions.assertThat(reportDto.getShipFromAddress()).as("Ship from address")
                    .isEqualTo("Ship from address");

            var itemsWithAttributes = reportDto.getItemsWithAttributes();

            assertions.assertThat(itemsWithAttributes.size()).as("Items with attributes").isEqualTo(2);
            var item1 = itemsWithAttributes.get(0);
            var item2 = itemsWithAttributes.get(1);

            assertions.assertThat(item1.getDispatchNumber()).as("Dispatch Number 1").isEqualTo("343722");
            assertions.assertThat(item1.getRegistryNumber()).as("Registry Number 1").isEqualTo("233233");
            assertions.assertThat(item1.getSku()).as("Sku 1").isEqualTo("Товар");
            assertions.assertThat(item1.getCount()).as("Count 1").isEqualTo(1);
            assertions.assertThat(item1.getAttributes()).as("Attributes 1")
                    .isEqualTo("Царапины,Замятия,Разбит дисплей");

            assertions.assertThat(item2.getDispatchNumber()).as("Dispatch Number 2").isEqualTo("343722");
            assertions.assertThat(item2.getRegistryNumber()).as("Registry Number 2").isEqualTo("233233");
            assertions.assertThat(item2.getSku()).as("Sku 2").isEqualTo("Товар");
            assertions.assertThat(item2.getCount()).as("Count 2").isEqualTo(2);
            assertions.assertThat(item2.getAttributes()).as("Attributes 2")
                    .isEqualTo("Недостача");
        });
    }

    @Test
    @DatabaseSetup("/service/discrepancies/receipt-and-details.xml")
    void getReportForUPDATABLE_CUSTOMER_RETURN() {
        //given
        when(cteClientApi.getQualityAttributesForUnitLabels(eq(343722L), any(GetUnitsRequestDTO.class)))
                .thenReturn(unitsResponseDTO(List.of("PLT122", "PLT123", "PLT124")));

        //when
        when(configService.getConfigAsBoolean(GENERATION_PER_PALLET_CONFIG)).thenReturn(false);
        var reportDto = (DiscrepanciesPerBoxReportData) discrepanciesReportDataService.getReportData("0000000419");

        var summaries = reportDto.getDiscrepancySummaries();

        var qualityAttributes = reportDto.getQualityAttributes();
        assertThat("Quality attributes length", qualityAttributes.size(), equalTo(3));
        var qualityAttribute1 = qualityAttributes.get(0);
        var qualityAttribute2 = qualityAttributes.get(1);
        var qualityAttribute3 = qualityAttributes.get(2);
        assertThat("Quality attributes 1", qualityAttribute1.getDispatchNumber(), equalTo("PLT122"));
        assertThat("Quality attributes 1", qualityAttribute1.getAttributes(), equalTo("title1"));
        assertThat("Quality attributes 2", qualityAttribute2.getDispatchNumber(), equalTo("PLT123"));
        assertThat("Quality attributes 2", qualityAttribute2.getAttributes(), equalTo("title2"));
        assertThat("Quality attributes 3", qualityAttribute3.getDispatchNumber(), equalTo("PLT124"));
        assertThat("Quality attributes 3", qualityAttribute3.getAttributes(), equalTo("title3"));
    }

    private GetUnitsResponseDTO unitsResponseDTO(List<String> barcodes) {
        AtomicInteger count = new AtomicInteger(0);
        return new GetUnitsResponseDTO(barcodes.stream().map(it -> unit(count.incrementAndGet(), it)).toList());
    }

    private SupplyItemUUIDResponseDTO itemsResponseDTO() {
        return new SupplyItemUUIDResponseDTO(List.of(
                new SupplyItemWithAttributesDTO(
                        1,
                        null,
                        "uuid7",
                        10264169,
                        "4565467",
                        "45674567",
                        6,
                        StockType.DAMAGE_RESELL,
                        "shopSku3",
                        "ROVSku3",
                        "222",
                        "Царапины,Замятия,Разбит дисплей",
                        10,
                        "ff_supply_id",
                        "171",
                        LocalDateTime.of(2021, 12, 8, 10, 55, 45)
                )));
    }

    private UnitDTO unit(long id, String barcode) {
        return new UnitDTO(barcode, newArrayList(getQualityAttributeDTO(id)));
    }

    @NotNull
    private QualityAttributeDTO getQualityAttributeDTO(long id) {
        return new QualityAttributeDTO(id, "name", "title" + id,
                "ref" + id, QualityAttributeType.TRANSPORTATION_UNIT, "description");
    }

    private BaseDiscrepanciesReportData.Discrepancy extractByBarcode(
            List<BaseDiscrepanciesReportData.Discrepancy> discrepancies, String barcode) {
        return discrepancies.stream()
                .filter(ds -> ds.getDispatchNumber().equals(barcode))
                .findFirst().orElse(null);
    }
}

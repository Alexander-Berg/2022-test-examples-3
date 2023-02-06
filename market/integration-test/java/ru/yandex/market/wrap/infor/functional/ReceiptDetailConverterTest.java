package ru.yandex.market.wrap.infor.functional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenContextHolder;
import ru.yandex.market.wrap.infor.entity.ReceiptDetailItem;
import ru.yandex.market.wrap.infor.entity.util.Pagination;
import ru.yandex.market.wrap.infor.model.InboundReceiptDetailKey;
import ru.yandex.market.wrap.infor.model.yt.ReceiptDetailRow;
import ru.yandex.market.wrap.infor.model.yt.converter.ReceiptDetailConverter;
import ru.yandex.market.wrap.infor.service.inbound.PageableInboundReceiptDetailExtractionService;

@DatabaseSetup(
    connection = "wrapConnection",
    value = "classpath:fixtures/functional/get_inbound_details/common/mapping_state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
@DatabaseSetup(
    connection = "wmsConnection",
    value = "classpath:fixtures/functional/get_inbound_details/common/receipt_state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
@DatabaseSetup(
    connection = "wmsConnection",
    value = "classpath:fixtures/functional/get_inbound_details/common/receiptdetail_state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
@DatabaseSetup(
    connection = "wmsConnection",
    value = "classpath:fixtures/functional/get_inbound_details/common/loc_state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
@DatabaseSetup(
    connection = "wmsConnection",
    value = "classpath:fixtures/functional/get_inbound_details/common/billofmaterial_state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
@DatabaseSetup(
    connection = "wmsConnection",
    value = "classpath:fixtures/functional/get_inbound_details/common/sku_state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
@SuppressWarnings("checkstyle:MagicNumber")
class ReceiptDetailConverterTest extends AbstractFunctionalTest {

    @Autowired
    private PageableInboundReceiptDetailExtractionService extractionService;
    @Autowired
    TokenContextHolder tokenContextHolder;

    @BeforeEach
    void prepareContext() {
        tokenContextHolder.setToken("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    }

    /**
     * Проверяет загрузку из базы и конвертацию в модель Yt данных для многоместного товара,
     * у которого заявлено 6 комплектов, принято полностью годных 6, годными излишками - 3, неполных(дефектов) - 3.
     */
    @Test
    void convertToYtModel() {
        List<ReceiptDetailItem> actualDetails =
            extractionService.getDetailsForPage(new Pagination(1, 8L));
        Map<InboundReceiptDetailKey, List<ReceiptDetailItem>> grouped =
            actualDetails.stream().collect(Collectors.groupingBy(ReceiptDetailItem::getKey));
        grouped.forEach((key, items) -> {
            ReceiptDetailRow row = ReceiptDetailConverter.convert(key, items, "147");
            SoftAssertions.assertSoftly(assertions -> {
                assertions.assertThat(row).isNotNull();
                assertions.assertThat(row.getExpectedAmount()).isEqualTo(6);
                assertions.assertThat(row.getReceivedAmount()).isEqualTo(9);
                assertions.assertThat(row.getDefectAmount()).isEqualTo(3);
                assertions.assertThat(row.getSurplusAmount()).isEqualTo(6);
            });
        });
    }

    /**
     * Проверяет загрузку и конвертацию многоместного товара, у частей которого разные заказы на отгрузку.
     */
    @Test
    void convertToYtModelSeveralOrders() {
        List<ReceiptDetailItem> actualDetails =
            extractionService.getDetailsForPage(new Pagination(1, 11L));
        Map<InboundReceiptDetailKey, List<ReceiptDetailItem>> grouped =
            actualDetails.stream().collect(Collectors.groupingBy(ReceiptDetailItem::getKey));
        InboundReceiptDetailKey k1 = new InboundReceiptDetailKey("0000000012", "TST0000000000000000026", "123", null);
        InboundReceiptDetailKey k2 = new InboundReceiptDetailKey("0000000012", "TST0000000000000000026", "123",
            "mehOrder");
        InboundReceiptDetailKey k3 = new InboundReceiptDetailKey("0000000012", "TST0000000000000000026", "123",
            "mehOrder2");

        ReceiptDetailRow row1 = ReceiptDetailConverter.convert(k1, grouped.get(k1), "147");
        ReceiptDetailRow row2 = ReceiptDetailConverter.convert(k2, grouped.get(k2), "147");
        ReceiptDetailRow row3 = ReceiptDetailConverter.convert(k3, grouped.get(k3), "147");
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(row1.getExpectedAmount()).isEqualTo(2);
            assertions.assertThat(row1.getExternalOrderNumber()).isNull();
            assertions.assertThat(row2.getReceivedAmount()).isEqualTo(1);
            assertions.assertThat(row2.getExternalOrderNumber()).isEqualTo("extMehOrder");
            assertions.assertThat(row3.getReceivedAmount()).isEqualTo(1);
            assertions.assertThat(row3.getExternalOrderNumber()).isEqualTo("extMehOrder2");
        });
    }

    /**
     * Проверяет комплексный кейс, когда приходит 2 возврата:
     * с первым заказом принят 1 годный комплект и 1 комплект поврежден
     * со вторым заказом принято 2 комплекта.
     * Это все излишки, т.к. незаявлены и ожидаемое количество 0
     */
    @Test
    void convertToYtModelCompoundWithOrder() {
        List<ReceiptDetailItem> actualDetails =
            extractionService.getDetailsForPage(new Pagination(1, 12L));
        Map<InboundReceiptDetailKey, List<ReceiptDetailItem>> grouped =
            actualDetails.stream().collect(Collectors.groupingBy(ReceiptDetailItem::getKey));

        InboundReceiptDetailKey k1 = new InboundReceiptDetailKey("0000000013", "TST0000000000000000027",
            "123", "order1");
        InboundReceiptDetailKey k2 = new InboundReceiptDetailKey("0000000013", "TST0000000000000000027",
            "123", "order2");

        ReceiptDetailRow row1 = ReceiptDetailConverter.convert(k1, grouped.get(k1), "147");
        ReceiptDetailRow row2 = ReceiptDetailConverter.convert(k2, grouped.get(k2), "147");

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(row1.getExpectedAmount()).isEqualTo(0);
            assertions.assertThat(row1.getReceivedAmount()).isEqualTo(1);
            assertions.assertThat(row1.getDefectAmount()).isEqualTo(1);
            assertions.assertThat(row1.getSurplusAmount()).isEqualTo(2);
            assertions.assertThat(row1.getExternalOrderNumber()).isEqualTo("extOrder1");
            assertions.assertThat(row2.getExpectedAmount()).isEqualTo(0);
            assertions.assertThat(row2.getReceivedAmount()).isEqualTo(2);
            assertions.assertThat(row2.getDefectAmount()).isEqualTo(0);
            assertions.assertThat(row2.getSurplusAmount()).isEqualTo(2);
            assertions.assertThat(row2.getExternalOrderNumber()).isEqualTo("extOrder2");
        });
    }

}

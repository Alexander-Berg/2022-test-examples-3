package ru.yandex.market.wrap.infor.functional;

import java.math.BigDecimal;
import java.util.Collection;
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
import ru.yandex.market.wrap.infor.service.inbound.PageableInboundReceiptDetailExtractionService;

import static ru.yandex.market.wrap.infor.entity.LocStatus.HOLD;
import static ru.yandex.market.wrap.infor.entity.LocStatus.OK;

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
class PageableInboundReceiptDetailExtractionServiceTest extends AbstractFunctionalTest {

    @Autowired
    private PageableInboundReceiptDetailExtractionService extractionService;
    @Autowired
    TokenContextHolder tokenContextHolder;

    @BeforeEach
    void prepareContext() {
        tokenContextHolder.setToken("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    }

    /**
     * Общий хэпи-пас - проверяет работу пагинации и схлопывание одноместного и многоместного товаров
     * без ордеров на отгрузку.
     */
    @Test
    void normalAndIncompleteKitsAreInPagination() {
        List<ReceiptDetailItem> actualDetails =
            extractionService.getDetailsForPage(new Pagination(2, 8L));
        // это многоместный товар - у него заявлено 6 комплектов,
        // принято - 6 цельных комплеков, 3 - излишка, и 3 неполных, которые должны посчитаться как дефект
        InboundReceiptDetailKey firstItem = new InboundReceiptDetailKey(
            "0000000009",
            "TST0000000000000000023",
            "123",
            null
        );
        // одноместный товар - 2 заявили, 2 приняли
        InboundReceiptDetailKey secondItem = new InboundReceiptDetailKey(
            "0000000010",
            "TST0000000000000000024",
            "123",
            null
        );
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(actualDetails.isEmpty()).isFalse();
            // для многометного товара должно быть три айтема:
            // stage - expected - 6 - received - 6
            // stage - surplus - expected - 0 - received - 3
            // damage - 3
            // для одноместного - один айтем:
            // stage - expected - 2 - received - 2
            assertions.assertThat(actualDetails.size()).isEqualTo(4);
            Map<InboundReceiptDetailKey, List<ReceiptDetailItem>> byKey =
                actualDetails.stream().collect(Collectors.groupingBy(ReceiptDetailItem::getKey));
            List<ReceiptDetailItem> firstItems = byKey.get(firstItem);
            assertItems(6, 9, 3, 6, 3, firstItems);
            List<ReceiptDetailItem> secondItems = byKey.get(secondItem);
            assertItems(2, 2, 0, 0, 1, secondItems);
        });
    }

    /**
     * Проверяет разбивку по номеру заказа на отгрузку.
     */
    @Test
    void singlePartItemWithDifferentInfo() {
        List<ReceiptDetailItem> actualDetails =
            extractionService.getDetailsForPage(new Pagination(1, 11L));
        // в поставке 1 товар, но должен разбиться на 3 айтема:
        // айтем, который соответствует строкам с заявленным количеством
        // так как инфор должен писать номер заказа в принятую строку
        InboundReceiptDetailKey withoutOrder = new InboundReceiptDetailKey(
            "0000000012",
            "TST0000000000000000026",
            "123",
            null
        );
        // айтем с принятым количеством для первого заказа
        InboundReceiptDetailKey withOrder = new InboundReceiptDetailKey(
            "0000000012",
            "TST0000000000000000026",
            "123",
            "mehOrder"
        );
        // айтем для принятого количества сог вторым заказом
        InboundReceiptDetailKey withAnotherOrder = new InboundReceiptDetailKey(
            "0000000012",
            "TST0000000000000000026",
            "123",
            "mehOrder2"
        );
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(actualDetails).isNotNull();
            assertions.assertThat(actualDetails.size()).isEqualTo(3);
            Map<InboundReceiptDetailKey, List<ReceiptDetailItem>> byKey =
                actualDetails.stream().collect(Collectors.groupingBy(ReceiptDetailItem::getKey));
            List<ReceiptDetailItem> itemsWithoutOrder = byKey.get(withoutOrder);
            // без заказа - ожидает 2 комплекта, приняли 0
            assertItems(2, 0, 0, 0, 1, itemsWithoutOrder);
            List<ReceiptDetailItem> itemsWithOrder = byKey.get(withOrder);
            // с первым заказом - приняли 1 комплект
            assertItems(0, 1, 0, 1, 1, itemsWithOrder);
            List<ReceiptDetailItem> itemsWithAnotherOrder = byKey.get(withAnotherOrder);
            // со вторым заказом приняли тоже 1 комплект
            assertItems(0, 1, 0, 1, 1, itemsWithAnotherOrder);
        });
    }

    private static void assertItems(long expected, long received, long damaged, long surplus, int expectedAmount,
                                    Collection<ReceiptDetailItem> items) {
        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(items).isNotNull();
            assertions.assertThat(items.size()).isEqualTo(expectedAmount);
            assertions.assertThat(findMaxExpected(items)).isEqualTo(expected);
            assertions.assertThat(findReceivedAmount(items)).isEqualTo(received);
            assertions.assertThat(findDamaged(items)).isEqualTo(damaged);
            assertions.assertThat(findSurplusAmount(items)).isEqualTo(surplus);

        });
    }

    private static long findSurplusAmount(Collection<ReceiptDetailItem> items) {
        return items.stream()
            .mapToLong(receiptDetailItem ->
                Math.max(
                    receiptDetailItem.getReceivedQuantity().longValue() -
                        receiptDetailItem.getExpectedQuantity().longValue()
                    , 0))
            .sum();
    }

    private static long findDamaged(Collection<ReceiptDetailItem> items) {
        return items.stream()
            .filter(it -> it.getTolocStatus() == HOLD)
            .map(ReceiptDetailItem::getReceivedQuantity)
            .findAny()
            .map(BigDecimal::longValue)
            .orElse(0L);
    }

    private static long findReceivedAmount(Collection<ReceiptDetailItem> items) {
        return items.stream()
            .filter(it -> it.getTolocStatus() == OK)
            .map(ReceiptDetailItem::getReceivedQuantity)
            .mapToLong(BigDecimal::longValue)
            .sum();
    }

    private static long findMaxExpected(Collection<ReceiptDetailItem> items) {
        return items.stream()
            .map(ReceiptDetailItem::getExpectedQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::max).longValue();
    }
}

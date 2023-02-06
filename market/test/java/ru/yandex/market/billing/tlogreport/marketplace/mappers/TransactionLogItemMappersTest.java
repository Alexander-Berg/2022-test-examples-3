package ru.yandex.market.billing.tlogreport.marketplace.mappers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.model.tlog.TransactionLogItem;
import ru.yandex.market.billing.tlog.dao.TransactionLogDao;
import ru.yandex.market.billing.tlogreport.marketplace.exception.JsonPayload2SerializationException;
import ru.yandex.market.billing.tlogreport.marketplace.model.TransactionReportLogItem;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.Matchers.hasSize;

@SuppressWarnings("checkstyle:LineLength")
class TransactionLogItemMappersTest extends FunctionalTest {

    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"))
            .registerModule(new JavaTimeModule());
    @Autowired
    @Qualifier("feeMapper")
    private TransactionLogItemMapper placementMapper;
    @Autowired
    @Qualifier("deliveryMapper")
    private TransactionLogItemMapper deliveryMapper;
    @Autowired
    @Qualifier("loyaltyMapper")
    private TransactionLogItemMapper loyaltyMapper;
    @Autowired
    @Qualifier("ffProcessingMapper")
    private TransactionLogItemMapper warehouseMapper;
    @Autowired
    @Qualifier("agencyMapper")
    private TransactionLogItemMapper agencyMapper;
    @Autowired
    @Qualifier("xdocMapper")
    private TransactionLogItemMapper transitMapper;
    @Autowired
    @Qualifier("surplusMapper")
    private TransactionLogItemMapper surplusMapper;
    @Autowired
    @Qualifier("withdrawMapper")
    private TransactionLogItemMapper withdrawMapper;
    @Autowired
    @Qualifier("sortMapper")
    private TransactionLogItemMapper sortMapper;
    @Autowired
    @Qualifier("returnsMapper")
    private TransactionLogItemMapper resupplyMapper;
    @Autowired
    @Qualifier("promosMapper")
    private TransactionLogItemMapper promosMapper;
    @Autowired
    @Qualifier("disposalMapper")
    private TransactionLogItemMapper utilizationMapper;
    @Autowired
    @Qualifier("expressMapper")
    private TransactionLogItemMapper expressMapper;
    @Autowired
    @Qualifier("ffStoringMapper")
    private TransactionLogItemMapper paidStorageMapper;
    @Autowired
    @Qualifier("installmentMapper")
    private TransactionLogItemMapper installmentMapper;
    @Autowired
    private TransactionLogDao transactionLogDao;

    protected static String serializePayload2(Object payload2Dto) {
        try {
            return MAPPER.writeValueAsString(payload2Dto);
        } catch (JsonProcessingException e) {
            throw new JsonPayload2SerializationException("Error during json deserialization", e);
        }
    }

    private List<String> getPayload2(TransactionLogItemMapper mapper, int size) {
        Collection<TransactionLogItem> items = transactionLogDao.getTransactionLogItem(mapper.getMappedProduct());
        List<TransactionReportLogItem> payloads2 = new ArrayList<>(mapper.map(items));
        MatcherAssert.assertThat("Number of payloads2 must be " + size, payloads2, hasSize(size));
        return payloads2.stream().map(i -> serializePayload2(i.getPayload2())).collect(Collectors.toList());
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testOrderMappers.before.csv"
    )
    public void testPlacementMapper() {
        List<String> payload2Excepcted = Arrays.asList(
                "\"{\\\"is_correction\\\":false,\\\"order_id\\\":10001,\\\"order_creation_datetime\\\":\\\"2021-09-29T17:33:51\\\",\\\"shop_sku\\\":\\\"тестовый sku\\\",\\\"offer_name\\\":\\\"тестовый оффер\\\",\\\"price\\\":2.72,\\\"count\\\":1,\\\"batch_size\\\":2,\\\"weight\\\":1,\\\"length\\\":1,\\\"width\\\":1,\\\"height\\\":1,\\\"dimensions_sum\\\":3,\\\"service_name\\\":\\\"Размещение товаров на витрине\\\",\\\"tariff\\\":2.72,\\\"tariff_dimension\\\":\\\"%\\\",\\\"min_tariff\\\":null,\\\"max_tariff\\\":null,\\\"service_before_tariff\\\":null,\\\"service_datetime\\\":\\\"2020-04-22T12:00:00\\\",\\\"is_cash_only\\\":false}\"",
                "\"{\\\"is_correction\\\":false,\\\"order_id\\\":null,\\\"order_creation_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"shop_sku\\\":\\\"draft sku\\\",\\\"offer_name\\\":\\\"mname\\\",\\\"price\\\":0.0,\\\"count\\\":1,\\\"batch_size\\\":null,\\\"weight\\\":null,\\\"length\\\":null,\\\"width\\\":null,\\\"height\\\":null,\\\"dimensions_sum\\\":null,\\\"service_name\\\":\\\"Размещение товаров на витрине\\\",\\\"tariff\\\":1.34,\\\"tariff_dimension\\\":\\\"руб.\\\",\\\"min_tariff\\\":null,\\\"max_tariff\\\":null,\\\"service_before_tariff\\\":null,\\\"service_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"is_cash_only\\\":null}\"",
                "\"{\\\"is_correction\\\":true,\\\"order_id\\\":null,\\\"order_creation_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"shop_sku\\\":\\\"draft sku\\\",\\\"offer_name\\\":\\\"mname\\\",\\\"price\\\":0.0,\\\"count\\\":1,\\\"batch_size\\\":null,\\\"weight\\\":null,\\\"length\\\":null,\\\"width\\\":null,\\\"height\\\":null,\\\"dimensions_sum\\\":null,\\\"service_name\\\":\\\"Корректировка комиссии\\\",\\\"tariff\\\":-0.34,\\\"tariff_dimension\\\":\\\"руб.\\\",\\\"min_tariff\\\":null,\\\"max_tariff\\\":null,\\\"service_before_tariff\\\":null,\\\"service_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"is_cash_only\\\":null}\""
        );
        List<String> payload2Actual = getPayload2(placementMapper, 3);
        Collections.sort(payload2Actual);
        Assertions.assertEquals(payload2Excepcted, payload2Actual);
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testOrderMappers.before.csv"
    )
    public void testDeliveryMapper() {
        String payload2Excepcted =
                "\"{\\\"is_correction\\\":false,\\\"order_id\\\":10001,\\\"shop_sku\\\":\\\"тестовый sku\\\",\\\"offer_name\\\":\\\"тестовый оффер\\\",\\\"price\\\":2.72,\\\"count\\\":1,\\\"batch_size\\\":2,\\\"weight\\\":1,\\\"length\\\":1,\\\"width\\\":1,\\\"height\\\":1,\\\"dimensions_sum\\\":3,\\\"service_name\\\":\\\"Доставка покупателю\\\",\\\"tariff\\\":2.72,\\\"tariff_dimension\\\":\\\"%\\\",\\\"min_tariff\\\":null,\\\"max_tariff\\\":null,\\\"service_before_tariff\\\":null,\\\"service_datetime\\\":\\\"2020-04-22T12:00:00\\\",\\\"region_to\\\":42,\\\"warehouse_id\\\":1}\"";
        List<String> payload2Actual = getPayload2(deliveryMapper, 1);
        Assertions.assertEquals(payload2Excepcted, payload2Actual.get(0));
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testOrderMappers.before.csv"
    )
    public void testLoyaltyMapper() {
        String payload2Excepcted = "\"{\\\"is_correction\\\":false,\\\"order_id\\\":10001,\\\"shop_sku\\\":\\\"тестовый sku\\\",\\\"offer_name\\\":\\\"тестовый оффер\\\",\\\"price\\\":2.72,\\\"client_price\\\":2.42,\\\"count\\\":1,\\\"service_name\\\":\\\"Участие в программе лояльности\\\",\\\"tariff\\\":2.72,\\\"tariff_dimension\\\":\\\"%\\\",\\\"service_datetime\\\":\\\"2020-04-22T12:00:00\\\"}\"";
        List<String> payload2Actual = getPayload2(loyaltyMapper, 1);
        Assertions.assertEquals(payload2Excepcted, payload2Actual.get(0));
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testOrderMappers.before.csv"
    )
    public void testWarehouseMapper() {
        String payload2Excepcted = "\"{\\\"is_correction\\\":false,\\\"order_id\\\":10001,\\\"shop_sku\\\":\\\"тестовый sku\\\",\\\"offer_name\\\":\\\"тестовый оффер\\\",\\\"price\\\":2.72,\\\"count\\\":1,\\\"batch_size\\\":2,\\\"weight\\\":1,\\\"length\\\":1,\\\"width\\\":1,\\\"height\\\":1,\\\"dimensions_sum\\\":3,\\\"service_name\\\":\\\"Складская обработка\\\",\\\"tariff\\\":2.72,\\\"tariff_dimension\\\":\\\"%\\\",\\\"min_tariff\\\":null,\\\"max_tariff\\\":null,\\\"service_before_tariff\\\":null,\\\"service_datetime\\\":\\\"2020-04-22T12:00:00\\\"}\"";
        List<String> payload2Actual = getPayload2(warehouseMapper, 1);
        Assertions.assertEquals(payload2Excepcted, payload2Actual.get(0));
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testAgencyMapper.before.csv"
    )
    public void testAgencyMapper() {
        List<String> payload2Excepcted = Arrays.asList(
                "\"{\\\"order_id\\\":10001,\\\"client_price\\\":0.0,\\\"tariff_part\\\":0.0,\\\"service_datetime\\\":\\\"2020-04-22T14:00:00\\\",\\\"type\\\":\\\"Корректировка\\\"}\"",
                "\"{\\\"order_id\\\":10001,\\\"client_price\\\":0.0,\\\"tariff_part\\\":0.0,\\\"service_datetime\\\":\\\"2020-04-22T15:00:00\\\",\\\"type\\\":\\\"Корректировка\\\"}\"",
                "\"{\\\"order_id\\\":10001,\\\"client_price\\\":455.0,\\\"tariff_part\\\":1.0,\\\"service_datetime\\\":\\\"2020-04-22T12:00:00\\\",\\\"type\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"order_id\\\":10001,\\\"client_price\\\":555.0,\\\"tariff_part\\\":1.0,\\\"service_datetime\\\":\\\"2020-04-22T13:00:00\\\",\\\"type\\\":\\\"Начисление\\\"}\""
        );
        List<String> payload2Actual = getPayload2(agencyMapper, 4);
        Collections.sort(payload2Actual);
        Assertions.assertEquals(payload2Excepcted, payload2Actual);
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testTransitMapper.before.csv"
    )
    public void testTransitMapper() {
        String payload2Excepcted = "\"{\\\"ff_request_id\\\":222,\\\"id\\\":\\\"000000\\\",\\\"is_box\\\":\\\"коробка\\\",\\\"tariff\\\":125.00,\\\"box_count\\\":1,\\\"service_datetime\\\":\\\"2020-04-22T12:00:00\\\"}\"";
        List<String> payload2Actual = getPayload2(transitMapper, 1);
        Assertions.assertEquals(payload2Excepcted, payload2Actual.get(0));
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testSurplusMapper.before.csv"
    )
    public void testSurplusMapper() {
        List<String> payload2Excepcted = Arrays.asList(
                "\"{\\\"is_correction\\\":false,\\\"ff_request_id\\\":404,\\\"id\\\":\\\"000000\\\",\\\"shop_sku\\\":\\\"test sku\\\",\\\"tariff\\\":606.00,\\\"count\\\":1,\\\"service_datetime\\\":\\\"2020-04-22T12:00:00\\\",\\\"type\\\":\\\"начисление\\\"}\"",
                "\"{\\\"is_correction\\\":true,\\\"ff_request_id\\\":404,\\\"id\\\":\\\"000000\\\",\\\"shop_sku\\\":\\\"test sku\\\",\\\"tariff\\\":606.00,\\\"count\\\":1,\\\"service_datetime\\\":\\\"2020-04-22T13:00:00\\\",\\\"type\\\":\\\"корректировка\\\"}\""
        );
        List<String> payload2Actual = getPayload2(surplusMapper, 2);
        Collections.sort(payload2Actual);
        Assertions.assertEquals(payload2Excepcted, payload2Actual);
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testWithdrawMapper.before.csv"
    )
    public void testWithdrawMapper() {
        List<String> payload2Excepcted = Arrays.asList(
                "\"{\\\"id\\\":30,\\\"ff_request_id\\\":\\\"000000\\\",\\\"shop_sku\\\":\\\"test sku\\\",\\\"offer_name\\\":\\\"test market name\\\",\\\"stock\\\":\\\"Годный\\\",\\\"cost\\\":6.67,\\\"count\\\":1,\\\"weight\\\":1.000,\\\"length\\\":1,\\\"width\\\":1,\\\"height\\\":1,\\\"dimensions_sum\\\":3,\\\"service_name\\\":\\\"Возврат товара заказчику\\\",\\\"tariff\\\":6.67,\\\"service_datetime\\\":\\\"2020-04-22T12:10:00\\\"}\"",
                "\"{\\\"id\\\":null,\\\"ff_request_id\\\":null,\\\"shop_sku\\\":\\\"test sku\\\",\\\"offer_name\\\":null,\\\"stock\\\":null,\\\"cost\\\":null,\\\"count\\\":null,\\\"weight\\\":null,\\\"length\\\":null,\\\"width\\\":null,\\\"height\\\":null,\\\"dimensions_sum\\\":null,\\\"service_name\\\":\\\"Корректировка\\\",\\\"tariff\\\":null,\\\"service_datetime\\\":\\\"2020-04-22T13:00:00\\\"}\""
        );
        List<String> payload2Actual = getPayload2(withdrawMapper, 2);
        Collections.sort(payload2Actual);
        Assertions.assertEquals(payload2Excepcted, payload2Actual);
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testSortMapper.before.csv"
    )
    public void testSortMapper() {
        List<String> payload2Excepcted = Arrays.asList(
                "\"{\\\"service_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"fact_service_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"order_count\\\":\\\"3\\\",\\\"location\\\":\\\"Сортировочный центр\\\",\\\"tariff\\\":15.00,\\\"summary\\\":30.00,\\\"min_tariff_summary\\\":7.00,\\\"type\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"service_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"fact_service_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"order_count\\\":\\\"3\\\",\\\"location\\\":\\\"Сортировочный центр\\\",\\\"tariff\\\":15.00,\\\"summary\\\":30.00,\\\"min_tariff_summary\\\":7.00,\\\"type\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"service_datetime\\\":\\\"2020-04-23T00:00:00\\\",\\\"fact_service_datetime\\\":\\\"2020-04-23T00:00:00\\\",\\\"order_count\\\":\\\"\\\",\\\"location\\\":\\\"Сортировочный центр\\\",\\\"tariff\\\":null,\\\"summary\\\":null,\\\"min_tariff_summary\\\":null,\\\"type\\\":\\\"Корректировка\\\"}\""
        );
        List<String> payload2Actual = getPayload2(sortMapper, 3);
        Collections.sort(payload2Actual);
        Assertions.assertEquals(payload2Excepcted, payload2Actual);
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testResupplyMapper.before.csv"
    )
    public void testResupplyMapper() {
        List<String> payload2Excepcted = Arrays.asList(

                "\"{\\\"order_item_id\\\":7,\\\"count\\\":2,\\\"resupply_type\\\":\\\"Возврат\\\",\\\"service_datetime\\\":\\\"2020-04-23T00:00:00\\\",\\\"order_id\\\":10001,\\\"return_id\\\":851,\\\"unredeemed_order_tariff\\\":null,\\\"return_tariff\\\":9.63,\\\"type\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"order_item_id\\\":7,\\\"count\\\":null,\\\"resupply_type\\\":\\\"Возврат\\\",\\\"service_datetime\\\":\\\"2020-04-23T00:00:00\\\",\\\"order_id\\\":10001,\\\"return_id\\\":851,\\\"unredeemed_order_tariff\\\":null,\\\"return_tariff\\\":null,\\\"type\\\":\\\"Корректировка\\\"}\"",
                "\"{\\\"order_item_id\\\":null,\\\"count\\\":null,\\\"resupply_type\\\":\\\"Возврат\\\",\\\"service_datetime\\\":\\\"2022-03-09T00:00:00\\\",\\\"order_id\\\":561783,\\\"return_id\\\":145437,\\\"unredeemed_order_tariff\\\":null,\\\"return_tariff\\\":15.00,\\\"type\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"order_item_id\\\":null,\\\"count\\\":null,\\\"resupply_type\\\":\\\"Невыкуп\\\",\\\"service_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"order_id\\\":10001,\\\"return_id\\\":null,\\\"unredeemed_order_tariff\\\":4.33,\\\"return_tariff\\\":null,\\\"type\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"order_item_id\\\":null,\\\"count\\\":null,\\\"resupply_type\\\":\\\"Невыкуп\\\",\\\"service_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"order_id\\\":10002,\\\"return_id\\\":null,\\\"unredeemed_order_tariff\\\":4.33,\\\"return_tariff\\\":null,\\\"type\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"order_item_id\\\":null,\\\"count\\\":null,\\\"resupply_type\\\":\\\"Невыкуп\\\",\\\"service_datetime\\\":\\\"2020-04-23T00:00:00\\\",\\\"order_id\\\":10001,\\\"return_id\\\":null,\\\"unredeemed_order_tariff\\\":null,\\\"return_tariff\\\":null,\\\"type\\\":\\\"Корректировка\\\"}\"",
                "\"{\\\"order_item_id\\\":null,\\\"count\\\":null,\\\"resupply_type\\\":\\\"Невыкуп\\\",\\\"service_datetime\\\":\\\"2022-03-03T00:00:00\\\",\\\"order_id\\\":561788,\\\"return_id\\\":145439,\\\"unredeemed_order_tariff\\\":null,\\\"return_tariff\\\":null,\\\"type\\\":\\\"Корректировка\\\"}\""
        );
        List<String> payload2Actual = getPayload2(resupplyMapper, 7);
        Collections.sort(payload2Actual);
        Assertions.assertEquals(payload2Excepcted, payload2Actual);
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testPromosMapper.before.csv"
    )
    public void testPromosMapper() {

        List<String> payload2Excepcted = Arrays.asList(
                "\"{\\\"order_id\\\":10001,\\\"shop_sku\\\":\\\"test shop sku\\\",\\\"offer_name\\\":\\\"test offer\\\",\\\"category_name\\\":\\\"test category\\\",\\\"price\\\":5.67,\\\"count\\\":1,\\\"service_name\\\":\\\"Расходы на рекламные кампании\\\",\\\"bet\\\":3.43,\\\"summary\\\":null,\\\"service_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"cpa_promotion_bonus_spent\\\":2.43}\"",
                "\"{\\\"order_id\\\":10001,\\\"shop_sku\\\":\\\"test shop sku\\\",\\\"offer_name\\\":\\\"test offer\\\",\\\"category_name\\\":\\\"test category\\\",\\\"price\\\":null,\\\"count\\\":null,\\\"service_name\\\":\\\"Корректировка суммы списания\\\",\\\"bet\\\":null,\\\"summary\\\":null,\\\"service_datetime\\\":\\\"2020-04-23T00:00:00\\\",\\\"cpa_promotion_bonus_spent\\\":null}\""
        );
        List<String> payload2Actual = getPayload2(promosMapper, 2);
        Collections.sort(payload2Actual);
        Assertions.assertEquals(payload2Excepcted, payload2Actual);
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testUtilizationMapper.before.csv"
    )
    public void testUtilizationMapper() {
        List<String> payload2Excepcted = Arrays.asList(
                "\"{\\\"shop_sku\\\":\\\"test sku\\\",\\\"offer_name\\\":\\\"test item name\\\",\\\"count\\\":1,\\\"weight\\\":1,\\\"length\\\":1,\\\"width\\\":1,\\\"height\\\":1,\\\"dimensions_sum\\\":3,\\\"service_name\\\":\\\"Корректировка Организации утилизации\\\",\\\"tariff\\\":null,\\\"request_datetime\\\":\\\"2020-04-22T12:20:00\\\",\\\"service_datetime\\\":\\\"2020-04-23T13:00:00\\\"}\"",
                "\"{\\\"shop_sku\\\":\\\"test sku\\\",\\\"offer_name\\\":\\\"test item name\\\",\\\"count\\\":1,\\\"weight\\\":1,\\\"length\\\":1,\\\"width\\\":1,\\\"height\\\":1,\\\"dimensions_sum\\\":3,\\\"service_name\\\":\\\"Организация утилизации\\\",\\\"tariff\\\":5.66,\\\"request_datetime\\\":\\\"2020-04-22T12:20:00\\\",\\\"service_datetime\\\":\\\"2020-04-22T12:00:00\\\"}\""
        );
        List<String> payload2Actual = getPayload2(utilizationMapper, 2);
        Collections.sort(payload2Actual);
        Assertions.assertEquals(payload2Excepcted, payload2Actual);
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testExpressMapper.before.csv"
    )
    public void testExpressMapper() {
        List<String> payload2Excepcted = Arrays.asList(
                "\"{\\\"order_id\\\":10001,\\\"service_name\\\":\\\"Экспресс-доставка покупателю\\\",\\\"tariff\\\":0.85,\\\"tariff_dimension\\\":\\\"руб.\\\",\\\"service_datetime\\\":\\\"2020-04-22T12:00:00\\\",\\\"shop_sku\\\":\\\"тестовый sku\\\",\\\"offer_name\\\":\\\"тестовый оффер\\\",\\\"price\\\":0.85,\\\"count\\\":1,\\\"batch_size\\\":2,\\\"weight\\\":1,\\\"length\\\":1,\\\"width\\\":1,\\\"height\\\":1,\\\"dimensions_sum\\\":3,\\\"min_tariff\\\":null,\\\"max_tariff\\\":null,\\\"service_before_tariff\\\":null}\"",
                "\"{\\\"order_id\\\":10002,\\\"service_name\\\":\\\"Возврат заказа (невыкупа)\\\",\\\"tariff\\\":0.7,\\\"tariff_dimension\\\":\\\"руб.\\\",\\\"service_datetime\\\":\\\"2020-04-22T12:15:00\\\",\\\"shop_sku\\\":\\\"тестовый sku\\\",\\\"offer_name\\\":\\\"тестовый оффер\\\",\\\"price\\\":0.7,\\\"count\\\":1,\\\"batch_size\\\":2,\\\"weight\\\":1,\\\"length\\\":1,\\\"width\\\":1,\\\"height\\\":1,\\\"dimensions_sum\\\":3,\\\"min_tariff\\\":null,\\\"max_tariff\\\":null,\\\"service_before_tariff\\\":null}\"",
                "\"{\\\"order_id\\\":10002,\\\"service_name\\\":\\\"Корректировка\\\",\\\"tariff\\\":0.15,\\\"tariff_dimension\\\":\\\"руб.\\\",\\\"service_datetime\\\":\\\"2020-04-23T13:00:00\\\",\\\"shop_sku\\\":\\\"тестовый sku\\\",\\\"offer_name\\\":\\\"тестовый оффер\\\",\\\"price\\\":0.7,\\\"count\\\":1,\\\"batch_size\\\":2,\\\"weight\\\":1,\\\"length\\\":1,\\\"width\\\":1,\\\"height\\\":1,\\\"dimensions_sum\\\":3,\\\"min_tariff\\\":null,\\\"max_tariff\\\":null,\\\"service_before_tariff\\\":null}\"",
                "\"{\\\"order_id\\\":29,\\\"service_name\\\":\\\"Экспресс-доставка покупателю\\\",\\\"tariff\\\":0.85,\\\"tariff_dimension\\\":\\\"РУБ\\\",\\\"service_datetime\\\":\\\"2020-04-22T12:00:00\\\",\\\"shop_sku\\\":null,\\\"offer_name\\\":null,\\\"price\\\":null,\\\"count\\\":null,\\\"batch_size\\\":null,\\\"weight\\\":null,\\\"length\\\":null,\\\"width\\\":null,\\\"height\\\":null,\\\"dimensions_sum\\\":null,\\\"min_tariff\\\":null,\\\"max_tariff\\\":null,\\\"service_before_tariff\\\":null}\"",
                "\"{\\\"order_id\\\":30,\\\"service_name\\\":\\\"Возврат заказа (невыкупа)\\\",\\\"tariff\\\":0.70,\\\"tariff_dimension\\\":\\\"РУБ\\\",\\\"service_datetime\\\":\\\"2020-04-22T12:15:00\\\",\\\"shop_sku\\\":null,\\\"offer_name\\\":null,\\\"price\\\":null,\\\"count\\\":null,\\\"batch_size\\\":null,\\\"weight\\\":null,\\\"length\\\":null,\\\"width\\\":null,\\\"height\\\":null,\\\"dimensions_sum\\\":null,\\\"min_tariff\\\":null,\\\"max_tariff\\\":null,\\\"service_before_tariff\\\":null}\"",
                "\"{\\\"order_id\\\":30,\\\"service_name\\\":\\\"Корректировка\\\",\\\"tariff\\\":null,\\\"tariff_dimension\\\":null,\\\"service_datetime\\\":\\\"2020-04-23T13:00:00\\\",\\\"shop_sku\\\":null,\\\"offer_name\\\":null,\\\"price\\\":null,\\\"count\\\":null,\\\"batch_size\\\":null,\\\"weight\\\":null,\\\"length\\\":null,\\\"width\\\":null,\\\"height\\\":null,\\\"dimensions_sum\\\":null,\\\"min_tariff\\\":null,\\\"max_tariff\\\":null,\\\"service_before_tariff\\\":null}\""

        );
        List<String> payload2Actual = getPayload2(expressMapper, 6);
        Collections.sort(payload2Actual);
        Assertions.assertEquals(payload2Excepcted, payload2Actual);
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testPaidStorageMapper.before.csv"
    )
    public void testPaidStorageMapper() {
        List<String> payload2Excepcted = Arrays.asList(
                "\"{\\\"supplier_id\\\":774,\\\"supply_id\\\":11,\\\"shop_sku\\\":\\\"test shop sku\\\",\\\"market_sku\\\":null,\\\"offer_name\\\":null,\\\"service_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"count\\\":1,\\\"weight\\\":1.000,\\\"length\\\":1,\\\"width\\\":1,\\\"height\\\":1,\\\"dimensions_sum\\\":3,\\\"tariff\\\":8.88,\\\"category\\\":null,\\\"avg_stock_liters\\\":null,\\\"avg_sold_liters\\\":null,\\\"turnover\\\":null,\\\"storage_period\\\":null,\\\"service_name\\\":null}\"",
                "\"{\\\"supplier_id\\\":774,\\\"supply_id\\\":22,\\\"shop_sku\\\":\\\"test shop sku\\\",\\\"market_sku\\\":null,\\\"offer_name\\\":null,\\\"service_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"count\\\":1,\\\"weight\\\":1.000,\\\"length\\\":1,\\\"width\\\":1,\\\"height\\\":1,\\\"dimensions_sum\\\":3,\\\"tariff\\\":8.88,\\\"category\\\":null,\\\"avg_stock_liters\\\":null,\\\"avg_sold_liters\\\":null,\\\"turnover\\\":null,\\\"storage_period\\\":null,\\\"service_name\\\":null}\"",
                "\"{\\\"supplier_id\\\":774,\\\"supply_id\\\":33,\\\"shop_sku\\\":\\\"test shop sku\\\",\\\"market_sku\\\":null,\\\"offer_name\\\":null,\\\"service_datetime\\\":\\\"2020-04-22T00:00:00\\\",\\\"count\\\":4,\\\"weight\\\":1.000,\\\"length\\\":1,\\\"width\\\":1,\\\"height\\\":1,\\\"dimensions_sum\\\":3,\\\"tariff\\\":2.22,\\\"category\\\":null,\\\"avg_stock_liters\\\":null,\\\"avg_sold_liters\\\":null,\\\"turnover\\\":null,\\\"storage_period\\\":null,\\\"service_name\\\":null}\"",
                "\"{\\\"supplier_id\\\":774,\\\"supply_id\\\":null,\\\"shop_sku\\\":\\\"test shop sku\\\",\\\"market_sku\\\":null,\\\"offer_name\\\":null,\\\"service_datetime\\\":\\\"2020-04-23T13:00:00\\\",\\\"count\\\":null,\\\"weight\\\":null,\\\"length\\\":null,\\\"width\\\":null,\\\"height\\\":null,\\\"dimensions_sum\\\":null,\\\"tariff\\\":null,\\\"category\\\":null,\\\"avg_stock_liters\\\":null,\\\"avg_sold_liters\\\":null,\\\"turnover\\\":null,\\\"storage_period\\\":null,\\\"service_name\\\":null}\"",
                "\"{\\\"supplier_id\\\":774,\\\"supply_id\\\":null,\\\"shop_sku\\\":null,\\\"market_sku\\\":null,\\\"offer_name\\\":null,\\\"service_datetime\\\":\\\"2022-06-30T00:00:00\\\",\\\"count\\\":null,\\\"weight\\\":null,\\\"length\\\":null,\\\"width\\\":null,\\\"height\\\":null,\\\"dimensions_sum\\\":null,\\\"tariff\\\":10,\\\"category\\\":\\\"cat 1\\\",\\\"avg_stock_liters\\\":12,\\\"avg_sold_liters\\\":24,\\\"turnover\\\":\\\"365.24\\\",\\\"storage_period\\\":30,\\\"service_name\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"supplier_id\\\":774,\\\"supply_id\\\":null,\\\"shop_sku\\\":null,\\\"market_sku\\\":null,\\\"offer_name\\\":null,\\\"service_datetime\\\":\\\"2022-06-30T00:00:00\\\",\\\"count\\\":null,\\\"weight\\\":null,\\\"length\\\":null,\\\"width\\\":null,\\\"height\\\":null,\\\"dimensions_sum\\\":null,\\\"tariff\\\":20,\\\"category\\\":\\\"cat 2\\\",\\\"avg_stock_liters\\\":15,\\\"avg_sold_liters\\\":26,\\\"turnover\\\":\\\"Infinity\\\",\\\"storage_period\\\":30,\\\"service_name\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"supplier_id\\\":774,\\\"supply_id\\\":null,\\\"shop_sku\\\":null,\\\"market_sku\\\":null,\\\"offer_name\\\":null,\\\"service_datetime\\\":\\\"2022-06-30T00:00:00\\\",\\\"count\\\":null,\\\"weight\\\":null,\\\"length\\\":null,\\\"width\\\":null,\\\"height\\\":null,\\\"dimensions_sum\\\":null,\\\"tariff\\\":null,\\\"category\\\":\\\"1\\\",\\\"avg_stock_liters\\\":null,\\\"avg_sold_liters\\\":null,\\\"turnover\\\":null,\\\"storage_period\\\":null,\\\"service_name\\\":\\\"Корректировка\\\"}\""
        );
        List<String> payload2Actual = getPayload2(paidStorageMapper, 7);
        Collections.sort(payload2Actual);
        Assertions.assertEquals(payload2Actual.size(), payload2Excepcted.size());
        for (int i = 0; i < payload2Excepcted.size(); i++) {
            String excepcted = payload2Excepcted.get(i);
            String actual = payload2Actual.get(i);
            Assertions.assertEquals(excepcted, actual);
        }
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testInstallmentMapper.before.csv"
    )
    public void testInstallmentMapper() {
        List<String> payload2Excepcted = Arrays.asList(
                "\"{\\\"order_id\\\":10001,\\\"shop_sku\\\":\\\"test shop sku\\\",\\\"offer_name\\\":\\\"test offer\\\",\\\"count\\\":1,\\\"price\\\":1.11,\\\"service_name\\\":\\\"Корректировка комиссии за оплату в рассрочку\\\",\\\"tariff\\\":5.55,\\\"tariff_dimension\\\":\\\"руб.\\\",\\\"service_datetime\\\":\\\"2020-04-23T13:00:00\\\",\\\"type\\\":\\\"Корректировка\\\"}\"",
                "\"{\\\"order_id\\\":10001,\\\"shop_sku\\\":\\\"test shop sku\\\",\\\"offer_name\\\":\\\"test offer\\\",\\\"count\\\":1,\\\"price\\\":1.11,\\\"service_name\\\":\\\"Яндекс.Сплит\\\",\\\"tariff\\\":1.11,\\\"tariff_dimension\\\":\\\"%\\\",\\\"service_datetime\\\":\\\"2020-04-22T12:10:00\\\",\\\"type\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"order_id\\\":10001,\\\"shop_sku\\\":\\\"test shop sku\\\",\\\"offer_name\\\":\\\"test offer\\\",\\\"count\\\":2,\\\"price\\\":2.32,\\\"service_name\\\":\\\"Возврат за Яндекс.Сплит\\\",\\\"tariff\\\":2.22,\\\"tariff_dimension\\\":\\\"%\\\",\\\"service_datetime\\\":\\\"2020-04-22T12:10:00\\\",\\\"type\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"order_id\\\":10001,\\\"shop_sku\\\":\\\"test shop sku\\\",\\\"offer_name\\\":\\\"test offer\\\",\\\"count\\\":3,\\\"price\\\":3.53,\\\"service_name\\\":\\\"Штраф за отмену заказа при оплате в рассрочку\\\",\\\"tariff\\\":3.33,\\\"tariff_dimension\\\":\\\"%\\\",\\\"service_datetime\\\":\\\"2020-04-22T12:10:00\\\",\\\"type\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"order_id\\\":10001,\\\"shop_sku\\\":\\\"test shop sku\\\",\\\"offer_name\\\":\\\"test offer\\\",\\\"count\\\":4,\\\"price\\\":1.11,\\\"service_name\\\":\\\"Отмена комиссии при оплате в рассрочку при возврате товара\\\",\\\"tariff\\\":4.44,\\\"tariff_dimension\\\":\\\"%\\\",\\\"service_datetime\\\":\\\"2020-04-22T12:10:00\\\",\\\"type\\\":\\\"Начисление\\\"}\"",
                "\"{\\\"order_id\\\":10001,\\\"shop_sku\\\":\\\"test shop sku\\\",\\\"offer_name\\\":\\\"test offer\\\",\\\"count\\\":4,\\\"price\\\":4.74,\\\"service_name\\\":\\\"Отмена комиссии при оплате в рассрочку при возврате товара\\\",\\\"tariff\\\":4.44,\\\"tariff_dimension\\\":\\\"%\\\",\\\"service_datetime\\\":\\\"2020-04-22T12:10:00\\\",\\\"type\\\":\\\"Начисление\\\"}\""
        );
        List<String> payload2Actual = getPayload2(installmentMapper, 6);
        Collections.sort(payload2Actual);
        Assertions.assertEquals(payload2Excepcted, payload2Actual);
    }

}

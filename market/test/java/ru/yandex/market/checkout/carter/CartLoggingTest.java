package ru.yandex.market.checkout.carter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;
import ru.yandex.market.checkout.carter.utils.CarterTestUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWith;
import static ru.yandex.market.checkout.carter.web.CarterWebParam.PARAM_USER_GROUP;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.EDA_ORDER_ID;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.EVENT_TYPE;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.FEE;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.FEE_SUM;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.HID;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.ITEM_ID;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.MODEL_ID;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.MSKU;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.RGB;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.SHOP_ID;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.SHOW_BLOCK_ID;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.SHOW_UID;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.USER_ID;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.USER_ID_TYPE;
import static ru.yandex.market.checkout.carter.web.logging.CartLogRecordField.WARE_MD5;

/**
 * Created by asafev on 25/04/2017.
 */
public class CartLoggingTest extends CarterMockedDbTestBase {

    private static final String UUID_USER_ID = "12345678";
    private static final String YANDEX_USER_ID = "234";
    private static final Long EXP_SHOP_ID = 111222333L;
    private static final Long EXP_MODEL_ID = 555666777L;
    private static final Long EXP_MSKU = 444555666777888L;
    private static final Long EXP_HID = 99999L;

    @Autowired
    private Carter carterClient;
    private InMemoryAppender appender;

    @BeforeEach
    public void mockLogger() {
        appender = new InMemoryAppender();
        appender.start();

        Logger logger = (Logger) LoggerFactory.getLogger("market-carter-cart.log");
        logger.addAppender(appender);
        appender.clear();
    }

    @AfterEach
    public void removeLoger() {
        Logger logger = (Logger) LoggerFactory.getLogger("market-carter-cart.log");
        logger.detachAppender(appender);
    }

    @Test
    public void createItemLog() {
        ItemOffer itemOffer = generateItemWith("54321", offer -> offer.setFee(CarterTestUtils.FEE));
        long itemId = carterClient.addItem(
                UUID_USER_ID, UserIdType.UID, -1, Color.BLUE, CartConverter.convert(itemOffer), PARAM_USER_GROUP);
        List<Map<String, String>> logs = appender.getTskvMaps();

        assertThat(logs, hasSize(1));
        Map<String, String> logRecord = logs.get(0);
        assertThat(logRecord.get(ITEM_ID.field()), is(Long.toString(itemId)));
        assertThat(logRecord.get(WARE_MD5.field()), is(itemOffer.getObjId()));
        assertThat(logRecord.get(EVENT_TYPE.field()), is("CREATE"));
        assertThat(logRecord.get(FEE.field()), is("0.0200"));
        assertThat(logRecord.get(FEE_SUM.field()), is("3.97"));
        assertThat(logRecord.get(SHOW_BLOCK_ID.field()), is("9691078107358612136"));
        assertThat(logRecord.get(SHOW_UID.field()), is("969107810735861213606008"));
        assertThat(logRecord.get(RGB.field()), is("BLUE"));
    }

    @Test
    public void deleteItemLog() {
        long itemId = carterClient.addItem(
                UUID_USER_ID, UserIdType.UID, -1, Color.BLUE, CartConverter.convert(generateItem("12345")),
                PARAM_USER_GROUP);
        appender.clear();

        carterClient.removeItem(UUID_USER_ID, UserIdType.UID, -1, Color.BLUE, itemId, PARAM_USER_GROUP);
        List<Map<String, String>> logs = appender.getTskvMaps();

        assertThat(logs, hasSize(1));
        assertThat(logs.get(0).get(ITEM_ID.field()), is(Long.toString(itemId)));
        assertThat(logs.get(0).get(WARE_MD5.field()), is("12345"));
        assertThat(logs.get(0).get(EVENT_TYPE.field()), is("DELETE"));
    }

    @Test
    public void bulkgenerateItemsLog() {
        carterClient.addItems(
                UUID_USER_ID, UserIdType.UID, -1, Color.BLUE,
                CartConverter.convert(CarterTestUtils.generateCartList(3)), PARAM_USER_GROUP);
        List<Map<String, String>> logs = appender.getTskvMaps();

        assertThat(logs, hasSize(3));
        assertThat(getObjIds(logs), containsInAnyOrder(0L, 1L, 2L));
        logs.forEach(rec -> {
            assertThat(rec.get(USER_ID.field()), is(UUID_USER_ID));
            assertThat(rec.get(USER_ID_TYPE.field()), is(equalToIgnoringCase(UserIdType.UID.name())));
            assertThat(rec.get(EVENT_TYPE.field()), is("CREATE"));
        });
    }

    @Test
    public void bulkDeleteItemsLog() {
        carterClient.addItems(
                UUID_USER_ID, UserIdType.UID, -1, Color.BLUE,
                CartConverter.convert(CarterTestUtils.generateCartList(3)), PARAM_USER_GROUP);
        List<Long> itemIds = getItemIds(appender.getTskvMaps());
        appender.clear();

        carterClient.removeItems(UUID_USER_ID, UserIdType.UID, -1, Color.BLUE, itemIds, PARAM_USER_GROUP);
        List<Map<String, String>> logs = appender.getTskvMaps();

        assertThat(logs, hasSize(3));
        assertThat(getItemIds(logs), containsInAnyOrder(itemIds.toArray()));
        assertThat(getObjIds(logs), containsInAnyOrder(0L, 1L, 2L));
        logs.forEach(rec -> {
            assertThat(rec.get(USER_ID.field()), is(UUID_USER_ID));
            assertThat(rec.get(USER_ID_TYPE.field()), is(equalToIgnoringCase(UserIdType.UID.name())));
            assertThat(rec.get(EVENT_TYPE.field()), is("DELETE"));
        });
    }

    @Test
    public void offerFieldsInLog() {
        ItemOffer offer = generateItem("54321");
        offer.setShopId(EXP_SHOP_ID);
        offer.setModelId(EXP_MODEL_ID);
        offer.setMsku(EXP_MSKU);
        offer.setHid(EXP_HID);
        long itemId = carterClient.addItem(UUID_USER_ID, UserIdType.UID, -1, Color.BLUE,
                CartConverter.convert(offer), PARAM_USER_GROUP);
        List<Map<String, String>> logs = appender.getTskvMaps();

        assertThat(logs, hasSize(1));
        assertThat(logs.get(0).get(ITEM_ID.field()), is(Long.toString(itemId)));
        assertThat(logs.get(0).get(WARE_MD5.field()), is(offer.getObjId()));
        assertThat(logs.get(0).get(EVENT_TYPE.field()), is("CREATE"));
        assertThat(logs.get(0).get(SHOP_ID.field()), is(EXP_SHOP_ID.toString()));
        assertThat(logs.get(0).get(MODEL_ID.field()), is(EXP_MODEL_ID.toString()));
        assertThat(logs.get(0).get(MSKU.field()), is(EXP_MSKU.toString()));
        assertThat(logs.get(0).get(HID.field()), is(EXP_HID.toString()));
    }

    @Test
    public void mergeLog() {
        long itemId = carterClient.addItem(
                YANDEX_USER_ID, UserIdType.YANDEXUID, -1, Color.BLUE, CartConverter.convert(generateItem("obj01")),
                PARAM_USER_GROUP);
        appender.clear();

        carterClient.mergeItems(YANDEX_USER_ID, UserIdType.YANDEXUID, UUID_USER_ID, UserIdType.UID, Color.BLUE);

        List<Map<String, String>> logs = appender.getTskvMaps();
        assertThat(logs, hasSize(3));
        assertThat(
                logs.stream()
                        .map(r -> r.get(EVENT_TYPE.field()))
                        .collect(Collectors.toList()),
                containsInAnyOrder("MERGE", "CREATE", "DELETE")
        );
        Map<String, String> deleteRecord = logs.stream()
                .filter(r -> r.get(EVENT_TYPE.field()).equals("MERGE"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No merge action in log after merge"));
        Map<String, String> createRecord = logs.stream()
                .filter(r -> r.get(EVENT_TYPE.field()).equals("CREATE"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No CREATE action in log after merge"));
        assertThat(deleteRecord.get(ITEM_ID.field()), is(Long.toString(itemId)));
        assertThat(createRecord.get(ITEM_ID.field()), not(Long.toString(itemId)));
    }

    private List<Long> getItemIds(List<Map<String, String>> records) {
        return records.stream()
                .map(r -> r.get(ITEM_ID.field()))
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    private List<Long> getObjIds(List<Map<String, String>> records) {
        return records.stream()
                .map(r -> r.get(WARE_MD5.field()))
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    /**
     * Тест на проверку хаковой ручки с проставлением edaOrderId
     *
     * @see ru.yandex.market.checkout.carter.web.CartResourceController#tmpHackingBulkDeleteItems
     */
    @Test
    public void tmpHackBulkDeleteItemsLog() {
        carterClient.addItems(
                UUID_USER_ID, UserIdType.UID, -1, Color.BLUE,
                CartConverter.convert(CarterTestUtils.generateCartList(3)), PARAM_USER_GROUP);
        List<Long> itemIds = getItemIds(appender.getTskvMaps());
        appender.clear();

        String expectedEdaOrderId = "eda-1503";

        carterClient.tmpHackRemoveItems(UUID_USER_ID, UserIdType.UID, -1, Color.BLUE, itemIds, PARAM_USER_GROUP,
                expectedEdaOrderId);
        List<Map<String, String>> logs = appender.getTskvMaps();

        assertThat(logs, hasSize(3));
        assertThat(getItemIds(logs), containsInAnyOrder(itemIds.toArray()));
        assertThat(getObjIds(logs), containsInAnyOrder(0L, 1L, 2L));
        logs.forEach(rec -> {
            assertThat(rec.get(USER_ID.field()), is(UUID_USER_ID));
            assertThat(rec.get(USER_ID_TYPE.field()), is(equalToIgnoringCase(UserIdType.UID.name())));
            assertThat(rec.get(EVENT_TYPE.field()), is("DELETE"));
            assertThat(logs.get(0).get(EDA_ORDER_ID.field()), is(expectedEdaOrderId));
        });
    }
}

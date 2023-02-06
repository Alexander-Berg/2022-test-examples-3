package ru.yandex.market.crm.triggers.services.active.factories.list;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.junit.Test;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.domain.trigger.variables.pdo.PdoEventTypes;
import ru.yandex.market.crm.core.domain.trigger.variables.pdo.PdoProductMessageData;
import ru.yandex.market.crm.core.domain.trigger.variables.pdo.PdoProductState;
import ru.yandex.market.crm.core.domain.trigger.variables.pdo.PdoProductsEventData;
import ru.yandex.market.crm.core.domain.trigger.variables.pdo.PdoProductsTable;
import ru.yandex.market.crm.core.domain.trigger.variables.pdo.ProductKey;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.test.utils.report.Offer;
import ru.yandex.market.crm.core.test.utils.report.Offer.Delivery;
import ru.yandex.market.crm.core.test.utils.report.Offer.Delivery.Region;
import ru.yandex.market.crm.core.test.utils.report.Offer.Model;
import ru.yandex.market.crm.core.test.utils.report.Offer.OfferPrices;
import ru.yandex.market.crm.core.test.utils.report.Offer.OfferPrices.Discount;
import ru.yandex.market.crm.core.test.utils.report.ReportTestHelper;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelType;
import ru.yandex.market.crm.triggers.services.active.MessageData;
import ru.yandex.market.crm.triggers.services.active.ProcessInfo;
import ru.yandex.market.crm.triggers.test.AbstractServiceTest;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.model;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.modelPrices;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.offer;

/**
 * @author apershukov
 */
public class PdoProductListMessageFactoryTest extends AbstractServiceTest {

    private static ProcessInfo processInfo(ModelInfo modelInfo) {
        ProductKey key = new ProductKey(Color.GREEN, modelInfo.getType(), modelInfo.getId());

        PdoProductState productState = new PdoProductState(key);
        productState.setRegion(REGION_ID);
        productState.setCount(1);
        productState.setModelInfo(modelInfo);

        String discount = modelInfo.getDiscount();
        productState.setMaxDiscount(discount == null ? 0 : Integer.parseInt(discount));

        PdoProductsTable productsTable = new PdoProductsTable();
        productsTable.update(List.of(productState));

        var vars = Map.<String, Object>of(ProcessVariablesNames.PRODUCTS_TABLE, productsTable);
        return new ProcessInfo("test_process", vars);
    }

    private static ModelInfo info(ModelType type, String id, String price, String oldPrice, int discount) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setType(type);
        modelInfo.setId(id);
        modelInfo.setOnstock(true);
        modelInfo.setOldPrice(oldPrice);
        modelInfo.setPrice(price);
        modelInfo.setDiscount(String.valueOf(discount));
        return modelInfo;
    }

    private ModelInfo offerInfo(String price, String oldPrice, int discount) {
        return info(ModelType.OFFER, OFFER_ID, price, oldPrice, discount);
    }

    private ModelInfo modelInfo(String price) {
        return info(ModelType.MODEL, String.valueOf(MODEL_ID), price, price, 0);
    }

    private OfferPrices prices(String price, String oldPrice, int discount) {
        return new OfferPrices()
                .setValue(price)
                .setDiscount(new Discount(oldPrice, discount));
    }

    private static final long REGION_ID = 213;
    private static final String OFFER_ID = "test-offer";
    private static final long MODEL_ID = 111;

    private static final ProductKey OFFER_KEY = new ProductKey(Color.GREEN, ModelType.OFFER, OFFER_ID);
    private static final ProductKey MODEL_KEY = new ProductKey(Color.GREEN, ModelType.MODEL, String.valueOf(MODEL_ID));

    @Inject
    private ReportTestHelper reportTestHelper;

    @Inject
    private PdoProductListMessageFactory factory;

    /**
     * Если на оффер появилась скидка и при этом он пропал из наличия, событие
     * на появление скидки не генерируется. Герерируется только событие выхода из продажи.
     *
     * По мотивам https://st.yandex-team.ru/LILUCRM-3512
     */
    @Test
    public void testNoDiscountEventWithOutOfStock() {
        OfferPrices prices = prices("10", "20", 50);

        Delivery delivery = new Delivery()
                .setShopPriorityRegion(new Region(REGION_ID))
                .setInStock(false);

        prepareOffers(
                offer(OFFER_ID).setPrices(prices)
                        .setDelivery(delivery)
        );

        ModelInfo modelInfo = offerInfo("20", "20", 0);
        var messages = generateEvents(modelInfo);
        assertThat(messages, hasSize(1));

        var message = messages.get(0);
        assertEquals(OFFER_KEY, message.getProductKey());
        assertEquals(PdoEventTypes.OUT_OF_STOCK, message.getMessageType());
    }

    /**
     * Если на позицию со скидкой появилась еще большая скидка благодаря которой цена действительно уменьшилась
     * генерируется событие появления новой скидки
     */
    @Test
    public void testGenerateDiscountEventForBiggerDiscount() {
        OfferPrices prices = prices("7500", "10000", 25);

        prepareOffers(
                offer(OFFER_ID).setPrices(prices)
        );

        ModelInfo modelInfo = offerInfo("9 000", "10 000", 10);

        var message = generateEvents(modelInfo).stream()
                .filter(x -> PdoEventTypes.NEW_DISCOUNT.equals(x.getMessageType()))
                .findAny().orElseThrow();

        assertEquals(OFFER_KEY, message.getProductKey());
        assertEquals(PdoEventTypes.NEW_DISCOUNT, message.getMessageType());
    }

    /**
     * Если у оффера появилась значительно большая скидка, но при этом его реальная
     * цена не изменилась, событие появления скидки не генерируется.
     *
     * По мотивам https://st.yandex-team.ru/LILUCRM-3512
     */
    @Test
    public void testDoNotGenerateDiscountEventIfPriceHasNotReallyDropped() {
        OfferPrices prices = prices("100", "200", 50);

        prepareOffers(
                offer(OFFER_ID).setPrices(prices)
        );

        ModelInfo modelInfo = offerInfo("100", "150", 33);
        var message = factory.create(processInfo(modelInfo));
        assertNull(message);
    }

    /**
     * Если цена на отслеживаемую модель значительно упала генерируется событие о снежении цены
     */
    @Test
    public void testGenerateNewModelMinPriceEvent() {
        Offer[] offers = new Offer[] {
                offer().setModel(new Model(MODEL_ID))
                        .setPrices(prices("100", "100", 0))
        };

        reportTestHelper.prepareModel(Color.GREEN, REGION_ID,
                model(MODEL_ID)
                        .setOffers(offers)
                        .setPrices(modelPrices(100))
        );

        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID, offers);

        ModelInfo modelInfo = modelInfo("200");
        var messages = generateEvents(modelInfo);
        assertThat(messages, hasSize(1));

        var message = messages.get(0);
        assertEquals(MODEL_KEY, message.getProductKey());
        assertEquals(PdoEventTypes.NEW_MIN_PRICE, message.getMessageType());
    }

    /**
     * Если на модель появлась большая скидка но реальная её цена при этом не изменилась
     * событие на снижение цены не генерируется.
     */
    @Test
    public void testDoNotGenerateNewMinPriceEventIfPriceHasNotReallyChanged() {
        Offer[] offers = new Offer[] {
                offer().setModel(new Model(MODEL_ID))
                        .setPrices(prices("100", "200", 50))
        };

        reportTestHelper.prepareModel(Color.GREEN, REGION_ID,
                model(MODEL_ID)
                        .setOffers(offers)
                        .setPrices(modelPrices(100))
        );

        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID, offers);

        ModelInfo modelInfo = modelInfo("100");

        MessageData data = factory.create(processInfo(modelInfo));
        assertNull(data);
    }

    @Nonnull
    private List<PdoProductMessageData> generateEvents(ModelInfo modelInfo) {
        MessageData data = factory.create(processInfo(modelInfo));
        assertNotNull("No message generated", data);

        PdoProductsEventData eventData = (PdoProductsEventData) data.getVars()
                .get(ProcessVariablesNames.PRODUCTS_MESSAGE_DATA);

        assertNotNull(eventData);

        return eventData.getData();
    }

    private void prepareOffers(Offer... offers) {
        reportTestHelper.prepareOffers(Color.GREEN, REGION_ID, offers);
    }
}

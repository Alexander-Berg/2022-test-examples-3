package ru.yandex.market.crm.triggers.services.active.factories;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.test.utils.report.ReportTestHelper;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.triggers.services.active.MessageData;
import ru.yandex.market.crm.triggers.services.active.ProcessInfo;
import ru.yandex.market.crm.triggers.services.bpm.variables.ModelOffersInfo;
import ru.yandex.market.crm.triggers.services.bpm.variables.OfferInfo;
import ru.yandex.market.crm.triggers.test.AbstractServiceTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.model;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.modelPrices;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.offer;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.offerPrices;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.promo;

/**
 * @author apershukov
 */
public class ModelNewPromoMessageFactoryTest extends AbstractServiceTest {

    private static final long MODEL_ID = 212121;
    private static final long REGION_ID = 213;

    private static final String PROMO_NAME = "Promo Name";
    private static final String PROMO_TYPE = "test_promo_type";

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private ModelNewPromoMessageFactory factory;

    @Inject
    private ReportTestHelper reportTestHelper;

    @Before
    public void setUp() {
        reportTestHelper.prepareModel(Color.GREEN, REGION_ID, model(MODEL_ID));
    }

    /**
     * Если найдено новое промо отправляется сообщение. При этом это
     * промо отмечается в переменной процесса как просмотренное
     */
    @Test
    public void testSendMessageOnPromo() {
        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID,
                offer().setPromo(
                        promo(PROMO_TYPE, PROMO_NAME)
                )
        );

        MessageData messageData = factory.create(processInfo());
        assertNotNull("Message should've been sent", messageData);

        Map<String, Object> vars = messageData.getVars();
        assertNotNull(vars);

        OfferInfo offerInfo = (OfferInfo) messageData.getVars().get(ProcessVariablesNames.OFFER_INFO);
        assertNotNull("Offer info must be specified", offerInfo);

        assertEquals(PROMO_NAME, offerInfo.getPromoName());

        assertFalse(vars.containsKey(ProcessVariablesNames.MODEL_REGION_OFFERS_INFO));
    }

    /**
     * Если у офферов модели промо не найдено сообщение не отправляется
     */
    @Test
    public void testDoNotSendMessageIfNoPromo() {
        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID,
                offer(),
                offer(),
                offer()
        );

        MessageData messageData = factory.create(processInfo());
        assertNull("Message should not have been sent", messageData);
    }

    /**
     * Если у офферов модели встречаются только просмотренные промо сообщение не отправляется
     */
    @Test
    public void testDoNotSendMessageIfModelAlreadyHadPromo() {
        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID,
                offer().setPromo(
                        promo(PROMO_TYPE, PROMO_NAME)
                )
        );

        MessageData messageData = factory.create(processInfo(PROMO_TYPE));
        assertNull("Message should not have been sent", messageData);
    }

    /**
     * Промо с типом "batch" игнорируется
     */
    @Test
    public void testIgnoreBatchPromoType() {
        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID,
                offer().setPromo(
                        promo("batch", PROMO_NAME)
                )
        );

        MessageData messageData = factory.create(processInfo());
        assertNull("Message should not have been sent", messageData);
    }

    /**
     * Корректно устанавливаются значения оффера при появлении нового промо
     */
    @Test
    public void testSetNewPromoOffer() {
        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID,
                offer().setPromo(
                        promo(PROMO_TYPE, PROMO_NAME)
                ).setPrices(offerPrices(100))
        );

        MessageData messageData = factory.create(processInfo());
        assertNotNull(messageData);

        Map<String, Object> vars = messageData.getVars();
        assertNotNull(vars);

        ModelInfo actualModel = (ModelInfo) vars.get(ProcessVariablesNames.MODEL);
        assertNotNull("Actual model must be specified", actualModel);

        OfferInfo offerInfo = (OfferInfo) vars.get(ProcessVariablesNames.OFFER_INFO);
        assertNotNull("Offer info must be specified", offerInfo);

        assertEquals(PROMO_NAME, offerInfo.getPromoName());
        assertEquals(PROMO_TYPE, offerInfo.getPromoType());
        assertEquals("100", offerInfo.getPrice());
        assertEquals("100", offerInfo.getOldPrice());
    }

    /**
     * Если актуальное состояние модели изменилось, то при появлении нового промо будет установлена актуальная
     * минимальная стоимость товара в поле price
     */
    @Test
    public void testCorrectSetNewActualPrice() {
        reportTestHelper.prepareModel(Color.GREEN, REGION_ID, model(MODEL_ID).setPrices(modelPrices(80)));
        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID,
                offer().setPromo(
                        promo(PROMO_TYPE, PROMO_NAME)
                ).setPrices(offerPrices(100))
        );

        MessageData messageData = factory.create(processInfo());
        assertNotNull(messageData);

        Map<String, Object> vars = messageData.getVars();
        assertNotNull(vars);

        ModelInfo actualModel = (ModelInfo) vars.get(ProcessVariablesNames.MODEL);
        assertNotNull("Actual model must be specified", actualModel);
        assertEquals("80", actualModel.getPrice());
    }

    private ProcessInfo processInfo(String... initialPromos) {
        ModelInfo initialModel = new ModelInfo();
        initialModel.setId(String.valueOf(MODEL_ID));
        initialModel.setOnstock(true);

        ModelOffersInfo initialInfo = new ModelOffersInfo();
        initialInfo.setColor(Color.GREEN.toString());
        initialInfo.setRegion(REGION_ID);
        initialInfo.setModelInfo(initialModel);
        initialInfo.setPromoTypes(Set.of(initialPromos));

        return new ProcessInfo("test_process", Map.of(
                ProcessVariablesNames.MODEL_REGION_OFFERS_INFO,
                jsonSerializer.writeObjectAsString(initialInfo)
        ));
    }
}

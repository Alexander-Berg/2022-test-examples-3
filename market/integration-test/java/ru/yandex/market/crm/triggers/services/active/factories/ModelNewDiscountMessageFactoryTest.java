package ru.yandex.market.crm.triggers.services.active.factories;

import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.test.utils.report.Offer.OfferPrices.Discount;
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
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.offer;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.offerPrices;

/**
 * @author apershukov
 */
public class ModelNewDiscountMessageFactoryTest extends AbstractServiceTest {

    private static final long MODEL_ID = 212121;
    private static final long REGION_ID = 213;

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private ModelNewDiscountMessageFactory factory;

    @Inject
    private ReportTestHelper reportTestHelper;

    @Before
    public void setUp() {
        reportTestHelper.prepareModel(Color.GREEN, REGION_ID, model(MODEL_ID));
    }

    /**
     * В случае если на модель без скидки появилась скидка отправляется сообщение
     */
    @Test
    public void testSendMessageIfDiscountOfferFound() {
        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID,
                offer().setPrices(
                        offerPrices(100)
                                .setDiscount(new Discount()
                                        .setOldMin("200")
                                        .setPercent(50)
                                )
                ),
                offer().setPrices(offerPrices(250))
        );

        MessageData messageData = factory.create(processInfo());
        assertNotNull("Message should've been sent", messageData);

        Map<String, Object> vars = messageData.getVars();
        assertNotNull(vars);

        ModelInfo modelInfo = (ModelInfo) vars.get(ProcessVariablesNames.MODEL);
        assertNotNull("Model info must be specified", modelInfo);

        OfferInfo offerInfo = (OfferInfo) vars.get(ProcessVariablesNames.OFFER_INFO);
        assertNotNull("Offer info must be specified", offerInfo);

        assertEquals("200", offerInfo.getOldPrice());
        assertEquals("100", offerInfo.getPrice());
        assertEquals("50", offerInfo.getDiscount());

        assertFalse(vars.containsKey(ProcessVariablesNames.MODEL_REGION_OFFERS_INFO));
    }

    /**
     * В случае если скидки не появились сообщение не отправляется
     */
    @Test
    public void testDoNotSendMessageIfNoDiscount() {
        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID,
                offer().setPrices(offerPrices(200))
        );

        MessageData messageData = factory.create(processInfo());
        assertNull("Message should not have been sent", messageData);
    }

    /**
     * В случае если изменение процента скидки незначительное (менее 7%)
     * сообщение не отправляется
     */
    @Test
    public void testDoNotSendMessageIfDiscountChangeIsInsignificant() {
        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID,
                offer().setPrices(
                        offerPrices(150)
                                .setDiscount(new Discount()
                                        .setOldMin("200")
                                        .setPercent(25)
                                )
                )
        );

        MessageData messageData = factory.create(processInfo(20));
        assertNull("Message should not have been sent", messageData);
    }

    /**
     * В случае если есть несколько офферов со скидками сообщение отправляется
     * с самой большой скидкой
     */
    @Test
    public void testSendMessageWithBiggestDiscount() {
        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID,
                offer().setPrices(offerPrices(200)),
                offer().setPrices(
                        offerPrices(182)
                                .setDiscount(new Discount()
                                        .setOldMin("200")
                                        .setPercent(9)
                                )
                ),
                offer().setPrices(
                        offerPrices(170)
                                .setDiscount(new Discount()
                                        .setOldMin("200")
                                        .setPercent(15)
                                )
                )
        );

        MessageData messageData = factory.create(processInfo());
        assertNotNull("Message should've been sent", messageData);

        OfferInfo offerInfo = (OfferInfo) messageData.getVars().get(ProcessVariablesNames.OFFER_INFO);
        assertNotNull("Offer info must be specified", offerInfo);

        assertEquals("200", offerInfo.getOldPrice());
        assertEquals("170", offerInfo.getPrice());
        assertEquals("15", offerInfo.getDiscount());
    }

    /**
     * В случае если репорт не вернул информацию о скидке в оффере
     * фабрика корректно завершает выполнение и сообщение не отправляется (считаем что скидка 0%)
     */
    @Test
    public void testProcessOfferWithoutDiscount() {
        reportTestHelper.prepareModelOffers(Color.GREEN, REGION_ID, MODEL_ID,
                offer().setPrices(
                        offerPrices(200)
                                .setDiscount(null)
                )
        );

        MessageData messageData = factory.create(processInfo());
        assertNull("Message should not have been sent", messageData);
    }

    private ProcessInfo processInfo(int maxDiscount) {
        ModelInfo initialModel = new ModelInfo();
        initialModel.setId(String.valueOf(MODEL_ID));
        initialModel.setOnstock(true);

        ModelOffersInfo initialInfo = new ModelOffersInfo();
        initialInfo.setColor(Color.GREEN.toString());
        initialInfo.setRegion(REGION_ID);
        initialInfo.setModelInfo(initialModel);
        initialInfo.setMaxDiscount(maxDiscount);

        return new ProcessInfo("test_process", Map.of(
                ProcessVariablesNames.MODEL_REGION_OFFERS_INFO,
                jsonSerializer.writeObjectAsString(initialInfo)
        ));
    }

    private ProcessInfo processInfo() {
        return processInfo(0);
    }
}

package ru.yandex.market.crm.triggers.services.active.factories;

import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.test.utils.report.ReportTestHelper;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.triggers.services.active.MessageData;
import ru.yandex.market.crm.triggers.services.active.ProcessInfo;
import ru.yandex.market.crm.triggers.services.bpm.variables.ModelOffersInfo;
import ru.yandex.market.crm.triggers.test.AbstractServiceTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.model;
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.modelPrices;

/**
 * @author apershukov
 */
public class ModelNewMinPriceMessageFactoryTest extends AbstractServiceTest {

    private static final long MODEL_ID = 212121;
    private static final long REGION_ID = 213;
    private static final int INITIAL_PRICE = 200;

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private ReportTestHelper reportTestHelper;

    @Inject
    private ModelNewMinPriceMessageFactory factory;

    /**
     * В случае если минимальная цена на модель упала отправляется сообщение
     */
    @Test
    public void testSendMessageIfPriceDropped() {
        reportTestHelper.prepareModel(Color.GREEN, REGION_ID,
                model(MODEL_ID)
                        .setPrices(modelPrices(100))
        );

        MessageData messageData = factory.create(processInfo());
        assertNotNull(messageData);

        Map<String, Object> vars = messageData.getVars();
        assertNotNull(vars);

        ModelInfo actualModel = (ModelInfo) vars.get(ProcessVariablesNames.MODEL);
        assertNotNull("Actual model must be specified", actualModel);
        assertEquals("100", actualModel.getPrice());
        assertEquals(String.valueOf(INITIAL_PRICE), actualModel.getOldPrice());
        assertEquals("50", actualModel.getDiscount());

        assertFalse(vars.containsKey(ProcessVariablesNames.MODEL_REGION_OFFERS_INFO));
    }

    /**
     * В случае если цена на модель не изменилась сообщение не отправляется
     */
    @Test
    public void testDoNotSendMessageIfModelPriceIsTheSame() {
        reportTestHelper.prepareModel(Color.GREEN, REGION_ID,
                model(MODEL_ID)
                        .setPrices(modelPrices(INITIAL_PRICE))
        );

        MessageData messageData = factory.create(processInfo());
        assertNull("Message must not be sent", messageData);
    }

    /**
     * В случае если цена на модель снизилась незначительно (меньше чем на 7%)
     * сообщение не отправляется
     */
    @Test
    public void testDoNotSendMessageIfPriceDropIsNotNotable() {
        reportTestHelper.prepareModel(Color.GREEN, REGION_ID,
                model(MODEL_ID)
                        .setPrices(modelPrices(195))
        );

        MessageData messageData = factory.create(processInfo());
        assertNull("Message must not be sent", messageData);
    }

    /**
     * В случае если модель не удалось найти сообщение не отправляется.
     * При этом метод корректно завершает работу
     */
    @Test
    public void testDoNotSendMessageIfModelHasGoneOutOfStock() {
        reportTestHelper.prepareNotExistingModel(Color.GREEN, REGION_ID, MODEL_ID);

        MessageData messageData = factory.create(processInfo());
        assertNull("Message must not be sent", messageData);
    }

    private ProcessInfo processInfo() {
        ModelInfo initialModel = new ModelInfo();
        initialModel.setId(String.valueOf(MODEL_ID));
        initialModel.setOnstock(true);
        initialModel.setPrice(String.valueOf(INITIAL_PRICE));

        ModelOffersInfo initialInfo = new ModelOffersInfo();
        initialInfo.setColor(Color.GREEN.toString());
        initialInfo.setRegion(REGION_ID);
        initialInfo.setModelInfo(initialModel);

        return new ProcessInfo("test_process", Map.of(
                ProcessVariablesNames.MODEL_REGION_OFFERS_INFO,
                jsonSerializer.writeObjectAsString(initialInfo)
        ));
    }
}

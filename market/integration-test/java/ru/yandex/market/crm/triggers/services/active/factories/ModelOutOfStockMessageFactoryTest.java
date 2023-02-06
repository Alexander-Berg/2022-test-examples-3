package ru.yandex.market.crm.triggers.services.active.factories;

import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.test.utils.report.ReportTestHelper;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
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
import static ru.yandex.market.crm.core.test.utils.report.ReportTestHelper.offer;

/**
 * @author apershukov
 */
public class ModelOutOfStockMessageFactoryTest extends AbstractServiceTest {

    private static final long MODEL_ID = 212121;
    private static final long REGION_ID = 213;
    private static final String INITIAL_PRICE = "9.99";

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private ReportTestHelper reportTestHelper;

    @Inject
    private ModelOutOfStockMessageFactory factory;

    /**
     * В случае если модель пропала из наличия отправляется сообщение
     */
    @Test
    public void testCreateMessageIfModelIsGone() {
        reportTestHelper.prepareModel(Color.GREEN, REGION_ID,
                model(MODEL_ID)
                    .setPrices(modelPrices(100500))
        );

        MessageData messageData = factory.create(processInfo());

        assertNotNull("No message created", messageData);

        Map<String, Object> vars = messageData.getVars();
        assertNotNull(vars);

        ModelInfo model = (ModelInfo) vars.get(ProcessVariablesNames.MODEL);
        assertNotNull("Model variable must be specified", model);
        assertFalse(model.isOnstock());

        String rawModelOffersInfo = (String) vars.get(ProcessVariablesNames.MODEL_REGION_OFFERS_INFO);
        assertNotNull("Offers info variable is not null", rawModelOffersInfo);

        ModelOffersInfo modelOffersInfo = jsonDeserializer.readObject(ModelOffersInfo.class, rawModelOffersInfo);

        ModelInfo modelFromInfo = modelOffersInfo.getModelInfo();
        assertNotNull(modelFromInfo);
        assertFalse(modelFromInfo.isOnstock());
        assertEquals(INITIAL_PRICE, modelFromInfo.getPrice());
    }

    /**
     * В случае если модель все еще в наличии сообщение не отправляется
     */
    @Test
    public void testDoNotCreateMessageIfModelIsStillInStock() {
        reportTestHelper.prepareModel(Color.GREEN, REGION_ID,
                model(MODEL_ID)
                        .setOffers(offer(), offer())
        );

        MessageData messageData = factory.create(processInfo());
        assertNull(messageData);
    }

    private ProcessInfo processInfo() {
        ModelInfo initialModel = new ModelInfo();
        initialModel.setId(String.valueOf(MODEL_ID));
        initialModel.setOnstock(true);
        initialModel.setPrice(INITIAL_PRICE);

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

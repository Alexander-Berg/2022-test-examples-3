package ru.yandex.market.partner.mvc.controller.moderation.serialization;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;

import ru.yandex.market.common.test.SerializationChecker;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.partner.model.PremoderationState;
import ru.yandex.market.partner.mvc.MvcTestSerializationConfig;

/**
 * @author Vadim Lyalin
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MvcTestSerializationConfig.class)
public class PremoderationStateSerializationTest {
    @Autowired
    private SerializationChecker checker;

    /**
     * Тест на сериализацию пустого ответа
     */
    @Test
    public void testEmptySerialization() throws IOException, SAXException, JSONException {
        PremoderationState premoderationState = new PremoderationState(Collections.emptyList());

        checker.testJsonSerialization(premoderationState, "{programStates:[]}");
    }

    /**
     * Тест на сериализацию заполненного ответа
     */
    @Test
    public void testSerialization() throws IOException, SAXException, JSONException {
        PremoderationState premoderationState = new PremoderationState(Arrays.asList(
                new PremoderationState.ProgramState(ShopProgram.CPA, null, TestingStatus.DISABLED),
                new PremoderationState.ProgramState(ShopProgram.CPC, 1000L, TestingStatus.CHECKING)));

        checker.testJsonSerialization(premoderationState,
                "{programStates:[" +
                        "{program: 'CPA', testingStatus: '10'}," +
                        "{program: 'CPC', startDate: 1000, testingStatus: '4'}" +
                        "]}");
    }
}

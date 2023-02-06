package ru.yandex.market.partner.mvc.controller.cpc.serialization;

import java.io.IOException;

import com.google.common.collect.ImmutableList;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;

import ru.yandex.market.common.test.SerializationChecker;
import ru.yandex.market.core.cpc.CPC;
import ru.yandex.market.core.cpc.CpcState;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.partner.mvc.MvcTestSerializationConfig;

/**
 * @author fbokovikov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MvcTestSerializationConfig.class)
public class CpcStateSerializationTest {

    @Autowired
    private SerializationChecker checker;

    @Test
    public void testCpcStateOutput() throws IOException, SAXException, JSONException {
        CpcState cpcState = new CpcState(CPC.NONE,
                ImmutableList.of(
                        CutoffType.COMMON_OTHER,
                        CutoffType.CLONE,
                        CutoffType.TECHNICAL_NEED_INFO
                ), false, true);
        checker.testJsonSerialization(cpcState,
                "{\"cpc\":\"NONE\",\"cpcCutoffs\":[\"42\",\"7\",\"24\"],\"canSwitchToOn\":false, " +
                        "\"passedModerationOnce\": true}");
    }
}

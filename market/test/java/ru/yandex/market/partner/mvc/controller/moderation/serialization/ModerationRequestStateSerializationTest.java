package ru.yandex.market.partner.mvc.controller.moderation.serialization;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;

import com.google.common.collect.ImmutableMap;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;

import ru.yandex.market.common.test.SerializationChecker;
import ru.yandex.market.core.moderation.ModerationDisabledReason;
import ru.yandex.market.core.moderation.request.ModerationRequestState;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.partner.mvc.MvcTestSerializationConfig;

/**
 * @author fbokovikov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = MvcTestSerializationConfig.class)
public class ModerationRequestStateSerializationTest {

    @Autowired
    private SerializationChecker checker;

    @Test
    public void testModerationRequestOutput() throws IOException, SAXException, JSONException {

        ModerationRequestState moderationState = new ModerationRequestState(
                ImmutableMap.of(
                        ShopProgram.CPC,
                        EnumSet.of(
                                ModerationDisabledReason.MISSED_DATASOURCE_PARAMS,
                                ModerationDisabledReason.FATAL_CUTOFFS
                        ),
                        ShopProgram.CPA,
                        EnumSet.of(
                                ModerationDisabledReason.MISSED_DATASOURCE_PARAMS,
                                ModerationDisabledReason.FATAL_CUTOFFS
                        )
                ), 5
        );

        checker.testJsonSerialization(moderationState,
                "{\"moderationEnabled\":false,\"attemptsLeft\":5," +
                        "\"testingTypes\":[]," +
                        "\"cpcModerationDisabledReasons\":[\"MISSED_DATASOURCE_PARAMS\",\"FATAL_CUTOFFS\"]," +
                        "\"cpaModerationDisabledReasons\":[\"MISSED_DATASOURCE_PARAMS\",\"FATAL_CUTOFFS\"]," +
                        "\"internalTestingTypes\":[]" +
                        "}");

        moderationState = new ModerationRequestState(
                ImmutableMap.of(
                        ShopProgram.CPC, Collections.emptySet(),
                        ShopProgram.CPA, Collections.emptySet(),
                        ShopProgram.GENERAL, EnumSet.of(ModerationDisabledReason.MODERATION_NOT_NEEDED)
                ), 2
        );

        checker.testJsonSerialization(moderationState,
                "{\"moderationEnabled\":true,\"attemptsLeft\":2," +
                        "\"testingTypes\":[\"CPC\",\"CPA\"]," +
                        "\"cpcModerationDisabledReasons\":[]," +
                        "\"cpaModerationDisabledReasons\":[]," +
                        "\"internalTestingTypes\":[\"CPC\",\"CPA\"]" +
                        "}");


    }
}

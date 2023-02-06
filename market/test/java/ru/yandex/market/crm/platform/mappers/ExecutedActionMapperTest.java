package ru.yandex.market.crm.platform.mappers;

import java.util.Collection;

import org.junit.Test;

import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.ExecutedAction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author apershukov
 */
public class ExecutedActionMapperTest {

    private ExecutedActionMapper mapper = new ExecutedActionMapper();

    @Test
    public void testParse() {
        Collection<ExecutedAction> actions = mapper.mapLine(
                "tskv\t" +
                "timestamp=1547919665000\t" +
                "actionId=mega_action\t" +
                "segmentId=seg_123\t" +
                "idValue=12345\t" +
                "idType=PUID\t" +
                "variant=mega_action_a"
        );

        assertNotNull(actions);
        assertEquals(1, actions.size());

        ExecutedAction action = actions.iterator().next();

        assertEquals(1547919665000L, action.getTimestamp());
        assertEquals("mega_action", action.getAction());
        assertEquals("seg_123", action.getSegment());
        assertEquals(12345, action.getUid().getIntValue());
        assertEquals(UidType.PUID, action.getUid().getType());
    }

    @Test
    public void testParseWithYuid() {
        Collection<ExecutedAction> actions = mapper.mapLine(
                "tskv\t" +
                "timestamp=1547919665000\t" +
                "actionId=mega_action\t" +
                "segmentId=seg_123\t" +
                "idValue=12345\t" +
                "idType=YUID\t" +
                "variant=mega_action_a"
        );

        assertNotNull(actions);
        assertEquals(1, actions.size());

        ExecutedAction action = actions.iterator().next();
        assertEquals("12345", action.getUid().getStringValue());
        assertEquals(UidType.YANDEXUID, action.getUid().getType());
    }
}

package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;
import ru.yandex.market.crm.triggers.services.bpm.variables.ProductItemChange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WishlistConsumerTest {

    private WishlistConsumer consumer;

    @Before
    public void setUp() {
        LogTypesResolver logTypes = mock(LogTypesResolver.class);
        when(logTypes.getLogIdentifier("pers.wishListChanges"))
                .thenReturn(new LogIdentifier(null, null, LBInstallation.LOGBROKER));

        consumer = new WishlistConsumer(mock(MessageSender.class), logTypes);
    }

    @Test
    public void makeBpmMessageTest() {
        Map<String, String> messageMap = ImmutableMap.of(
                "puid", "0",
                "uuid", "112233",
                "action", "1",
                "model_id", "123321",
                "region_id", "54"
        );

        UidBpmMessage bpmMessage = consumer.asBpmMessage(messageMap);

        assertNotNull(bpmMessage);
        assertEquals(Uid.asUuid("112233"), bpmMessage.getUid());
        assertEquals(MessageTypes.WISHLIST_ITEM_ADDED, bpmMessage.getType());

        Map<String, Object> vars = bpmMessage.getVariables();
        assertEquals(3, vars.size());

        ProductItemChange productItemChange =
                (ProductItemChange) vars.get(ProcessVariablesNames.Event.PRODUCT_ITEM_CHANGE);
        assertEquals(
                "123321",
                productItemChange.getProductItem().getModelId()
        );
        assertEquals(Long.valueOf(54), productItemChange.getRegionId());
        assertEquals(54L, vars.get(ProcessVariablesNames.REGION_ID));
        assertEquals("123321", vars.get(ProcessVariablesNames.PRODUCT_ITEM_ID));
    }

    @Test
    public void bpmMessageIsSkipedTest() {
        ImmutableMap<String, String> noUserId = ImmutableMap.of(
                "action", "1",
                "model_id", "123321",
                "region_id", "54"
        );

        assertNull(consumer.asBpmMessage(noUserId));

        ImmutableMap<String, String> badAction = ImmutableMap.of(
                "puid", "112233",
                "action", "3",
                "model_id", "123321",
                "region_id", "54"
        );

        assertNull(consumer.asBpmMessage(badAction));

        ImmutableMap<String, String> regionIsZero = ImmutableMap.of(
                "puid", "112233",
                "action", "3",
                "model_id", "123321",
                "region_id", "0"
        );

        assertNull(consumer.asBpmMessage(regionIsZero));
    }
}

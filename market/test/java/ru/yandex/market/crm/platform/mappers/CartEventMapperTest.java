package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.models.CartEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author apershukov
 */
public class CartEventMapperTest {

    private CartEventMapper mapper;

    @Before
    public void setUp() {
        mapper = new CartEventMapper();
    }

    @Test
    public void testMapping() {
        String line = "tskv\tevent_time=2018-06-19T12:40:26+0300\tevent_type=CREATE\tuser_id=658839248\t" +
                "user_id_type=UID\trgb=GREEN\tlist_id=34501309\titem_id=26836426\titem_type=OFFER\titem_count=1\t" +
                "ware_md5=UfV1k-33uct98giLver0wA\tmodel_id=10404626\thid=91491\tshop_id=365223\tfee=0.0200\t" +
                "fee_sum=7.93\tshow_block_id=7265565350985187587\tshow_uid=\t" +
                "fee_show=8-qH2tqoDtIR3TCyLzAdoU56vD5EcVLF5tc4QYltiUVo6J98R8oUCUflBTWlpAo9uj7bAF56h1YYKxAuWXncKA,,";
        List<CartEvent> result = mapper.apply(line.getBytes());
        assertEquals(1, result.size());

        CartEvent cartEvent = result.get(0);

        UserIds ids = cartEvent.getUserIds();
        assertEquals(658839248, ids.getPuid());
        assertTrue(Strings.isNullOrEmpty(ids.getYandexuid()));
        assertTrue(Strings.isNullOrEmpty(ids.getUuid()));

        assertEquals(1529401226000L, cartEvent.getTimestamp());

        assertEquals(RGBType.GREEN, cartEvent.getRgb());

        assertEquals(34501309, cartEvent.getListId());
        assertEquals(26836426, cartEvent.getItemId());

        assertEquals("CREATE", cartEvent.getEventType());
        assertEquals("OFFER", cartEvent.getItemType());
        assertEquals(1, cartEvent.getItemCount());
        assertEquals("UfV1k-33uct98giLver0wA", cartEvent.getWareMd5());
        assertEquals(10404626, cartEvent.getModelId());

        assertEquals(91491, cartEvent.getHid());
        assertEquals(365223, cartEvent.getShopId());

        assertEquals(7.93, cartEvent.getFeeSum(), 0);
    }

    @Test
    public void testParseLineWithowModelId() {
        String line = "tskv\tevent_time=2018-06-19T12:40:26+0300\tevent_type=CREATE\tuser_id=658839248\t" +
                "user_id_type=UID\trgb=GREEN\tlist_id=34501309\titem_id=26836427\titem_type=OFFER\titem_count=1\t" +
                "ware_md5=h_W2CkIoTWKsgmDdtrQo7Q\thid=91660\tshop_id=720\tfee=0.0200\tfee_sum=5.57\t" +
                "show_block_id=7220321763186519379\tshow_uid=\t" +
                "fee_show=8-qH2tqoDtJ1ZZ0ViELD3xAcHjjUWMWyZL3lyPFD6vlCkywFJrQsvWK4ZjcJYK0EIzoWrWsZ6vjwMSZedkGWMw,,";
        List<CartEvent> result = mapper.apply(line.getBytes());
        assertEquals(1, result.size());

        CartEvent cartEvent = result.get(0);
        assertEquals(0, cartEvent.getModelId());
    }
}

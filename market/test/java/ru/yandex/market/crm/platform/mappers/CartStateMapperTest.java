package ru.yandex.market.crm.platform.mappers;

import com.google.common.base.Strings;
import org.junit.Test;

import ru.yandex.market.crm.platform.models.CartEventType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CartStateMapperTest {

    private final CartStateMapper mapper = new CartStateMapper();

    @Test
    public void testMapping() {
        var line = "tskv\tevent_time=2018-06-19T12:40:26+0300\tevent_type=CREATE\tuser_id=658839248\t" +
                   "user_id_type=UID\trgb=GREEN\tlist_id=34501309\titem_id=26836426\titem_type=OFFER\titem_count=2\t" +
                   "ware_md5=UfV1k-33uct98giLver0wA\tmodel_id=10404626\thid=91491\tshop_id=365223\tfee=0.0200\t" +
                   "price=3790.12\tfee_sum=7.93\tshow_block_id=7265565350985187587\tshow_uid=\t" +
                   "fee_show=8-qH2tqoDtIR3TCyLzAdoU56vD5EcVLF5tc4QYltiUVo6J98R8oUCUflBTWlpAo9uj7bAF56h1YYKxAuWXncKA,,";
        var result = mapper.apply(line.getBytes());
        assertEquals(1, result.size());

        var cartState = result.get(0);

        var ids = cartState.getUserIds();
        assertEquals(658839248, ids.getPuid());
        assertTrue(Strings.isNullOrEmpty(ids.getYandexuid()));
        assertTrue(Strings.isNullOrEmpty(ids.getUuid()));

        var expectedTs = 1529401226000L;
        assertEquals(expectedTs, cartState.getTimestamp());
        assertEquals(expectedTs, cartState.getUpdateTime());

        assertEquals(CartEventType.CREATE, cartState.getLastEventType());
        assertEquals(1, cartState.getItemsCount());
        assertEquals(379012, cartState.getPriceTotal());

        var items = cartState.getItemsList();
        assertEquals(1, items.size());

        var item = items.get(0);
        assertEquals("26836426", item.getId());
        assertEquals("OFFER", item.getItemType());
        assertEquals(10404626, item.getModelId());
        assertEquals("UfV1k-33uct98giLver0wA", item.getWareMd5());
        assertEquals("", item.getSku());
        assertEquals(91491, item.getHid());
        assertEquals(2, item.getCount());
        assertEquals(379012, item.getPrice());
        assertEquals(expectedTs, item.getAppendTime());
        assertEquals(expectedTs, item.getUpdateTime());
    }

    /**
     * Если в событии отсутствует поле wareMd5, то пропускаем его при маппинге
     */
    @Test
    public void testParseLineWithoutWareMd5() {
        var line = """
                tskv\tevent_time=2018-06-19T12:40:26+0300\tevent_type=CREATE\tuser_id=658839248\t
                user_id_type=UID\trgb=GREEN\tlist_id=34501309\titem_id=26836427\titem_type=OFFER\titem_count=1\t
                model_id=663834739\thid=91660\tshop_id=720\tfee=0.0200\tfee_sum=5.57\t
                show_block_id=7220321763186519379\tshow_uid=\t
                fee_show=8-qH2tqoDtJ1ZZ0ViELD3xAcHjjUWMWyZL3lyPFD6vlCkywFJrQsvWK4ZjcJYK0EIzoWrWsZ6vjwMSZedkGWMw,,""";
        var result = mapper.apply(line.getBytes());
        assertEquals(0, result.size());
    }
}

package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.models.Coin;
import ru.yandex.market.crm.platform.models.CoinEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author apershukov
 */
public class CoinEventMapperTest {

    @Test
    public void testParse() {
        CoinEventMapper mapper = new CoinEventMapper();

        String line = "tskv\tdate=2018-09-07T16:07:50.241+03:00\t" +
                "user={\\\"uid\\\":543647447,\\\"uuid\\\":null,\\\"yandexUid\\\":null,\\\"muid\\\":null," +
                "\\\"email\\\":null,\\\"phone\\\":null}\t" +
                "coin={\\\"id\\\":19695," +
                "\\\"title\\\":\\\"Скидка на 100 рублей\\\"," +
                "\\\"subtitle\\\":\\\"на все заказы\\\"," +
                "\\\"coinType\\\":\\\"FIXED\\\"," +
                "\\\"nominal\\\":100.00," +
                "\\\"description\\\":\\\"Скидку 100 рублей можно применить на любой заказ.\\\"," +
                "\\\"inactiveDescription\\\":\\\"Описание неактивной монеты\\\"," +
                "\\\"creationDate\\\":\\\"25-08-2018 13:23:11\\\"," +
                "\\\"startDate\\\":\\\"2018-08-25T17:19:07.000+0000\\\"," +
                "\\\"endDate\\\":\\\"25-09-2018 13:23:11\\\"," +
                "\\\"image\\\":\\\"https://avatars.mdst.yandex.net/get-smart_shopping/1823/2080eff0-2a8e-4679-926c-032dfb3b29b8/\\\"," +
                "\\\"backgroundColor\\\":null," +
                "\\\"status\\\":\\\"ACTIVE\\\"," +
                "\\\"promoId\\\":10382," +
                "\\\"requireAuth\\\":true," +
                "\\\"activationToken\\\":\\\"idkfa\\\", " +
                "\\\"reason\\\":\\\"EMAIL_COMPANY\\\"," +
                "\\\"reasonParam\\\":\\\"gift0220\\\"," +
                "\\\"outgoingLink\\\":\\\"https://beru.ru/bonus\\\"," +
                "\\\"mergeTag\\\":\\\"welcome_delivery_coin_2020\\\"}\t" +
                "event_type=COIN_CREATED\n";

        List<CoinEvent> events = mapper.apply(line.getBytes());
        assertEquals(1, events.size());

        CoinEvent event = events.get(0);
        assertEquals(1536325670000L, event.getTimestamp());
        assertEquals(543647447, event.getUserIds().getPuid());
        assertEquals("COIN_CREATED", event.getEventType());
        assertEquals(RGBType.BLUE, event.getRgb());

        Coin coin = event.getCoin();
        assertEquals(19695, coin.getId());
        assertEquals(10382, coin.getPromoId());
        assertEquals("FIXED", coin.getType());
        assertEquals("Скидка на 100 рублей", coin.getTitle());
        assertEquals("на все заказы", coin.getSubtitle());
        assertEquals("Скидку 100 рублей можно применить на любой заказ.", coin.getDescription());
        assertEquals("Описание неактивной монеты", coin.getInactiveDescription());
        assertEquals(100.00, coin.getNominal(), .001);
        assertEquals("25-08-2018 13:23:11", coin.getCreationDate());
        assertEquals("2018-08-25T17:19:07.000+0000", coin.getStartDate());
        assertEquals("25-09-2018 13:23:11", coin.getEndDate());
        assertTrue(coin.getRequireAuth());

        assertEquals(
                "https://avatars.mdst.yandex.net/get-smart_shopping/1823/2080eff0-2a8e-4679-926c-032dfb3b29b8/",
                coin.getImage()
        );

        assertEquals("ACTIVE", coin.getStatus());
        assertEquals("idkfa", coin.getActivationToken());
        assertEquals("EMAIL_COMPANY", coin.getReason());
        assertEquals("gift0220", coin.getReasonParam());

        assertEquals("https://beru.ru/bonus", coin.getOutgoingLink());
        assertEquals("welcome_delivery_coin_2020", coin.getMergeTag());
    }

    /**
     * Проверяем, что {@code null} необязательных параметров не приводит о ошибкам маппинга
     */
    @Test
    public void testParseNullParams() {
        CoinEventMapper mapper = new CoinEventMapper();

        String line = "tskv\tdate=2018-09-07T16:07:50.241+03:00\t" +
                "user={\\\"uid\\\":null,\\\"uuid\\\":null,\\\"yandexUid\\\":null,\\\"muid\\\":null," +
                "\\\"email\\\":null,\\\"phone\\\":null}\t" +
                "coin={\\\"id\\\":19695," +
                "\\\"title\\\":\\\"Скидка на 100 рублей\\\"," +
                "\\\"subtitle\\\":\\\"на все заказы\\\"," +
                "\\\"coinType\\\":\\\"FIXED\\\"," +
                "\\\"nominal\\\":null," +
                "\\\"description\\\":\\\"Скидку 100 рублей можно применить на любой заказ.\\\"," +
                "\\\"inactiveDescription\\\":null," +
                "\\\"creationDate\\\":\\\"25-08-2018 13:23:11\\\"," +
                "\\\"startDate\\\":\\\"2018-08-25T17:19:07.000+0000\\\"," +
                "\\\"endDate\\\":\\\"25-09-2018 13:23:11\\\"," +
                "\\\"image\\\":null," +
                "\\\"backgroundColor\\\":null,\\\"status\\\":\\\"ACTIVE\\\",\\\"promoId\\\":10382," +
                "\\\"requireAuth\\\":true,\\\"activationToken\\\":null, " +
                "\\\"reason\\\":\\\"EMAIL_COMPANY\\\",\\\"reasonParam\\\":null," +
                "\\\"outgoingLink\\\":null,\\\"mergeTag\\\":null}\t" +
                "event_type=COIN_CREATED\n";

        List<CoinEvent> events = mapper.apply(line.getBytes());
        assertEquals(1, events.size());

        CoinEvent event = events.get(0);
        assertEquals(1536325670000L, event.getTimestamp());
        assertEquals("COIN_CREATED", event.getEventType());
        assertEquals(RGBType.BLUE, event.getRgb());

        Coin coin = event.getCoin();
        assertEquals(19695, coin.getId());
        assertEquals(10382, coin.getPromoId());
        assertEquals("FIXED", coin.getType());
        assertEquals("Скидка на 100 рублей", coin.getTitle());
        assertEquals("на все заказы", coin.getSubtitle());
        assertEquals("Скидку 100 рублей можно применить на любой заказ.", coin.getDescription());
        assertEquals("25-08-2018 13:23:11", coin.getCreationDate());
        assertEquals("2018-08-25T17:19:07.000+0000", coin.getStartDate());
        assertEquals("25-09-2018 13:23:11", coin.getEndDate());
        assertTrue(coin.getRequireAuth());

        assertEquals("ACTIVE", coin.getStatus());
        assertEquals("EMAIL_COMPANY", coin.getReason());
    }
}

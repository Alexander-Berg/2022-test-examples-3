package ru.yandex.market.clickphite.dictionary.processors;

import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickphite.dictionary.ClickhouseService;
import ru.yandex.market.clickphite.dictionary.Dictionary;
import ru.yandex.market.clickphite.dictionary.dicts.OrdersAggrDictionary;
import ru.yandex.market.clickphite.dictionary.dicts.RegionsDictionary;
import ru.yandex.market.clickphite.dictionary.dicts.ShopDatasourceDictionary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * @author Denis Khurtin <dkhurtin@yandex-team.ru>
 */
public class JsonDictionaryProcessorTest {

    private final ClickhouseTemplate clickhouseTemplate = Mockito.mock(ClickhouseTemplate.class);
    private final ClickhouseService clickhouseService = new ClickhouseService() {{
        setClickhouseTemplate(clickhouseTemplate);
    }};

    @Test
    public void insertDataWithNullValues() throws Exception {
        // Given
        String line = "{\"id\":10000,\"ru_name\":\"Земля\",\"type\":0,\"parent_id\":0,\"parent_ru_name\":\"\",\"parent_type\":0,\"country_id\":null,\"country_ru_name\":\"\",\"parents\":null,\"path_ru_name\":[\"Земля\"],\"children\":[138,241,245,318,10001,10002,10003,20001],\"distribution_region_group_id\":15}";

        // When
        RegionsDictionary dictionary = new RegionsDictionary();
        insertLineToDictionary(line, dictionary);

        // Then
        Mockito.verify(clickhouseTemplate).update("INSERT INTO tmp_tbl (id, ru_name, type, parent_id, parent_ru_name, parent_type, country_id, country_ru_name, parents, path_ru_name, children, en_name, parent_en_name, country_en_name, path_en_name, distribution_region_group_id) VALUES\n" +
            "(10000,'Земля',0,0,'',0,0,'',[],['Земля'],[138,241,245,318,10001,10002,10003,20001],'','','',[],15)\n", "test-host");
    }

    @Test
    public void insertDataToShop() throws Exception {
        // Given
        String line = "{\"id\":1417,\"name\":\"moyaodejda.ru\",\"urlforlog\":\"www.moyaodejda.ru\",\"createdAt\":1109234157000,\"comments\":null,\"managerId\":-2}";

        // When
        ShopDatasourceDictionary dictionary = new ShopDatasourceDictionary();
        insertLineToDictionary(line, dictionary);

        // Then
        Mockito.verify(clickhouseTemplate).update("INSERT INTO tmp_tbl (id, name, urlforlog, createdAt, comments, managerId) VALUES\n" +
            "(1417,'moyaodejda.ru','www.moyaodejda.ru','2005-02-24 11:35:57','',-2)\n", "test-host");
    }

    @Test
    public void insertDataWithSingleQuotaWithinData() throws Exception {
        // Given
        String line = "{\"id\":109086,\"ru_name\":\"Альп-д'Юэз\",\"type\":7,\"parent_id\":152700,\"parent_ru_name\":\"Гренобль\",\"parent_type\":10,\"country_id\":124,\"country_ru_name\":\"Франция\",\"parents\":[10000,10001,111,124,104376,152602,152700],\"path_ru_name\":[\"Альп-д'Юэз\",\"Гренобль\",\"Изер\",\"Овернь — Рона — Альпы\",\"Франция\",\"Европа\",\"Евразия\",\"Земля\"],\"children\":null," +
                "\"en_name\":\"Alpe d'Huez\", \"parent_en_name\":\"Grenoble\",\"country_en_name\":\"France\",\"path_en_name\":[\"Alpe d'Huez\",\"Grenoble\",\"Isere\",\"Auvergne-Rhone-Alpes\",\"France\",\"Europe\",\"Eurasia\",\"Earth\"]}";

        // When
        RegionsDictionary dictionary = new RegionsDictionary();
        insertLineToDictionary(line, dictionary);

        // Then
        Mockito.verify(clickhouseTemplate).update("INSERT INTO tmp_tbl (id, ru_name, type, parent_id, parent_ru_name, parent_type, country_id, country_ru_name, parents, path_ru_name, children, en_name, parent_en_name, country_en_name, path_en_name, distribution_region_group_id) VALUES\n" +
            "(109086,'Альп-д\\'Юэз',7,152700,'Гренобль',10,124,'Франция',[10000,10001,111,124,104376,152602,152700],['Альп-д\\'Юэз','Гренобль','Изер','Овернь — Рона — Альпы','Франция','Европа','Евразия','Земля'],[],'Alpe d\\'Huez','Grenoble','France',['Alpe d\\'Huez','Grenoble','Isere','Auvergne-Rhone-Alpes','France','Europe','Eurasia','Earth'],-1)\n", "test-host");
    }

    @Test
    public void shouldInsertDateFieldsCorrectly() throws IOException {
        // Given
        String line =
                "{" +
                   "\"creation_time\":1495643476000," + // 2017-05-24 19:31:16
                   "\"creation_day\":\"2017-05-24\"" +
                "}";
        OrdersAggrDictionary dictionary = new OrdersAggrDictionary();
        insertLineToDictionary(line, dictionary);

        Mockito.verify(clickhouseTemplate).update("INSERT INTO tmp_tbl (event_id, offer_ware_md5, offer_feed_id, " +
                "offer_feed_category_id, offer_availability, offer_price, offer_currency, offer_fee, order_fee, " +
                "model_id, model_hid, show_block_id, show_uid, item_count, item_price, item_revenue, buyer_price, " +
                "buyer_currency, order_status, order_substatus, order_is_fake, payment_type, payment_method, " +
                "order_accept_method, buyer_uid, buyer_is_auth, buyer_user_group, buyer_is_fake, buyer_region_id, " +
                "buyer_uuid, buyer_device_type, buyer_platform, click_rough_pp, delivery_type, delivery_price, " +
                "delivery_buyer_price, delivery_from_date, delivery_to_date, delivery_region_id, shop_id, " +
                "shop_is_fake, event_type, event_time, event_hour, event_day, creation_time, creation_hour, " +
                "delivery_deliveryserviceid, delivery_shopaddress, delivery_address, delivery_servicename, " +
                "delivery_hiddenpaymentoptions, delivery_paymentoptions, delivery_validfeatures, billing_id, " +
                "billing_time, billing_hour, billing_day, order_is_billed, order_statuses, offer_id, order_id, " +
                "creation_day, offer_name, offer_url) VALUES\n" + "(-1,'','','','',0.0,'',0.0,0.0,-1,-1,'','',0,0.0,0.0,0.0,'','','',0,'',''," +
                "'','',0,'',0,-1,'','','','','',0.0,0.0,'0000-00-00','0000-00-00',-1,-1,0,'','1970-01-01 00:00:00',0," +
                "'0000-00-00','2017-05-24 19:31:16',0,'','','','','','','',-1,'1970-01-01 00:00:00',0,'0000-00-00',0," +
                "[],'',-1,'2017-05-24','','')\n", "test-host");

    }

    private void insertLineToDictionary(String line, Dictionary dictionary) throws IOException {
        insertLineToDictionary(line, dictionary, clickhouseService, new JsonDictionaryProcessor());
    }

    static void insertLineToDictionary(String line, Dictionary dictionary, ClickhouseService clickhouseService,
                                       JsonDictionaryProcessor jsonDictionaryProcessor) throws IOException {
        ClickhouseService.BulkUpdater bulkUpdater =
            clickhouseService.createBulkUpdater(dictionary, "tmp_tbl", 1, "test-host");

        jsonDictionaryProcessor.insertData(dictionary, new BufferedReader(new StringReader(line)),
            bulkUpdater::submit);
        bulkUpdater.done();
    }
}

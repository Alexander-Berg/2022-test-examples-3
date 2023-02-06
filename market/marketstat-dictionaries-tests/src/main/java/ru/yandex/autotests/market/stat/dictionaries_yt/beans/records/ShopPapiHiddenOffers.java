package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "shop_papi_hidden_offer")
public class ShopPapiHiddenOffers implements DictionaryRecord {

    @DictionaryIdField
    private String feed_id;
    @DictionaryIdField
    private String offer_id;
    private String timeout;
    private String comment_msg;
    private String cmid;
    private String cmid_update_time;
    private String author_client_id;
    private String offer_name;
    private String category_id;
    private String hiding_time;
    private String datasource_id;
    private String url;

}

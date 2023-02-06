package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by kateleb on 28.06.17
 */
@Data
@DictTable(name = "accessory_offers")
public class AccessoryOffers implements DictionaryRecord {
    @DictionaryIdField
    private String offerId;
    private String accessoryOfferIds;

}

package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.clickHouse.annotations.ClickHouseField;

import static ru.yandex.autotests.market.stat.attribute.Fields.CAMPAIGN_ID;
import static ru.yandex.autotests.market.stat.attribute.Fields.CLIENT_ID;
import static ru.yandex.autotests.market.stat.attribute.Fields.DATASOURCE_ID;
import static ru.yandex.autotests.market.stat.attribute.Fields.ID;
import static ru.yandex.autotests.market.stat.attribute.Fields.MANAGER;
import static ru.yandex.autotests.market.stat.attribute.Fields.NAME;

/**
 * Created by kateleb on 01.07.16
 */
@Data
@DictTable(name = "shop_agency_campaigns")
public class ShopsCampaign implements DictionaryRecord {
    @DictionaryIdField
    @ClickHouseField(ID)
    private String id;

    @ClickHouseField(NAME)
    @RequiredField
    private String name;

    @ClickHouseField(MANAGER)
    private String manager;

    @ClickHouseField(CLIENT_ID)
    @RequiredField
    private String clientId;

    @DictionaryIdField
    @ClickHouseField(CAMPAIGN_ID)
    private String campaignId;

    @ClickHouseField(DATASOURCE_ID)
    private String datasourceId;

}

package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import org.beanio.annotation.Field;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

import static ru.yandex.autotests.market.stat.attribute.Fields.CURRENCY_ID;

/**
 * Created by timofeevb on 07.04.17.
 */
@Data
@DictTable(name = "distribution")
public class Distribution implements DictionaryRecord {
    private String clicksMarket;

    private String ordersMarketCpa;

    private String partnerMarketCpc;

    private String partnerMarketCpa;

    private String turnoverMarketCpc;

    private String turnoverMarketCpa;

    @DictionaryIdField
    private String softId;

    @RequiredField
    private String softName;

    @Field(name = CURRENCY_ID, at = 9)
    @RequiredField
    private String currencyId;

    @DictionaryIdField(isForQuery = false)
    private LocalDateTime day;

    private LocalDateTime month; // in hive is date;
}

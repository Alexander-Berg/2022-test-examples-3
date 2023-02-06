package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by kateleb
 */

@Data
@DictTable(name = "currency_rates")
public class CurrencyRates implements DictionaryRecord {

    @DictionaryIdField
    private String currencyFrom;

    @DictionaryIdField
    private String regionFrom;

    @DictionaryIdField
    private String currencyTo;

    private String regionTo;

    private String rate;

    private LocalDateTime day;


}

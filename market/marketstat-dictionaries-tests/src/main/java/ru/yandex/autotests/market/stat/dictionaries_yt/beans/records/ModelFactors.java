package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * @author timofeevb
 */
@Data
@DictTable(name = "model_factors")
public class ModelFactors implements DictionaryRecord {
    @DictionaryIdField
    private String model_id;
    private String recommend;
    private String recommend_answered;
    private String total;

    @DictionaryIdField
    private String factor_id;
    private String factor_name;
    private String value;
    private String count;

}

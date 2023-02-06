package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "internal_complaint_offer")
public class AboInternalComplaintOffer implements DictionaryRecord {
    @DictionaryIdField
    private String complain_id; /* Long */
    private String title; /* String */
    @DictionaryIdField
    private String hyper_model_id; /* Long */
    private String hyper_category_id; /* Long */
    private String hyper_category_name; /* String */
    private String price; /* Double */
    private String description; /* String */
}

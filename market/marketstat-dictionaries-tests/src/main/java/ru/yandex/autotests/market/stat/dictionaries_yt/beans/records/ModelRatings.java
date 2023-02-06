package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by kateleb on 08.06.17.
 */
@Data
@DictTable(name = "model_ratings")
public class ModelRatings implements DictionaryRecord {
    @DictionaryIdField
    private String modelId;
    private String ratingValue;
    private String ratingTotal;
    private String opinionsTotal;
    private String reviewsTotal;

}

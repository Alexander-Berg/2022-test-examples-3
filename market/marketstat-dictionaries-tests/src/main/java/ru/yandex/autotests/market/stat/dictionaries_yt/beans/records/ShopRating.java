package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.beanio.annotation.Field;
import org.beanio.annotation.Record;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.clickHouse.annotations.ClickHouseField;

import static ru.yandex.autotests.market.stat.attribute.Fields.DAY;
import static ru.yandex.autotests.market.stat.attribute.Fields.IS_ENABLED;
import static ru.yandex.autotests.market.stat.attribute.Fields.RATING;
import static ru.yandex.autotests.market.stat.attribute.Fields.RATING_DATE;
import static ru.yandex.autotests.market.stat.attribute.Fields.RATING_DATE_CH;
import static ru.yandex.autotests.market.stat.attribute.Fields.RATING_TYPE;
import static ru.yandex.autotests.market.stat.attribute.Fields.SHOP_ID;

/**
 * Created by kateleb on 28.06.16.
 */
@Record
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@DictTable(name = "shop_ratings")
public class ShopRating implements DictionaryRecord {
    @DictionaryIdField
    @ClickHouseField(SHOP_ID)
    @Field(at = 0)
    private String shopId;

    @ClickHouseField(RATING)
    @RequiredField
    @Field(at = 1)
    private String rating;

    @ClickHouseField(RATING_DATE_CH)
    @Field(at = 2)
    private LocalDateTime date;

    @ClickHouseField(IS_ENABLED)
    @Field(at = 3)
    private boolean isEnabled;

    @ClickHouseField(RATING_TYPE)
    @Field(at = 4)
    private String ratingType;

    @ClickHouseField(RATING_DATE)
    @Field(name = DAY, at = 5)
    private LocalDateTime day;

}

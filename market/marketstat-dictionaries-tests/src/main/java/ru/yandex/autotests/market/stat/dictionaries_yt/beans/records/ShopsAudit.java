package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.beanio.annotation.Field;
import org.beanio.annotation.Record;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;
import ru.yandex.autotests.market.stat.clickHouse.annotations.ClickHouseField;
import ru.yandex.autotests.market.stat.handlers.Handlers;
import ru.yandex.autotests.market.stat.util.data.IgnoreField;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.yandex.autotests.market.stat.attribute.Fields.OTHERS;
import static ru.yandex.autotests.market.stat.attribute.Fields.SHOP_ID;
import static ru.yandex.autotests.market.stat.attribute.Fields.URL;

/**
 * Created by kateleb on 29.06.16.
 */
@Record
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@DictTable(name = "shop_audit")
public class ShopsAudit implements DictionaryRecord {
    @DictionaryIdField
    @ClickHouseField(SHOP_ID)
    @Field(name = SHOP_ID, at = 0, handlerName = Handlers.QUOTED_VALUE_HANDLER)
    private String shopId;

    @ClickHouseField(URL)
    @Field(name = URL, at = 1, handlerName = Handlers.QUOTED_VALUE_HANDLER)
    @RequiredField
    private String url;

    @ClickHouseField(OTHERS)
    @Field(name = OTHERS, at = 2, handlerName = Handlers.QUOTED_MAP_HANDLER)
    @RequiredField
    private String auditsAsString;

    @IgnoreField
    private Map<String, String> auditsAsMap;

    public Map<String, String> getAuditsAsMap() {
        if (auditsAsMap == null || auditsAsMap.isEmpty()) {
            parseAuditsToMap();
        }
        return auditsAsMap;
    }

    private void parseAuditsToMap() {
        auditsAsMap = new HashMap<>();
        if (auditsAsString != null) {
            List<String> errors = Arrays.asList(auditsAsString.split(","));
            int index = 1;
            for (String error : errors) {
                auditsAsMap.put(String.valueOf(index), error);
                index++;
            }
        }
    }

}

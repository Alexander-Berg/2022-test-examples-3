package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * @author kateleb
 */
@Data
@DictTable(name = "region_groups_services")
public class RegionGroupServices implements DictionaryRecord {

    @DictionaryIdField
    private String deliveryRegionGroupId;
    @DictionaryIdField
    private String deliveryServiceId;
    private LocalDateTime day;

}

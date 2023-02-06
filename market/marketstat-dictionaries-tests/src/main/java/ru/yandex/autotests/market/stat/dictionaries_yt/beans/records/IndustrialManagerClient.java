package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import org.beanio.annotation.Field;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.RequiredField;

import static ru.yandex.autotests.market.stat.attribute.Fields.INDUSTRIAL_MANAGER_CLIENT;

/**
 * Created by kateleb on 21.12.16
 */
@Data
@DictTable(name = "industrial_manager_client")
public class IndustrialManagerClient implements DictionaryRecord {

    @Field(name = INDUSTRIAL_MANAGER_CLIENT, at = 0)
    @RequiredField
    private String industrialManagerClient; // in hive is bigint

    @DictionaryIdField
    private String clientId; // in hive is bigint

    private LocalDateTime startDate; // in hive is date

    private LocalDateTime endDate; // in hive is date
}

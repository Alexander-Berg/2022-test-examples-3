package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import org.beanio.annotation.Field;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.handlers.Handlers;

import static ru.yandex.autotests.market.stat.attribute.Fields.RFACTORS;

/**
 * Created by kateleb on 08.06.17.
 */
@Data
@DictTable(name = "abt_experiments")
public class AbtExperiments implements DictionaryRecord {
    @DictionaryIdField
    private String testId;
    @DictionaryIdField
    private String title;
    @Field(name = RFACTORS, handlerName = Handlers.QUOTED_MAP_HANDLER)
    private String rfactors;

    private LocalDateTime dateModified;
    private String ticket;

}

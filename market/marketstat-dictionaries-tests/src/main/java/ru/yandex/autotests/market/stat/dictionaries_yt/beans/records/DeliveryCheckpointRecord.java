package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * @author Alexey Mokhnin <nettoyeur@yandex-team.ru>
 */
@Data
@DictTable(name = "delivery_checkpoint")
public class DeliveryCheckpointRecord implements DictionaryRecord {

    @DictionaryIdField
    private String id;
    private String trackId;
    private String country;
    private String city;
    private String location;
    private String message;
    private String status;
    private String zipCode;

    private LocalDateTime checkpointTs;
    private String rawStatus;


}

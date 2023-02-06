package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * @author Alexey Mokhnin <nettoyeur@yandex-team.ru>
 */
@Data
@DictTable(name = "delivery_track")
public class DeliveryTrackRecord implements DictionaryRecord {

    @DictionaryIdField
    private String id;
    private String trackCode;
    private String backUrl;
    private String delivery_serviceId;
    private String consumerId;
    private String sourceId;

    private LocalDateTime startTs;

    private LocalDateTime lastUpdatedTs;

    private LocalDateTime lastNotifySuccessTs;

    private LocalDateTime nextRequestTs;
    private String status;
    private String orderId;

    private LocalDateTime estimatedArrivalDateFrom;

    private LocalDateTime estimatedArrivalDateTo;
    private String deliveryType;


    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }
}

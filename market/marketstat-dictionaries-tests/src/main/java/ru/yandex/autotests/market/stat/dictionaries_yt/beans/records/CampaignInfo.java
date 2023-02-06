package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "campaign_info")
public class CampaignInfo implements DictionaryRecord  {

    @DictionaryIdField
    private String campaign_id;
    private String billing_type;
    private String client_id;
    private String datasource_id;
    private LocalDateTime end_date;
    private LocalDateTime start_date;

}

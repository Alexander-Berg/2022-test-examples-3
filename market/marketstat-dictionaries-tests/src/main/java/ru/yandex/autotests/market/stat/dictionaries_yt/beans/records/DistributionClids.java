package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * @author zoom
 */
@Data
@DictTable(name = "distribution_clids")
public class DistributionClids implements DictionaryRecord {

    @DictionaryIdField
    private String id;

    private String typeId;

    private String setId;

    private String pageId;

    private String packId;

    private String softId;

    private String isPayableId;

    private String contractTagId;

    private String tagCaption;

    private String setDomain;

    private String setCaption;

    private String packDomain;

    private String packCaption;

    private String userLogin;

    private String viewStatistics;

}

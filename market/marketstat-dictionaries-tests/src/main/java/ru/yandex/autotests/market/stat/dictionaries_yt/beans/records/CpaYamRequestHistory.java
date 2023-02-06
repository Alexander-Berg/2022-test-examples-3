package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * Created by entarrion <entarrion@yandex-team.ru> on 28.06.17.
 */
@Data
@DictTable(name = "v_partner_app_business")
public class CpaYamRequestHistory implements DictionaryRecord {
    @DictionaryIdField
    private String shopId; /*Actualy bigint */

    @DictionaryIdField
    private String requestId; /*Actualy bigint */

    @DictionaryIdField
    private String status; /*Actualy int */

    private String comment; /*Actualy string */

    private String sellerClientId; /*Actualy bigint */

    private String contractId; /*Actualy bigint */

    private String personId; /*Actualy bigint */

    private LocalDateTime startDate; /*Actualy date */


}

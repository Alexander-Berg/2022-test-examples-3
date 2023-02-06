package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

@Data
@DictTable(name = "core_problem_type")
public class AboCoreProblemType implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Actualy Integer */
    private String problem_class_id; /* Actualy Integer */
    private String name; /* Actualy String */
    private String description; /* Actualy String */
    private String critical; /* Actualy Boolean */
    private String hidden; /* Actualy Boolean */
    private String involves_offer_removal; /* Actualy Boolean */
    private String sending_message; /* Actualy Boolean */
    private String waiting_base_update; /* Actualy Boolean */
    private String for_shop_type; /* Actualy BigDecimal */
    private String show_in_pi; /* Actualy Boolean */
    private String can_be_mass; /* Actualy Boolean */
    private String relates_to_placement; /* Actualy String */
}

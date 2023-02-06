package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;

/**
 * @author aostrikov
 */
@Data
@DictTable(name = "shop_datasources_in_testing")
public class ShopDatasourcesInTesting implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Actually long */
    private String datasource_id; /* Actually long */
    private String ready; /* Actually int */
    private String approved; /* Actually int */
    private String in_progress; /* Actually int */
    private String cancelled; /* Actually int */
    private String push_ready_count; /* Actually long */
    private String fatal_cancelled; /* Actually int */
    private String iter_count; /* Actually long */
    private LocalDateTime updated_at; /* Actually timestamp */
    private String recommendations; /* Actually string */
    private LocalDateTime start_date; /* Actually timestamp */
    private String testing_type; /* Actually long */
    private String claim_link; /* Actually long */
    private String status; /* Actually long */
    private String attempt_num; /* Actually long */
    private String quality_check_required; /* Actually int */
    private String clone_check_required; /* Actually int */
    private String shop_program; /* Actually long */

}

package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import lombok.Data;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictTable;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;


@Data
@DictTable(name = "core_problem")
public class AboCoreProblem implements DictionaryRecord {
    @DictionaryIdField
    private String id; /* Actualy Long*/
    private String hyp_id; /* Actualy Long*/
    private String problem_type_id; /* Actualy Integer*/
    private String status_id; /* Actualy Integer*/
    private LocalDateTime creation_time;
    private LocalDateTime modification_time;
    private String user_comment;
    private String public_comment;
    private String creation_tag_id; /* Actualy Long */
    private String modification_tag_id; /* Actualy Long */
    private String force_approve; /* Actualy Boolean */
}

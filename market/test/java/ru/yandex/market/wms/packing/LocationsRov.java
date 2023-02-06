package ru.yandex.market.wms.packing;

import java.util.Set;

import ru.yandex.market.wms.packing.pojo.PackingTable;
import ru.yandex.market.wms.packing.pojo.SortingCell;

public class LocationsRov {

    public static final String NONSORT_TABLE1_CONS_OVERSIZE = "NSCONS1-01";
    public static final String NONSORT_TABLE1_CONS_SINGLES = "NSCONS1-02";
    public static final String NONSORT_TABLE2_CONS_OVERSIZE = "NSCONS2-01";
    public static final String NONSORT_TABLE2_CONS_W_OVERSIZE = "NSCONS2-02";

    public static final PackingTable TABLE_1 = PackingTable.builder()
            .loc("PACKTBL1")
            .sourceLocs(Set.of("SS1"))
            .build();
    public static final PackingTable TABLE_2 = PackingTable.builder()
            .loc("PACKTBL2")
            .sourceLocs(Set.of("SS2"))
            .build();
    public static final PackingTable TABLE_3 = PackingTable.builder()
            .loc("PACKTBL3")
            .sourceLocs(Set.of("SS3"))
            .build();
    public static final PackingTable TABLE_4 = PackingTable.builder()
            .loc("PACKTBL4")
            .sourceLocs(Set.of("SS4"))
            .build();

    public static final PackingTable TABLE_PROMO_1 = PackingTable.builder()
            .loc("PRMPACK1")
            .sourceLocs(Set.of("PRMPICK1", "PRMPICK2"))
            .isPromo(true)
            .build();

    public static final PackingTable NONSORT_TABLE_1 = PackingTable.builder()
            .loc("NSPACKTBL1")
            .sourceLocs(Set.of(NONSORT_TABLE1_CONS_OVERSIZE, NONSORT_TABLE1_CONS_SINGLES))
            .build();

    public static final PackingTable NONSORT_TABLE_2 = PackingTable.builder()
            .loc("NSPACKTBL2")
            .sourceLocs(Set.of(NONSORT_TABLE2_CONS_OVERSIZE, NONSORT_TABLE2_CONS_W_OVERSIZE))
            .build();

    public static final SortingCell SS1_CELL1 =
            SortingCell.builder().loc("SS1").cell("CELL1").id("101").build();
    public static final SortingCell SS1_CELL2 =
            SortingCell.builder().loc("SS1").cell("CELL2").id("102").build();
    public static final SortingCell SS1_CELL3 =
            SortingCell.builder().loc("SS1").cell("CELL3").id("103").build();
    public static final SortingCell SS1_CELL4 =
            SortingCell.builder().loc("SS1").cell("CELL4").id("104").build();
    public static final SortingCell SS1_CELL5 =
            SortingCell.builder().loc("SS1").cell("CELL5").id("105").build();
    public static final SortingCell SS1_CELL6 =
            SortingCell.builder().loc("SS1").cell("CELL6").id("106").build();

    public static final SortingCell SS2_CELL1 =
            SortingCell.builder().loc("SS2").cell("CELL1").id("201").build();
    public static final SortingCell SS2_CELL2 =
            SortingCell.builder().loc("SS2").cell("CELL2").id("202").build();
    public static final SortingCell SS2_CELL3 =
            SortingCell.builder().loc("SS2").cell("CELL3").id("203").build();
    public static final SortingCell SS2_CELL4 =
            SortingCell.builder().loc("SS2").cell("CELL4").id("204").build();

    public static final SortingCell SS3_CELL1 =
            SortingCell.builder().loc("SS3").cell("CELL1").id("301").build();
    public static final SortingCell SS3_CELL2 =
            SortingCell.builder().loc("SS3").cell("CELL2").id("302").build();
    public static final SortingCell SS3_CELL3 =
            SortingCell.builder().loc("SS3").cell("CELL3").id("303").build();
    public static final SortingCell SS3_CELL4 =
            SortingCell.builder().loc("SS3").cell("CELL4").id("304").build();

    public static final SortingCell SS4_CELL1 =
            SortingCell.builder().loc("SS4").cell("CELL1").id("401").build();
    public static final SortingCell SS4_CELL2 =
            SortingCell.builder().loc("SS4").cell("CELL2").id("402").build();
    public static final SortingCell SS4_CELL3 =
            SortingCell.builder().loc("SS4").cell("CELL3").id("403").build();
    public static final SortingCell SS4_CELL4 =
            SortingCell.builder().loc("SS4").cell("CELL4").id("404").build();

    private LocationsRov() {
    }

}

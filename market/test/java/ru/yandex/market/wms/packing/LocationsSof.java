package ru.yandex.market.wms.packing;

import java.util.Set;

import ru.yandex.market.wms.packing.pojo.PackingTable;
import ru.yandex.market.wms.packing.pojo.SortingCell;

public class LocationsSof {

    public static final PackingTable TABLE_1_A = PackingTable.builder()
            .loc("PACKTBL1A")
            .sourceLocs(Set.of("SS1"))
            .rank(0)
            .build();
    public static final PackingTable TABLE_1_B = PackingTable.builder()
            .loc("PACKTBL1B")
            .sourceLocs(Set.of("SS1"))
            .rank(1)
            .build();
    public static final PackingTable TABLE_2_A = PackingTable.builder()
            .loc("PACKTBL2A")
            .sourceLocs(Set.of("SS2"))
            .rank(0)
            .build();
    public static final PackingTable TABLE_2_B = PackingTable.builder()
            .loc("PACKTBL2B")
            .sourceLocs(Set.of("SS2"))
            .rank(1)
            .build();
    public static final PackingTable NONSORT_TABLE_1 = PackingTable.builder()
            .loc("NSPACKTBL1")
            .sourceLocs(Set.of("NSCONS1-01", "NSCONS1-02"))
            .build();

    public static final SortingCell SS1_CELL1 =
            SortingCell.builder().loc("SS1").cell("CELL1").id("101").build();
    public static final SortingCell SS1_CELL2 =
            SortingCell.builder().loc("SS1").cell("CELL2").id("102").build();
    public static final SortingCell SS1_CELL3 =
            SortingCell.builder().loc("SS1").cell("CELL3").id("103").build();
    public static final SortingCell SS1_CELL4 =
            SortingCell.builder().loc("SS1").cell("CELL4").id("104").build();
    public static final SortingCell SS2_CELL1 =
            SortingCell.builder().loc("SS2").cell("CELL1").id("201").build();
    public static final SortingCell SS2_CELL2 =
            SortingCell.builder().loc("SS2").cell("CELL2").id("202").build();
    public static final SortingCell SS2_CELL3 =
            SortingCell.builder().loc("SS2").cell("CELL3").id("203").build();
    public static final SortingCell SS2_CELL4 =
            SortingCell.builder().loc("SS2").cell("CELL4").id("204").build();

    private LocationsSof() {
    }

}

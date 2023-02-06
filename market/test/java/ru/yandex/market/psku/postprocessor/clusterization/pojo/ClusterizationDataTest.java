package ru.yandex.market.psku.postprocessor.clusterization.pojo;

import com.google.common.collect.Sets;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Maps;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClusterizationDataTest {
    private static final long CATEGORY_ID = 123456;
    private static final long CATEGORY_ID_2 = 335577;

    @Test
    public void combineDifferentClusterTypeResults() {
        long idBc0Vc0 = 1;
        long idBc1Vc0 = 2;
        long idBc0Vc1 = 3;
        long idBc1Vc1 = 4;
        long idBc3Vc3 = 5;
        long idBc3Vc1 = 6;
        long idBc3Vc0 = 7;
        long idBc1Vc3 = 8;
        long idBc0Vc3 = 9;

        Psku pskuBc3Vc3 = createPsku(idBc3Vc3, CATEGORY_ID);
        Psku pskuBc3Vc1 = createPsku(idBc3Vc1, CATEGORY_ID);
        Psku pskuBc3Vc0 = createPsku(idBc3Vc0, CATEGORY_ID);
        Psku pskuBc1Vc3 = createPsku(idBc1Vc3, CATEGORY_ID);
        Psku pskuBc0Vc3 = createPsku(idBc0Vc3, CATEGORY_ID);

        Map<Long, Set<Long>> notEligibleForBarcode = Collections.singletonMap(
            CATEGORY_ID,
            Sets.newHashSet(idBc0Vc0, idBc0Vc1, idBc0Vc3)
        ); //BC0
        Map<Long, Set<Long>> singleForBarcode = Collections.singletonMap(
            CATEGORY_ID,
            Sets.newHashSet(idBc1Vc0, idBc1Vc1, idBc1Vc3)
        ); //BC1
        List<PskuCluster> clustersForBarcode = Collections.singletonList(
            new PskuCluster(
                pskuBc3Vc3,
                Arrays.asList(pskuBc3Vc3, pskuBc3Vc1, pskuBc3Vc0),
                EnumSet.of(ClusterType.BARCODE),
                "BARCODE:3",
                CATEGORY_ID
            )
        ); //BC3

        ClusterizationData barcodeResult = new ClusterizationData(
            EnumSet.of(ClusterType.BARCODE),
            notEligibleForBarcode,
            singleForBarcode,
            clustersForBarcode
        );

        Map<Long, Set<Long>> notEligibleForVendorCode = Collections.singletonMap(
            CATEGORY_ID,
            Sets.newHashSet(idBc0Vc0, idBc1Vc0, idBc3Vc0)
        ); //VC0
        Map<Long, Set<Long>> singleForVendorCode = Collections.singletonMap(
            CATEGORY_ID,
            Sets.newHashSet(idBc0Vc1, idBc1Vc1, idBc3Vc1)
        ); //VC1
        List<PskuCluster> clustersForVendorCode = Collections.singletonList(
            new PskuCluster(
                pskuBc3Vc3,
                Arrays.asList(pskuBc3Vc3, pskuBc1Vc3, pskuBc0Vc3),
                EnumSet.of(ClusterType.VENDOR_CODE),
                "VENDOR_CODE:3",
                CATEGORY_ID
            )
        ); //VC3

        ClusterizationData vendorCodeResult = new ClusterizationData(
            EnumSet.of(ClusterType.VENDOR_CODE),
            notEligibleForVendorCode,
            singleForVendorCode,
            clustersForVendorCode
        );

        ClusterizationData totalResult = ClusterizationData.combineDifferentClusterTypeResults(
            Arrays.asList(barcodeResult, vendorCodeResult)
        );

        Set<ClusterType> clusterTypeExpected = EnumSet.of(ClusterType.VENDOR_CODE, ClusterType.BARCODE);
        Map<Long, Set<Long>> notEligibleExpected = Collections.singletonMap(
            CATEGORY_ID,
            Sets.newHashSet(idBc0Vc0)
        ); //0
        Map<Long, Set<Long>> singleForExpected = Collections.singletonMap(
            CATEGORY_ID,
            Sets.newHashSet(idBc1Vc0, idBc1Vc1, idBc0Vc1)
        ); //1
        PskuCluster expectedCluster1 = new PskuCluster(
            pskuBc3Vc3,
            Arrays.asList(pskuBc3Vc3, pskuBc3Vc1, pskuBc3Vc0),
            EnumSet.of(ClusterType.BARCODE),
            "BARCODE:3",
            CATEGORY_ID
        );
        PskuCluster expectedCluster2 = new PskuCluster(
            pskuBc3Vc3,
            Arrays.asList(pskuBc3Vc3, pskuBc1Vc3, pskuBc0Vc3),
            EnumSet.of(ClusterType.VENDOR_CODE),
            "VENDOR_CODE:3",
            CATEGORY_ID
        );

        Assertions.assertThat(totalResult.getClusterTypes()).isEqualTo(clusterTypeExpected);
        Assertions.assertThat(totalResult.getNotEligiblePskuByCategory()).isEqualTo(notEligibleExpected);
        Assertions.assertThat(totalResult.getSinglePskuByCategory()).isEqualTo(singleForExpected);
        Assertions.assertThat(totalResult.getClusters()).containsExactlyInAnyOrder(expectedCluster1, expectedCluster2);
    }

    @Test
    public void addCategoryResults() {
        long idNeCat1 = 1;
        long idSingleCat1 = 2;
        long idGroup1Cat1 = 3;
        long idGroup2Cat1 = 4;
        long idNeCat2 = 5;
        long idSingleCat2 = 6;
        long idGroup1Cat2 = 7;
        long idGroup2Cat2 = 8;

        Psku pskuGroup1Cat1 = createPsku(idGroup1Cat1, CATEGORY_ID);
        Psku pskuGroup2Cat1 = createPsku(idGroup2Cat1, CATEGORY_ID);
        Psku pskuGroup1Cat2 = createPsku(idGroup1Cat2, CATEGORY_ID_2);
        Psku pskuGroup2Cat2 = createPsku(idGroup2Cat2, CATEGORY_ID_2);

        Map<Long, Set<Long>> notEligibleCat1 = Collections.singletonMap(
            CATEGORY_ID,
            Collections.singleton(idNeCat1)
        ); //BC0
        Map<Long, Set<Long>> singleCat1 = Collections.singletonMap(
            CATEGORY_ID,
            Collections.singleton(idSingleCat1)
        ); //BC1
        List<PskuCluster> clustersCat1 = Collections.singletonList(
            new PskuCluster(
                pskuGroup1Cat1,
                Arrays.asList(pskuGroup1Cat1, pskuGroup2Cat1),
                EnumSet.of(ClusterType.BARCODE),
                "BARCODE:1",
                CATEGORY_ID
            )
        ); //BC3

        ClusterizationData resultCat1 = new ClusterizationData(
            EnumSet.of(ClusterType.BARCODE),
            notEligibleCat1,
            singleCat1,
            clustersCat1
        );

        Map<Long, Set<Long>> notEligibleCat2 = Collections.singletonMap(
            CATEGORY_ID_2,
            Collections.singleton(idNeCat2)
        ); //VC0
        Map<Long, Set<Long>> singleCat2 = Collections.singletonMap(
            CATEGORY_ID_2,
            Collections.singleton(idSingleCat2)
        ); //VC1
        List<PskuCluster> clustersCat2 = Collections.singletonList(
            new PskuCluster(
                pskuGroup1Cat2,
                Arrays.asList(pskuGroup1Cat2, pskuGroup2Cat2),
                EnumSet.of(ClusterType.BARCODE),
                "BARCODE:2",
                CATEGORY_ID_2
            )
        ); //VC3

        ClusterizationData resultCat2 = new ClusterizationData(
            EnumSet.of(ClusterType.BARCODE),
            notEligibleCat2,
            singleCat2,
            clustersCat2
        );

        ClusterizationData totalResult = ClusterizationData.addCategoryResults(
            Arrays.asList(resultCat1, resultCat2)
        );

        Set<ClusterType> clusterTypeExpected = EnumSet.of(ClusterType.BARCODE);
        Map<Long, Set<Long>> notEligibleExpected = Maps.newHashMap(
            CATEGORY_ID,
            Collections.singleton(idNeCat1)
        );
        notEligibleExpected.put(
            CATEGORY_ID_2,
            Collections.singleton(idNeCat2)
        ); //0
        Map<Long, Set<Long>> singleForExpected = Maps.newHashMap(
            CATEGORY_ID,
            Collections.singleton(idSingleCat1)
        );
        singleForExpected.put(
            CATEGORY_ID_2,
            Collections.singleton(idSingleCat2)
        ); //1
        PskuCluster expectedCluster1 = new PskuCluster(
            pskuGroup1Cat1,
            Arrays.asList(pskuGroup1Cat1, pskuGroup2Cat1),
            EnumSet.of(ClusterType.BARCODE),
            "BARCODE:1",
            CATEGORY_ID
        );
        PskuCluster expectedCluster2 = new PskuCluster(
            pskuGroup1Cat2,
            Arrays.asList(pskuGroup1Cat2, pskuGroup2Cat2),
            EnumSet.of(ClusterType.BARCODE),
            "BARCODE:2",
            CATEGORY_ID_2
        );

        Assertions.assertThat(totalResult.getClusterTypes()).isEqualTo(clusterTypeExpected);
        Assertions.assertThat(totalResult.getNotEligiblePskuByCategory()).isEqualTo(notEligibleExpected);
        Assertions.assertThat(totalResult.getSinglePskuByCategory()).isEqualTo(singleForExpected);
        Assertions.assertThat(totalResult.getClusters()).containsExactlyInAnyOrder(expectedCluster1, expectedCluster2);
    }

    private Psku createPsku(long id, long categoryId) {
        return new Psku(categoryId, 123, id, "", Collections.emptyList(),
            Collections.emptyList(), "", "");
    }
}
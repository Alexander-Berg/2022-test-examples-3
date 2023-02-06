package ru.yandex.market.psku.postprocessor.clusterization.service;

import com.google.common.collect.Sets;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.psku.postprocessor.clusterization.pojo.ClusterType;
import ru.yandex.market.psku.postprocessor.clusterization.pojo.Psku;
import ru.yandex.market.psku.postprocessor.clusterization.pojo.PskuCluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConnectedComponentClusterReducerTest {

    public static final int CATEGORY_ID = 123456;
    public static final int CATEGORY_ID_2 = 345678;

    @Test
    public void reduce() {
        List<Psku> pskus = new ArrayList<>();
        for (int i = 0; i < 13; ++i) {
            Psku psku = createPsku(
                i + 1,
                i != 10 ? CATEGORY_ID : CATEGORY_ID_2
            );
            pskus.add(psku);
        }

        List<PskuCluster> pskuClusters = Arrays.asList(
            new PskuCluster(
                pskus.get(0),
                Arrays.asList(pskus.get(0), pskus.get(1)),
                Collections.singleton(ClusterType.BARCODE),
                "BARCODE:01",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(1),
                Arrays.asList(pskus.get(1), pskus.get(0)),
                Collections.singleton(ClusterType.BARCODE),
                "BARCODE:01",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(2),
                Arrays.asList(pskus.get(2), pskus.get(3)),
                Collections.singleton(ClusterType.BARCODE),
                "BARCODE:23",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(2),
                Arrays.asList(pskus.get(2), pskus.get(3)),
                Collections.singleton(ClusterType.VENDOR_CODE),
                "VENDOR_CODE:23",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(2),
                Arrays.asList(pskus.get(2), pskus.get(4)),
                Collections.singleton(ClusterType.VENDOR_CODE),
                "VENDOR_CODE:24",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(3),
                Arrays.asList(pskus.get(3), pskus.get(2)),
                Collections.singleton(ClusterType.BARCODE),
                "BARCODE:23",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(3),
                Arrays.asList(pskus.get(3), pskus.get(2)),
                Collections.singleton(ClusterType.VENDOR_CODE),
                "VENDOR_CODE:23",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(4),
                Arrays.asList(pskus.get(4), pskus.get(2), pskus.get(5)),
                Collections.singleton(ClusterType.VENDOR_CODE),
                "VENDOR_CODE:425",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(5),
                Arrays.asList(pskus.get(5), pskus.get(4)),
                Collections.singleton(ClusterType.VENDOR_CODE),
                "VENDOR_CODE:54",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(5),
                Arrays.asList(pskus.get(5), pskus.get(6)),
                Collections.singleton(ClusterType.VENDOR_CODE),
                "VENDOR_CODE:56",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(6),
                Arrays.asList(pskus.get(6), pskus.get(5)),
                Collections.singleton(ClusterType.VENDOR_CODE),
                "VENDOR_CODE:56",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(7),
                Arrays.asList(pskus.get(7), pskus.get(8)),
                Collections.singleton(ClusterType.BARCODE),
                "BARCODE:78",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(7),
                Arrays.asList(pskus.get(7), pskus.get(9), pskus.get(10)),
                Collections.singleton(ClusterType.VENDOR_CODE),
                "VENDOR_CODE:79(10)",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(8),
                Arrays.asList(pskus.get(8), pskus.get(7)),
                Collections.singleton(ClusterType.BARCODE),
                "BARCODE:78",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(8),
                Arrays.asList(pskus.get(8), pskus.get(9), pskus.get(10)),
                Collections.singleton(ClusterType.VENDOR_CODE),
                "VENDOR_CODE:89(10)",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(9),
                Arrays.asList(pskus.get(9), pskus.get(7), pskus.get(8)),
                Collections.singleton(ClusterType.VENDOR_CODE),
                "VENDOR_CODE:978",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(10),
                Arrays.asList(pskus.get(10), pskus.get(7), pskus.get(8)),
                Collections.singleton(ClusterType.VENDOR_CODE),
                "VENDOR_CODE:(10)78",
                CATEGORY_ID_2
            ),
            new PskuCluster(
                pskus.get(10),
                Arrays.asList(pskus.get(10), pskus.get(11), pskus.get(12)),
                Collections.singleton(ClusterType.BARCODE),
                "BARCODE:(10)(11)(12)",
                CATEGORY_ID_2
            ),
            new PskuCluster(
                pskus.get(11),
                Arrays.asList(pskus.get(11), pskus.get(10), pskus.get(12)),
                Collections.singleton(ClusterType.BARCODE),
                "BARCODE:(10)(11)(12)",
                CATEGORY_ID
            ),
            new PskuCluster(
                pskus.get(12),
                Arrays.asList(pskus.get(12), pskus.get(10), pskus.get(11)),
                Collections.singleton(ClusterType.BARCODE),
                "BARCODE:(10)(11)(12)",
                CATEGORY_ID
            )
        );

        ConnectedComponentClusterReducer reducer = new ConnectedComponentClusterReducer();

        reducer.consume(pskuClusters);
        List<PskuCluster> result = reducer.reduce();

        Map<Long, PskuCluster> expected = new HashMap<>();
        expected.put(
            pskus.get(0).getId(),
            new PskuCluster(
                pskus.get(0),
                Arrays.asList(pskus.get(0), pskus.get(1)),
                Collections.singleton(ClusterType.BARCODE),
                "BARCODE:01",
                CATEGORY_ID
            )
        );
        expected.put(
            pskus.get(2).getId(),
            new PskuCluster(
                pskus.get(2),
                Arrays.asList(pskus.get(2), pskus.get(3), pskus.get(4), pskus.get(5), pskus.get(6)),
                Sets.newHashSet(ClusterType.BARCODE, ClusterType.VENDOR_CODE),
                "BARCODE:23; VENDOR_CODE:23; VENDOR_CODE:56; VENDOR_CODE:24; VENDOR_CODE:425; VENDOR_CODE:54",
                CATEGORY_ID
            )
        );
        expected.put(
            pskus.get(10).getId(),
            new PskuCluster(
                pskus.get(10),
                Arrays.asList(pskus.get(7), pskus.get(8), pskus.get(9), pskus.get(10), pskus.get(11), pskus.get(12)),
                Sets.newHashSet(ClusterType.BARCODE, ClusterType.VENDOR_CODE),
                "BARCODE:78; BARCODE:(10)(11)(12); VENDOR_CODE:79(10); VENDOR_CODE:978; VENDOR_CODE:(10)78; " +
                    "VENDOR_CODE:89(10)",
                CATEGORY_ID_2
            )
        );

        Map<Long, PskuCluster> resultMap = result.stream()
            .collect(Collectors.toMap(c -> c.getMainPsku().getId(), c -> c));

        Assertions.assertThat(resultMap.size()).isEqualTo(expected.size());

        for (Map.Entry<Long, PskuCluster> entry : expected.entrySet()) {
            Long key = entry.getKey();
            PskuCluster expectedCluster = entry.getValue();
            PskuCluster resultCluster = resultMap.get(key);

            Assertions.assertThat(resultCluster).isNotNull();

            Assertions.assertThat(resultCluster.getPskus())
                .containsExactlyInAnyOrder(expectedCluster.getPskus().toArray(new Psku[0]));
            Assertions.assertThat(resultCluster.getClusterTypes()).isEqualTo(expectedCluster.getClusterTypes());
            Assertions.assertThat(splitGroupData(resultCluster.getGroupData()))
                .containsExactlyInAnyOrder(splitGroupData(resultCluster.getGroupData()));

            Assertions.assertThat(resultCluster.getMainPsku()).isEqualTo(expectedCluster.getMainPsku());
            Assertions.assertThat(resultCluster.getCategoryId()).isEqualTo(expectedCluster.getCategoryId());
        }
    }

    private String[] splitGroupData(String groupData) {
        return groupData.split("; ");
    }

    private Psku createPsku(long id, long categoryId) {
        return new Psku(categoryId, 123, id, "", Collections.emptyList(),
            Collections.emptyList(), "", "");
    }
}
package ru.yandex.market.psku.postprocessor.clusterization;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.clusterization.pojo.ClusterType;
import ru.yandex.market.psku.postprocessor.clusterization.service.ClusterizationService;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuInClusterInfo;
import ru.yandex.market.psku.postprocessor.config.ManualTestConfig;
import ru.yandex.market.psku.postprocessor.msku_creation.ClusterPriorityService;
import ru.yandex.market.psku.postprocessor.msku_creation.PrioritizedCluster;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


@ContextConfiguration(classes = ManualTestConfig.class)
public class ClusterizeWorkerTest extends BaseDBTest {
    private static final int CLUSTERS_COUNT_LIMIT = 10000;
    @Autowired
    private ClusterizationService clusterizationService;

    @Autowired
    private ClusterPriorityService clusterPriorityService;

    @Autowired
    private ModelStorageHelper modelStorageHelper;

    private List<ModelStorage.Model> getClusterModels(PrioritizedCluster cluster) {
        Long categoryId = cluster.getPskus().get(0).getCategoryId();
        List<Long> pskuIds = cluster.getPskus().stream()
            .map(PskuInClusterInfo::getPskuId)
            .collect(Collectors.toList());
        return modelStorageHelper.getModels(categoryId, pskuIds);
    }

    @Ignore
    @Test
    public void getPSkuClustersTopFives() {
        try (PrintWriter writer = new PrintWriter("pskuClusters.txt", "UTF-8")) {
            clusterPriorityService.refresh();
            List<PrioritizedCluster> prioritizedClusters =
                    clusterPriorityService.getClusterPriorityList(CLUSTERS_COUNT_LIMIT);

            Map<Long, List<ModelStorage.Model>> mbyc = new HashMap<>();
            prioritizedClusters.forEach(c->{
                List<ModelStorage.Model> models;
                models = getClusterModels(c);
                mbyc.put(c.getId(), models);
                Map<Long, Long> counts = models.stream()
                    .collect(Collectors.groupingBy(ModelStorage.Model::getSupplierId, Collectors.counting()));
                long distinctSuppliers = counts.entrySet().size();
//                c.setDistinctSuppliers((int) distinctSuppliers); // Requires making field non-final
            });
            prioritizedClusters.sort(Comparator.reverseOrder());
            prioritizedClusters.stream()
                .filter(c -> c.getDistinctSuppliers() == 1)
                .limit(5)
                .forEach(pc -> {
                    List<ModelStorage.Model> models = mbyc.get(pc.getId());
                    mbyc.put(pc.getId(), models);
                    models.forEach(model -> writer.print(pskuToTsv(pc.getId(), model)));
                    writer.println();
                });
            prioritizedClusters.stream()
                .filter(c -> c.getDistinctSuppliers() == 2)
                .limit(5)
                .forEach(pc -> {
                    List<ModelStorage.Model> models = mbyc.get(pc.getId());
                    mbyc.put(pc.getId(), models);
                    models.forEach(model -> writer.print(pskuToTsv(pc.getId(), model)));
                    writer.println();
                });
            prioritizedClusters.stream()
                .filter(c -> c.getDistinctSuppliers() == 3)
                .limit(5)
                .forEach(pc -> {
                    List<ModelStorage.Model> models = mbyc.get(pc.getId());
                    mbyc.put(pc.getId(), models);
                    models.forEach(model -> writer.print(pskuToTsv(pc.getId(), model)));
                    writer.println();
                });
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void getPSkuClusters() {
        try (PrintWriter writer = new PrintWriter("pskuClusters.txt", "UTF-8")) {
            clusterPriorityService.refresh();
            List<PrioritizedCluster> prioritizedClusters =
                    clusterPriorityService.getClusterPriorityList(CLUSTERS_COUNT_LIMIT);
            Map<Long, List<ModelStorage.Model>> mbyc = new HashMap<>();
            prioritizedClusters.forEach(c->{
                List<ModelStorage.Model> models = getClusterModels(c);
                Map<Long, Long> counts = models.stream()
                    .collect(Collectors.groupingBy(ModelStorage.Model::getSupplierId, Collectors.counting()));
                long distinctSuppliers = counts.entrySet().size();
//                c.setDistinctSuppliers((int) distinctSuppliers); // Requires making field non final
                mbyc.put(c.getId(), models);
            });
            prioritizedClusters.sort(Comparator.reverseOrder());
            prioritizedClusters.forEach(pc -> {
                    long clusterId = pc.getId();
                    List<ModelStorage.Model> models = mbyc.get(clusterId);
                    models.forEach(model -> writer.print(pskuToTsv(clusterId, model)));
                    writer.print("\n");
                    writer.flush();
                }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String pskuToTsv(long clusterId, ModelStorage.Model model) {
        return clusterId + "\t" +
            model.getSupplierId() + "\t" +
            String.format("https://mbo.market.yandex.ru/gwt/#modelEditor/entity-id=%d", model.getId()) +
            "\t" + model.getTitles(0).getValue() + "\n";
    }

    private long getDistinctParentModelsCount(List<ModelStorage.Model> models) {
        return models.stream().map(model ->
            model.getRelationsList().stream()
                .filter(relation -> relation.getType() == ModelStorage.RelationType.SKU_PARENT_MODEL)
                .findFirst().get().getId())
            .distinct()
            .count();
    }


    //manual clusterization starter for dev purpose, uses YT, do not remove ignore in master
    @Ignore
    @Test
    public void startManualClusterization() {
        try {
            clusterizationService.createClustersByTypes(EnumSet.of(ClusterType.VENDOR_CODE));
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
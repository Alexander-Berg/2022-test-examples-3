package ru.yandex.market.psku.postprocessor.bazinga.deduplication;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MbocCommon.BusinessSkuKey;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterMetaDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.TaskPropertiesDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterMeta;
import ru.yandex.market.psku.postprocessor.service.deduplication.TaskPropertiesService;

import static java.lang.Math.random;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;


public class CreatePskuForDSBSOfferTaskTest extends BaseDBTest {

    private static final int CLUSTERS_TO_BE_GENERATED = 1500;

    @Autowired
    ClusterMetaDao clusterMetaDao;

    @Autowired
    ClusterContentDao clusterContentDao;

    @Autowired
    TaskPropertiesDao taskPropertiesDao;

    MboMappingsService mboMappingsServiceMock;
    CreatePskuForDSBSOfferTask createPskuForDSBSOfferTask;

    @Before
    public void setUp() throws Exception {
        System.setProperty("configs.path",
                getClass().getClassLoader().getResource("task_properties_service_test.properties").getFile());
        mboMappingsServiceMock = Mockito.mock(MboMappingsService.class);
        createPskuForDSBSOfferTask = new CreatePskuForDSBSOfferTask(
                clusterMetaDao, clusterContentDao, mboMappingsServiceMock,
            new TaskPropertiesService(taskPropertiesDao)
        );

        Mockito.when(mboMappingsServiceMock.addOfferToContentProcessing(Mockito.any())).thenAnswer(invocation -> {
            MboMappings.AddToContentProcessingResponse.Builder responseBuilder = MboMappings.AddToContentProcessingResponse.newBuilder();

            MboMappings.AddToContentProcessingRequest request = invocation.getArgument(0);
            responseBuilder.addAllResult(
                    request.getBusinessSkuKeyList().stream().map(businessSkuKey ->
                        MboMappings.AddToContentProcessingResponse.Result.newBuilder()
                                    .setBusinessSkuKey(
                                            BusinessSkuKey.newBuilder()
                                                    .setBusinessId(businessSkuKey.getBusinessId())
                                                    .setOfferId(businessSkuKey.getOfferId())
                                                    .build()
                                    )
                                    .setStatus(
                                            businessSkuKey.getBusinessId() % 10 == 0
                                                    ? MboMappings.AddToContentProcessingResponse.Status.ERROR
                                                    : MboMappings.AddToContentProcessingResponse.Status.OK
                                    )
                                    .build()
                    ).collect(Collectors.toList())
            );

            return responseBuilder.build();
        });

        generateDbObjects();
    }

    @Test
    public void testClusterMetaStatusesCorrect() {
        createPskuForDSBSOfferTask.execute(null);
        clusterMetaDao.findAll().forEach(clusterMeta -> {
            List<ClusterContent> clusterContents = clusterContentDao.fetchByClusterMetaId(clusterMeta.getId());

            if (clusterContents.isEmpty()) {
                assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.CREATE_CARD_UNSUCCESS);
            } else {
                Long businessId = clusterContents.get(0).getBusinessId();
                if (businessId % 10 == 0) {
                    assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.CREATE_CARD_UNSUCCESS);
                    assertThat(clusterContents).allMatch(el -> el.getStatus() == ClusterContentStatus.CARD_CREATE_UNSUCCESS);
                } else {
                    assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.CREATE_CARD_IN_PROCESS);
                    assertThat(clusterContents).anyMatch(el -> el.getStatus() == ClusterContentStatus.CARD_CREATE_IN_PROCESS);
                }
            }
        });
    }

    private void generateDbObjects() {
        IntStream.range(0, CLUSTERS_TO_BE_GENERATED).forEach(i -> {
            ClusterMeta clusterMeta = createClusterMeta();
            int offersCount = i % 13;
            for (int j = 0; j < offersCount; j++) {
                createClusterContent(clusterMeta.getId(), i, (double) j);
            }
        });
    }

    public ClusterMeta createClusterMeta() {
        ClusterMeta clusterMeta = new ClusterMeta();
        clusterMeta.setType(ClusterType.DSBS);
        clusterMeta.setStatus(ClusterStatus.CREATE_CARD_NEW);
        clusterMetaDao.insert(clusterMeta);
        return clusterMeta;
    }

    public ClusterContent createClusterContent(Long clusterMetaId, Integer businessId, Double weight) {
        ClusterContent clusterContent = new ClusterContent();
        clusterContent.setClusterMetaId(clusterMetaId);
        clusterContent.setType(ClusterContentType.DSBS);
        clusterContent.setBusinessId((long) businessId);
        clusterContent.setOfferId(format("%d-offer-%d-%e-%f", clusterMetaId, businessId, weight, random()));
        clusterContent.setWeight(weight);
        clusterContent.setStatus(ClusterContentStatus.NEW);
        clusterContentDao.insert(clusterContent);
        return clusterContent;
    }

}
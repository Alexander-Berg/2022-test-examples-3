package ru.yandex.market.psku.postprocessor.bazinga.deduplication;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.ir.http.PartnerContentUi.ListGcSkuTicketRequest;
import ru.yandex.market.ir.http.PartnerContentUi.ListGcSkuTicketRequest.Filter.Column;
import ru.yandex.market.ir.http.PartnerContentUi.ListGcSkuTicketResponse;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterContentDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterGenerationDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ClusterMetaDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterContentType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ClusterType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterContent;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterGeneration;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ClusterMeta;

import static java.lang.Math.random;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class CreatePskuForDSBSOfferWatcherTest extends BaseDBTest {

    private static final int CLUSTERS_TO_BE_GENERATED = 1200;
    private static final LocalDateTime TIME_CREATED = LocalDateTime.of(2021, 11, 22, 13, 30);
    private static final LocalDateTime TIME_UPDATED = LocalDateTime.of(2021, 11, 22, 13, 31);
    private static final LocalDateTime TIME_GG_UPDATED = LocalDateTime.of(2021, 11, 22, 13, 35);

    @Autowired
    ClusterMetaDao clusterMetaDao;

    @Autowired
    ClusterContentDao clusterContentDao;

    @Autowired
    ClusterGenerationDao clusterGenerationDao;

    PartnerContentService partnerContentService;
    CreatePskuForDSBSOfferWatcher createPskuForDSBSOfferWatcher;

    @Before
    public void setUp() throws Exception {
        partnerContentService = Mockito.mock(PartnerContentService.class);
        createPskuForDSBSOfferWatcher = new CreatePskuForDSBSOfferWatcher(
                clusterMetaDao, clusterContentDao, partnerContentService, clusterGenerationDao
        );

        Mockito.when(partnerContentService.listGcSkuTickets(Mockito.any())).thenAnswer(invocation -> {
            ListGcSkuTicketRequest request = invocation.getArgument(0);
            ListGcSkuTicketResponse.Builder responseBuilder = ListGcSkuTicketResponse.newBuilder();

            int businessId = request.getFilterList().stream().filter(ListGcSkuTicketRequest.Filter::hasColumn)
                    .filter(filter -> filter.getColumn() == Column.SOURCE_ID)
                    .map(ListGcSkuTicketRequest.Filter::getValue).map(Integer::parseInt)
                    .findAny().orElse(-1);

            if (businessId == -1) {
                throw new IllegalStateException();
            } else if (businessId % 10 == 0) {
                responseBuilder.addData(
                        ListGcSkuTicketResponse.Row.newBuilder()
                                .setStatus(ListGcSkuTicketResponse.Status.MAPPING_REJECTED)
                                .setUpdateDate(Timestamp.valueOf(TIME_GG_UPDATED).getTime())
                                .build()
                );
            } else if (businessId % 13 == 0) {
                responseBuilder.addData(
                        ListGcSkuTicketResponse.Row.newBuilder()
                                .setStatus(ListGcSkuTicketResponse.Status.CANCELLED)
                                .setUpdateDate(Timestamp.valueOf(TIME_GG_UPDATED).getTime())
                                .build()
                );
            } else {
                responseBuilder.addData(
                        ListGcSkuTicketResponse.Row.newBuilder()
                                .setStatus(ListGcSkuTicketResponse.Status.SUCCESS)
                                .setResultMboPskuId((long) Math.abs(Math.random() * 10000))
                                .setUpdateDate(Timestamp.valueOf(TIME_GG_UPDATED).getTime())
                                .build()
                );
            }

            return responseBuilder.build();
        });

        generateDbObjects();
    }

    @Test
    public void testResultStatusesAreValid() {
        createPskuForDSBSOfferWatcher.execute(null);

        clusterMetaDao.findAll().forEach(clusterMeta -> {
            List<ClusterContent> clusterContents = clusterContentDao.fetchByClusterMetaId(clusterMeta.getId());

            assertThat(clusterContents).isNotEmpty();

            Long businessId = clusterContents.get(0).getBusinessId();
            if (businessId % 10 == 0) {
                // MAPPING REJECTED
                assertThat(clusterContents).filteredOn(
                        el -> el.getStatus() == ClusterContentStatus.CARD_CREATE_UNSUCCESS
                ).hasSize(1);
                assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.CREATE_CARD_NEW);
                assertThat(clusterContents).allMatch(
                        el -> el.getStatus() == ClusterContentStatus.CARD_CREATE_UNSUCCESS
                                || el.getStatus() == ClusterContentStatus.NEW
                );
            } else if (businessId % 13 == 0) {
                // CANCELLED
                assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.INVALID);
                assertThat(clusterContents).anyMatch(el -> el.getStatus() == ClusterContentStatus.CARD_CREATE_UNSUCCESS);
                assertThat(clusterContents).allMatch(
                        el -> el.getStatus() == ClusterContentStatus.CARD_CREATE_UNSUCCESS
                                || el.getStatus() == ClusterContentStatus.NEW
                );
            } else {
                // SUCCESS
                assertThat(clusterMeta.getStatus()).isEqualTo(ClusterStatus.MAPPING_MODERATION_NEW);
                assertThat(clusterMeta.getType()).isEqualTo(ClusterType.PSKU_EXISTS);
                assertThat(clusterContents).anyMatch(el -> el.getStatus() == ClusterContentStatus.CARD_CREATED);
                assertThat(clusterContents).filteredOn(el -> el.getStatus() == ClusterContentStatus.CARD_CREATED)
                        .allMatch(el -> el.getSkuId() != null);
                assertThat(clusterContents).filteredOn(el -> el.getStatus() == ClusterContentStatus.CARD_CREATED)
                        .allMatch(el -> el.getType() == ClusterContentType.PSKU);
            }
        });
    }

    private void generateDbObjects() {
        ClusterGeneration clusterGeneration = insertClusterGeneration();
        IntStream.range(0, CLUSTERS_TO_BE_GENERATED).forEach(i -> {
            ClusterMeta clusterMeta = createClusterMeta(clusterGeneration.getId());
            int offersCount = i % 13;
            for (int j = 0; j < offersCount; j++) {
                createClusterContent(clusterMeta.getId(), i, (double) j, ClusterContentStatus.NEW);
            }
            createClusterContent(clusterMeta.getId(), i, (double) (offersCount + 1), ClusterContentStatus.CARD_CREATE_IN_PROCESS);
        });
    }

    public ClusterGeneration insertClusterGeneration() {
        ClusterGeneration clusterGeneration = new ClusterGeneration();
        clusterGeneration.setCreateDate(Timestamp.from(Instant.now()));
        clusterGeneration.setYtPath("/");
        clusterGeneration.setIsCurrent(true);
        clusterGenerationDao.insert(clusterGeneration);
        return clusterGeneration;
    }

    public ClusterMeta createClusterMeta(Long clusterGenerationId) {
        ClusterMeta clusterMeta = new ClusterMeta();
        clusterMeta.setClusterGenerationId(clusterGenerationId);
        clusterMeta.setType(ClusterType.DSBS);
        clusterMeta.setStatus(ClusterStatus.CREATE_CARD_IN_PROCESS);
        clusterMeta.setCreateDate(Timestamp.valueOf(TIME_CREATED));
        clusterMeta.setUpdateDate(Timestamp.valueOf(TIME_UPDATED));
        clusterMetaDao.insert(clusterMeta);
        return clusterMeta;
    }

    public ClusterContent createClusterContent(
            Long clusterMetaId, Integer businessId, Double weight, ClusterContentStatus status
    ) {
        ClusterContent clusterContent = new ClusterContent();
        clusterContent.setClusterMetaId(clusterMetaId);
        clusterContent.setType(ClusterContentType.DSBS);
        clusterContent.setBusinessId((long) businessId);
        clusterContent.setOfferId(format("%d-offer-%d-%e-%f", clusterMetaId, businessId, weight, random()));
        clusterContent.setWeight(weight);
        clusterContent.setStatus(status);
        clusterContentDao.insert(clusterContent);
        return clusterContent;
    }
}
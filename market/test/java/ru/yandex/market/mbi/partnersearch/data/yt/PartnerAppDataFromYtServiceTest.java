package ru.yandex.market.mbi.partnersearch.data.yt;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.data.PartnerAppDataOuterClass;
import ru.yandex.market.mbi.partnersearch.AbstractFunctionalTest;
import ru.yandex.market.mbi.partnersearch.data.elastic.ElasticService;
import ru.yandex.market.mbi.partnersearch.data.elastic.SearchEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Тесты для {@link PartnerAppDataFromYtService}
 */
@DbUnitDataSet(before = "PartnerAppDataFromYtServiceTest.csv")
public class PartnerAppDataFromYtServiceTest extends AbstractFunctionalTest {
    private static final Timestamp NEWER = Timestamp.newBuilder().setSeconds(1635168817).build();
    private static final Timestamp OLDER = Timestamp.newBuilder().setSeconds(1632576816).build();

    private static final Map<Long, PartnerAppDataOuterClass.PartnerAppData> TEST_DATA = Map.of(
            //Новая заявка партнера, есть обновление в эластике
            3000L, buildPartnerAppData(3000L, List.of(1000L, 2000L), "12345", false),
            //Обновление заявки партнера, есть обновление в эластике
            3010L, buildPartnerAppData(3010L, List.of(100L, 103L), "102030", false),
            //Старая заявка партнера, но новое время обновления, нет обновления в эластике
            3011L, buildPartnerAppData(3011L, List.of(101L), "102031", false),
            //Обновленная заявка партнера, но старое время обновления, нет обновления в эластике
            3012L, buildPartnerAppData(3012L, List.of(102L), "102032", true),
            //Старая заявка партнера, но новое время обновления, но был флаг обязательного обновления,
            // есть обновление в эластике
            3013L, buildPartnerAppData(3013L, List.of(113L), "102031", false),
            //Старая заявка партнера, но новое время обновления и огрн, появится флаг обязательного обновления, нет
            // обновления в эластике
            3014L, buildPartnerAppData(3014L, List.of(114L), "102034", false),
            //Обновление удаленной заявки партнера, нет обновления в эластике
            3030L, buildPartnerAppData(3030L, List.of(0L), "000", false)
    );

    private static final Set<Long> EXPECTED_ELASTIC_PARTNER_DATA = Set.of(1000L, 2000L, 100L, 101L, 103L);

    @Autowired
    private ElasticService elasticService;

    @Autowired
    private PartnerAppDataFromYtService partnerAppDataFromYtService;

    private static PartnerAppDataOuterClass.PartnerAppData buildPartnerAppData(long requestId,
                                                                               List<Long> partnerIds,
                                                                               String ogrn,
                                                                               boolean older) {
        GeneralData.GeneralDataInfo generalDataInfo = GeneralData.GeneralDataInfo.newBuilder()
                .setActionType(GeneralData.ActionType.READ)
                .setUpdatedAt(older ? OLDER : NEWER)
                .build();
        return PartnerAppDataOuterClass.PartnerAppData.newBuilder()
                .setRequestId(requestId)
                .addAllPartnerIds(partnerIds)
                .setOrgName("OOO " + requestId)
                .setGeneralInfo(generalDataInfo)
                .setType(PartnerAppDataOuterClass.PartnerAppType.MARKETPLACE)
                .setInn("1234567")
                .setOgrn(ogrn)
                .setStatus(PartnerAppDataOuterClass.PartnerAppStatus.COMPLETED)
                .build();
    }

    @Test
    @DbUnitDataSet(after = "PartnerAppDataFromYtServiceTest.testSave.after.csv")
    void testSave() throws IOException {
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Collection<SearchEntity>> captorSearchEntities = ArgumentCaptor.forClass(Collection.class);

        Mockito.when(elasticService.getByPartnerIds(eq(Set.of(1000L, 2000L))))
                .thenReturn(List.of(createSearchEntity(1000L), createSearchEntity(2000L)));
        Mockito.when(elasticService.getByPartnerIds(eq(Set.of(100L, 103L))))
                .thenReturn(List.of(createSearchEntity(100L), createSearchEntity(103L)));
        Mockito.when(elasticService.getByPartnerIds(eq(Set.of(101L))))
                .thenReturn(List.of(createSearchEntity(101L)));
        Mockito.when(elasticService.getByPartnerIds(eq(Set.of(102L))))
                .thenReturn(List.of(createSearchEntity(102L)));

        partnerAppDataFromYtService.doSave(TEST_DATA);

        Mockito.verify(elasticService, Mockito.times(4)).getByPartnerIds(captor.capture());
        ArgumentCaptor<Collection> listArgumentCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(elasticService, Mockito.times(3))
                .updateAndCreateSearchEntities(captorSearchEntities.capture(), listArgumentCaptor.capture());

        Set<Long> collectedElasticCallPartnerIds =
                captorSearchEntities.getAllValues().stream().flatMap(Collection::stream).
                        map(SearchEntity::getPartnerId)
                        .collect(Collectors.toSet());
        assertThat(collectedElasticCallPartnerIds).isEqualTo(EXPECTED_ELASTIC_PARTNER_DATA);
    }

    private SearchEntity createSearchEntity(long partnerId) {
        SearchEntity searchEntity = new SearchEntity(partnerId, 1011L, "test");
        searchEntity.setPrimaryTerm(10L);
        searchEntity.setSeqNo(20L);
        return searchEntity;
    }
}

package ru.yandex.market.mbi.partnersearch.data.yt;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.data.PartnerDataOuterClass;
import ru.yandex.market.mbi.partnersearch.AbstractFunctionalTest;
import ru.yandex.market.mbi.partnersearch.data.elastic.ElasticService;
import ru.yandex.market.mbi.partnersearch.data.elastic.SearchEntity;

/**
 * Тесты для {@link PartnerDataFromYtService}
 */
@DbUnitDataSet(before = "PartnerDataFromYtServiceTest.csv")
public class PartnerDataFromYtServiceTest extends AbstractFunctionalTest {
    private static final Timestamp NEWER = Timestamp.newBuilder().setSeconds(1635168817).build();
    private static final Timestamp OLDER = Timestamp.newBuilder().setSeconds(1632576816).build();

    private static final Map<Long, PartnerDataOuterClass.PartnerData> TEST_DATA = Map.of(
            //Новый партнер
            100L, buildPartnerData(100L, 1000L, false, false),
            //Обновленный партнер
            200L, buildPartnerData(200L, 2000L, false, false),
            //Старый партнер, но новое время обновления
            300L, buildPartnerData(300L, 3000L, false, false),
            //Обновленный партнер, но старое время обновления
            400L, buildPartnerData(400L, 4000L, true, false),
            //Обновление удаленного партнера(не должны восстановить тк нет бизнеса)
            500L, buildPartnerData(500L, 0L, false, false),
            //Обновление удаленного партнера(не должны восстановить, но есть бизнес)
            600L, buildPartnerData(600L, 6000L, false, false),
            //Удаление партнера
            700L, buildPartnerData(700L, 7000L, false, true)
    );

    private static final Set<Long> EXPECTED_ELASTIC_DATA = Set.of(100L, 200L, 700L);

    @Autowired
    private ElasticService elasticService;

    @Autowired
    private PartnerDataFromYtService partnerDataFromYtService;

    @Test
    @DbUnitDataSet(after = "PartnerDataFromYtServiceTest.testSave.after.csv")
    void testSave() throws IOException {
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<SearchEntity> deleteCaptor = ArgumentCaptor.forClass(SearchEntity.class);

        Mockito.when(elasticService.getByPartnerIds(Set.of(700L)))
                .thenReturn(List.of(new SearchEntity(700L, 7000L, "business7000")));

        partnerDataFromYtService.doSave(TEST_DATA);
        Mockito.verify(elasticService, Mockito.times(3)).getByPartnerIds(captor.capture());

        Set<Long> processedPartnerIds = captor.getAllValues().stream()
                .flatMap(Collection::stream).collect(Collectors.toSet());
        Assertions.assertEquals(EXPECTED_ELASTIC_DATA, processedPartnerIds);

        Mockito.verify(elasticService, Mockito.times(1)).deleteSearchEntity(deleteCaptor.capture());
        List<SearchEntity> deletedValues = deleteCaptor.getAllValues();
        Assertions.assertEquals(1, deletedValues.size());
        Assertions.assertEquals(700, deletedValues.get(0).getPartnerId());
    }


    private static PartnerDataOuterClass.PartnerData buildPartnerData(long partnerId, long businessId,
                                                                      boolean older, boolean deleted) {
        GeneralData.GeneralDataInfo generalDataInfo = GeneralData.GeneralDataInfo.newBuilder()
                .setActionType(GeneralData.ActionType.READ)
                .setUpdatedAt(older ? OLDER : NEWER)
                .build();
        return PartnerDataOuterClass.PartnerData.newBuilder()
                .setPartnerId(partnerId)
                .setBusinessId(businessId)
                .setInternalName("partner" + partnerId)
                .setGeneralInfo(generalDataInfo)
                .setType(PartnerDataOuterClass.PartnerType.SUPPLIER)
                .setIsDeleted(deleted)
                .build();
    }
}

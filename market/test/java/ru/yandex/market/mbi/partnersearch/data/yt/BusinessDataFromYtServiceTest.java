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
import ru.yandex.market.mbi.data.BusinessDataOuterClass;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.partnersearch.AbstractFunctionalTest;
import ru.yandex.market.mbi.partnersearch.data.elastic.ElasticService;

/**
 * Тесты для {@link BusinessDataFromYtService}.
 */
@DbUnitDataSet(before = "BusinessDataFromYtServiceTest.csv")
public class BusinessDataFromYtServiceTest extends AbstractFunctionalTest {
    private static final Timestamp NEWER = Timestamp.newBuilder().setSeconds(1635168817).build();
    private static final Timestamp OLDER = Timestamp.newBuilder().setSeconds(1632576816).build();

    private static final Map<Long, BusinessDataOuterClass.BusinessData> TEST_DATA = Map.of(
            //Новый бизнес
            100L, buildBusinessData(100L, Set.of(10000L, 11000L), false, false),
            //Обновленный бизнес
            200L, buildBusinessData(200L, Set.of(20000L, 21000L), false, false),
            //Старый бизнес, но новое время обновления
            300L, buildBusinessData(300L, Set.of(30000L, 31000L), false, false),
            //Обновленный бизнес, но старое время обновления
            400L, buildBusinessData(400L, Set.of(40000L, 41000L), true, false),
            //Обновление удаленного бизнеса
            500L, buildBusinessData(500L, Set.of(50000L, 51000L), false, false),
            //Удаление бизнеса
            600L, buildBusinessData(600L, Set.of(60000L, 61000L), false, true)
    );

    private static final Set<Long> EXPECTED_ELASTIC_DATA = Set.of(10000L, 11000L, 20000L, 21000L, 60000L, 61000L);

    @Autowired
    private ElasticService elasticService;

    @Autowired
    private BusinessDataFromYtService businessDataFromYtService;

    @Test
    @DbUnitDataSet(after = "BusinessDataFromYtServiceTest.testSave.after.csv")
    void testSave() throws IOException {
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(List.class);

        businessDataFromYtService.doSave(TEST_DATA);
        Mockito.verify(elasticService, Mockito.times(3)).getByPartnerIds(captor.capture());

        Set<Long> processedPartnerIds = captor.getAllValues().stream()
                .flatMap(Collection::stream).collect(Collectors.toSet());
        Assertions.assertEquals(EXPECTED_ELASTIC_DATA, processedPartnerIds);
    }

    private static BusinessDataOuterClass.BusinessData buildBusinessData(long businessId, Collection<Long> partnerIds,
                                                                         boolean older, boolean deleted) {
        GeneralData.GeneralDataInfo generalDataInfo = GeneralData.GeneralDataInfo.newBuilder()
                .setActionType(GeneralData.ActionType.READ)
                .setUpdatedAt(older ? OLDER : NEWER)
                .build();
        return BusinessDataOuterClass.BusinessData.newBuilder()
                .setBusinessId(businessId)
                .addAllPartnerIds(partnerIds)
                .setName("business" + businessId)
                .setGeneralInfo(generalDataInfo)
                .setIsDeleted(deleted)
                .build();
    }
}

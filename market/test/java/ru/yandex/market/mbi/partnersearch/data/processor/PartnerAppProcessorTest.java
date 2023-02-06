package ru.yandex.market.mbi.partnersearch.data.processor;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.data.PartnerAppDataOuterClass;
import ru.yandex.market.mbi.partnersearch.AbstractFunctionalTest;
import ru.yandex.market.mbi.partnersearch.data.elastic.ElasticService;
import ru.yandex.market.mbi.partnersearch.data.elastic.SearchEntity;
import ru.yandex.market.mbi.partnersearch.data.entity.PartnerApp;
import ru.yandex.market.mbi.partnersearch.data.repository.PartnerAppRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяем изменение заявки на подключение в {@link PartnerAppProcessor}.
 */
@DbUnitDataSet(before = "PartnerAppProcessorTest.csv")
public class PartnerAppProcessorTest extends AbstractFunctionalTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Timestamp NEWER = Timestamp.newBuilder().setSeconds(1635168817).build();
    private static final Timestamp OLDER = Timestamp.newBuilder().setSeconds(1632576816).build();

    @Autowired
    private PartnerAppProcessor partnerAppProcessor;

    @Autowired
    private ElasticService elasticService;

    @Autowired
    private PartnerAppRepository partnerAppRepository;

    static Stream<Arguments> testUpdatePartnerAppData() {
        return Stream.of(Arguments.of("", "1234567", "OOO business200"),
                Arguments.of(" ", "1234567", "OOO business200"),
                Arguments.of(null, "1234567", "OOO business200"),
                Arguments.of("12345", "1234567", "OOO business200"),
                Arguments.of("102030", "1234567", "OOO newBusiness200"),
                Arguments.of("102030", "11234567", "OOO business200"));
    }

    @Test
    @DbUnitDataSet(after = "PartnerAppProcessorTest.delete.after.csv")
    public void testDeletePartnerApp() {
        processPartnerAppChanges(GeneralData.ActionType.DELETE, 3010L, Set.of(101L), "OOO business200", false);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(after = "PartnerAppProcessorTest.testDeleteNotExists.after.csv")
    public void testDeletePartnerAppNotExists() {
        processPartnerAppChanges(GeneralData.ActionType.DELETE, 3030L, Set.of(1000L, 2000L), "OOO business200", false);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(after = "PartnerAppProcessorTest.csv")
    public void testUpdateDeletedOlderPartnerApp() {
        processPartnerAppChanges(GeneralData.ActionType.UPDATE, 3010L, Set.of(1000L, 2000L), "IP business1000new",
                true);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(after = "PartnerAppProcessorTest.updateDeleted.after.csv")
    public void testUpdateDeletedPartnerApp() {
        processPartnerAppChanges(GeneralData.ActionType.UPDATE, 3011L, Set.of(101L), "OOO business", false);
        Mockito.verifyNoInteractions(elasticService);
    }

    @ParameterizedTest
    @MethodSource("testUpdatePartnerAppData")
    @DbUnitDataSet(after = "PartnerAppProcessorTest.update.after.csv")
    public void testUpdatePartnerApp(String ogrn, String inn, String jurName) throws IOException {
        SearchEntity searchEntity1 = new SearchEntity(101L, 1011L, "test");
        searchEntity1.setPrimaryTerm(10L);
        searchEntity1.setSeqNo(20L);
        searchEntity1.setOgrn("102030");
        searchEntity1.setInn("1234567");
        searchEntity1.setJurName("OOO business200");

        SearchEntity searchEntity2 = new SearchEntity(102L, 1011L, "business100new");
        searchEntity2.setPrimaryTerm(100L);
        searchEntity2.setSeqNo(200L);
        //если в эластике с пустым огрн, то должно обновиться
        searchEntity2.setOgrn(ogrn);
        searchEntity2.setInn(inn);
        searchEntity2.setJurName(jurName);


        ArgumentCaptor<List<SearchEntity>> updateCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<SearchEntity>> createCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.when(elasticService.getByPartnerIds(Set.of(101L, 102L, 100L)))
                .thenReturn(List.of(searchEntity1, searchEntity2));

        processPartnerAppChanges(GeneralData.ActionType.UPDATE, 3010L,
                Set.of(101L, 102L, 100L), "OOO business200", false);

        Mockito.verify(elasticService).updateAndCreateSearchEntities(updateCaptor.capture(), createCaptor.capture());

        //partnerId 102 обновился в эластике тк поменялся ОГРН,
        // 101 не обновился тк ничего не поменялось,
        // 100 не должен создаться в эластике
        List<SearchEntity> updateEntities = updateCaptor.getValue();
        assertThat(updateEntities).hasSize(1);
        SearchEntity updateEntity = updateEntities.get(0);
        assertThat(updateEntity.getPartnerId()).isEqualTo(102L);
        assertThat(updateEntity.getBusinessId()).isEqualTo(1011L);
        assertThat(updateEntity.getBusinessName()).isEqualTo("business100new");
        assertThat(updateEntity.getPrimaryTerm()).isEqualTo(100);
        assertThat(updateEntity.getSeqNo()).isEqualTo(200);

        List<SearchEntity> createEntities = createCaptor.getValue();
        assertThat(createEntities).isEmpty();
    }

    @Test
    @DbUnitDataSet(after = "PartnerAppProcessorTest.csv")
    public void testUpdateOlderPartnerApp() {
        processPartnerAppChanges(GeneralData.ActionType.UPDATE, 3010L,
                Set.of(1000L, 2000L, 3000L), "business100new", true);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(after = "PartnerAppProcessorTest.after.create.csv")
    public void testCreatePartnerApp() {
        processPartnerAppChanges(GeneralData.ActionType.CREATE, 3020L,
                Set.of(100L), "OOO business200", false);
    }

    private void processPartnerAppChanges(GeneralData.ActionType actionType, long requestId,
                                          Collection<Long> partnerIds, String name, boolean older) {
        GeneralData.GeneralDataInfo generalDataInfo = GeneralData.GeneralDataInfo.newBuilder()
                .setActionType(actionType)
                .setUpdatedAt(older ? OLDER : NEWER)
                .build();
        PartnerAppDataOuterClass.PartnerAppData partnerAppData = PartnerAppDataOuterClass.PartnerAppData.newBuilder()
                .setRequestId(requestId)
                .addAllPartnerIds(partnerIds)
                .setOrgName(name)
                .setInn("1234567")
                .setOgrn("102030")
                .setType(PartnerAppDataOuterClass.PartnerAppType.MARKETPLACE)
                .setStatus(PartnerAppDataOuterClass.PartnerAppStatus.COMPLETED)
                .setGeneralInfo(generalDataInfo)
                .build();
        partnerAppProcessor.accept(partnerAppData);

        if (!older) {
            PartnerApp partnerApp = partnerAppRepository.findById(requestId).orElseThrow();
            try {
                JSONAssert.assertEquals(
                        "{\"partner_app_id\":" + requestId +
                                ",\"org_name\":\"" + name +
                                "\",\"partner_ids\":" + partnerIds +
                                ",\"type\":\"MARKETPLACE\",\"status\":\"COMPLETED\",\"ogrn\":\"102030\"," +
                                "\"inn\":\"1234567\"}",
                        OBJECT_MAPPER.writeValueAsString(partnerApp.getPartnerAppData()),
                        JSONCompareMode.NON_EXTENSIBLE
                );
            } catch (JSONException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

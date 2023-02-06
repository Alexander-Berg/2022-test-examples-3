package ru.yandex.market.mbi.partnersearch.data.processor;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.data.BusinessDataOuterClass;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.partnersearch.AbstractFunctionalTest;
import ru.yandex.market.mbi.partnersearch.data.elastic.ElasticService;
import ru.yandex.market.mbi.partnersearch.data.elastic.SearchEntity;
import ru.yandex.market.mbi.partnersearch.data.entity.Business;
import ru.yandex.market.mbi.partnersearch.data.repository.BusinessRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "BusinessDataProcessorTest.csv")
public class BusinessDataProcessorTest extends AbstractFunctionalTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Timestamp NEWER = Timestamp.newBuilder().setSeconds(1635168817).build();
    private static final Timestamp OLDER = Timestamp.newBuilder().setSeconds(1632576816).build();

    @Autowired
    private BusinessDataProcessor businessDataProcessor;

    @Autowired
    private ElasticService elasticService;

    @Autowired
    private BusinessRepository businessRepository;

    @Value("${retry.attempts:5}")
    private int retryAttempts;

    @Test
    @DbUnitDataSet(after = "BusinessDataProcessorTest.testDeleteBusiness.after.csv")
    public void testDeleteBusiness() throws IOException {
        SearchEntity searchEntity = new SearchEntity(1000L, 100L, "test");
        Mockito.when(elasticService.getByPartnerIds(Set.of(1000L, 2000L))).then((Answer<List<SearchEntity>>)
                invocation -> List.of(searchEntity));
        processBusinessChanges(GeneralData.ActionType.DELETE, 100L, Set.of(1000L, 2000L), "business100", false);
        Mockito.verify(elasticService).deleteSearchEntity(Mockito.eq(searchEntity));
    }

    @Test
    @DbUnitDataSet(after = "BusinessDataProcessorTest.csv")
    public void testRetriesAndTransactionRollback() throws IOException {
        Mockito.when(elasticService.getByPartnerIds(Set.of(1000L))).then((Answer<List<SearchEntity>>) invocation -> {
            SearchEntity searchEntity = new SearchEntity(1000L, 100L, "test");
            return List.of(searchEntity);
        });
        Mockito.doThrow(new IOException("Mock exception")).when(elasticService)
                .updateAndCreateSearchEntities(Mockito.anyCollection(), Mockito.anyCollection());

        Assertions.assertThrows(RuntimeException.class,
                () -> processBusinessChanges(GeneralData.ActionType.UPDATE, 100L, Set.of(1000L),
                        "business1000new", false));
        Mockito.verify(elasticService, Mockito.times(retryAttempts * retryAttempts))
                .updateAndCreateSearchEntities(Mockito.anyCollection(), Mockito.anyCollection());
    }

    @Test
    @DbUnitDataSet(after = "BusinessDataProcessorTest.testDeleteBusinessNotExists.after.csv")
    public void testDeleteBusinessNotExists() throws IOException {
        processBusinessChanges(GeneralData.ActionType.DELETE, 200L, Set.of(1000L, 2000L), "business200", false);
        Mockito.verify(elasticService).getByPartnerIds(Set.of(1000L, 2000L));
        Mockito.verify(elasticService, Mockito.never()).deleteSearchEntity(Mockito.any());
    }

    @Test
    @DbUnitDataSet(after = "BusinessDataProcessorTest.csv")
    public void testUpdateDeletedOlderBusiness() {
        processBusinessChanges(GeneralData.ActionType.UPDATE, 1000L, Set.of(1000L, 2000L), "business1000new", true);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(after = "BusinessDataProcessorTest.testUpdateDeletedBusiness.after.csv")
    public void testUpdateDeletedBusiness() {
        processBusinessChanges(GeneralData.ActionType.UPDATE, 1000L, Set.of(1000L, 2000L), "business1000", false);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(after = "BusinessDataProcessorTest.testUpdateBusiness.after.csv")
    public void testUpdateBusiness() throws IOException {
        SearchEntity searchEntity1 = new SearchEntity(1000L, 100L, "test");
        SearchEntity searchEntity2 = new SearchEntity(2000L, 100L, "business100new");
        searchEntity1.setPrimaryTerm(10L);
        searchEntity1.setSeqNo(20L);
        searchEntity2.setPrimaryTerm(100L);
        searchEntity2.setPrimaryTerm(200L);

        ArgumentCaptor<List<SearchEntity>> updateCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<SearchEntity>> createCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.when(elasticService.getByPartnerIds(Set.of(1000L, 2000L, 3000L)))
                .thenReturn(List.of(searchEntity1, searchEntity2));

        processBusinessChanges(GeneralData.ActionType.UPDATE, 100L,
                Set.of(1000L, 2000L, 3000L), "business100new", false);
        Mockito.verify(elasticService).getByPartnerIds(Set.of(1000L, 2000L, 3000L));
        Mockito.verify(elasticService).updateAndCreateSearchEntities(updateCaptor.capture(), createCaptor.capture());

        //partnerId 1000 обновился в эластике тк поменялось имя, 2000 не обновился тк имя не поменялось, 3000 создался
        List<SearchEntity> updateEntities = updateCaptor.getValue();
        assertThat(updateEntities).hasSize(1);
        SearchEntity updateEntity = updateEntities.get(0);
        assertThat(updateEntity.getPartnerId()).isEqualTo(1000L);
        assertThat(updateEntity.getBusinessId()).isEqualTo(100L);
        assertThat(updateEntity.getBusinessName()).isEqualTo("business100new");
        assertThat(updateEntity.getPrimaryTerm()).isEqualTo(10);
        assertThat(updateEntity.getSeqNo()).isEqualTo(20);

        List<SearchEntity> createEntities = createCaptor.getValue();
        assertThat(createEntities).hasSize(0);
    }

    @Test
    @DbUnitDataSet(after = "BusinessDataProcessorTest.csv")
    public void testUpdateOlderBusiness() {
        processBusinessChanges(GeneralData.ActionType.UPDATE, 100L,
                Set.of(1000L, 2000L, 3000L), "business100new", true);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(after = "BusinessDataProcessorTest.testCreateBusiness.after.csv")
    public void testCreateBusiness() throws IOException {
        processBusinessChanges(GeneralData.ActionType.CREATE, 200L,
                Set.of(4000L), "business200", false);
        Mockito.verify(elasticService).getByPartnerIds(Set.of(4000L));
    }

    private void processBusinessChanges(GeneralData.ActionType actionType, long businessId,
                                        Collection<Long> partnerIds, String name, boolean older) {
        GeneralData.GeneralDataInfo generalDataInfo = GeneralData.GeneralDataInfo.newBuilder()
                .setActionType(actionType)
                .setUpdatedAt(older ? OLDER : NEWER)
                .build();
        BusinessDataOuterClass.BusinessData businessData = BusinessDataOuterClass.BusinessData.newBuilder()
                .setBusinessId(businessId)
                .addAllPartnerIds(partnerIds)
                .setName(name)
                .setGeneralInfo(generalDataInfo)
                .build();
        businessDataProcessor.accept(businessData);

        if (!older) {
            Business business = businessRepository.findById(businessId).orElseThrow();
            try {
                JSONAssert.assertEquals(
                        "{\"business_id\":" + businessId +
                                " ,\"name\":\"" + name + "\"" +
                                ",\"partner_ids\": " + partnerIds + "}",
                        OBJECT_MAPPER.writeValueAsString(business.getBusinessData()), JSONCompareMode.NON_EXTENSIBLE
                );
            } catch (JSONException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

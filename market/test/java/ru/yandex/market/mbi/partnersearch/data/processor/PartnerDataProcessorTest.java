package ru.yandex.market.mbi.partnersearch.data.processor;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.data.PartnerDataOuterClass;
import ru.yandex.market.mbi.partnersearch.AbstractFunctionalTest;
import ru.yandex.market.mbi.partnersearch.data.elastic.ElasticService;
import ru.yandex.market.mbi.partnersearch.data.elastic.SearchEntity;
import ru.yandex.market.mbi.partnersearch.data.entity.Partner;
import ru.yandex.market.mbi.partnersearch.data.repository.PartnerRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniil Ivanov storabic@yandex-team.ru
 */
@DbUnitDataSet(before = "PartnerDataProcessorTest.before.csv")
public class PartnerDataProcessorTest extends AbstractFunctionalTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Timestamp NEWER = Timestamp.newBuilder().setSeconds(1635168817).build();
    private static final Timestamp OLDER = Timestamp.newBuilder().setSeconds(1632576816).build();
    private static final SearchEntity SEARCH_ENTITY = new SearchEntity(100L, 999L, "businessName");

    static {
        SEARCH_ENTITY.setPartnerType(PartnerDataOuterClass.PartnerType.SHOP);
        SEARCH_ENTITY.setCampaignId(9L);
        SEARCH_ENTITY.setPlacementProgramTypes(List.of());
        SEARCH_ENTITY.setInternalName("ZimbabveShop");
        SEARCH_ENTITY.setOwnerUid(14L);
        SEARCH_ENTITY.setPrimaryTerm(100L);
        SEARCH_ENTITY.setSeqNo(200L);
    }

    @Autowired
    private PartnerDataProcessor partnerDataProcessor;

    @Autowired
    private ElasticService elasticService;

    @Autowired
    private PartnerRepository partnerRepository;

    @Value("${retry.attempts:5}")
    private int retryAttempts;

    private static Stream<Arguments> updatePartnerArgs() {
        return Stream.of(
                // Изменили имя
                Arguments.of(9L, 999L, "ZimbabveStore", null, PartnerDataOuterClass.PartnerType.SHOP, 14L),
                // Изменили кампанию
                Arguments.of(10L, 999L, "ZimbabveShop", null, PartnerDataOuterClass.PartnerType.SHOP, 14L),
                // Изменили бизнес
                Arguments.of(9L, 1000L, "ZimbabveShop", null, PartnerDataOuterClass.PartnerType.SHOP, 14L),
                // Изменили бизнес и овнера
                Arguments.of(9L, 1000L, "ZimbabveShop", null, PartnerDataOuterClass.PartnerType.SHOP, 15L),
                // Изменили программы размещения
                Arguments.of(9L, 999L, "ZimbabveShop",
                        Set.of(PartnerDataOuterClass.PlacementProgramType.DROPSHIP_BY_SELLER),
                        PartnerDataOuterClass.PartnerType.SHOP, 14L),
                // Изменили овнера
                Arguments.of(9L, 999L, "ZimbabveShop", null, PartnerDataOuterClass.PartnerType.SHOP, 17L),
                // ЗаNULLили внутреннее название магазина
                Arguments.of(9L, 999L, null, null, PartnerDataOuterClass.PartnerType.SHOP, 17L),
                // Изменили тип (такого не должно быть на самом деле) и программы размещения
                Arguments.of(9L, 999L, "ZimbabveShop", Set.of(PartnerDataOuterClass.PlacementProgramType.DROPSHIP,
                                PartnerDataOuterClass.PlacementProgramType.FULFILLMENT),
                        PartnerDataOuterClass.PartnerType.SUPPLIER, 17L)
        );
    }

    private static String getPlacementProgramsJson(Set<PartnerDataOuterClass.PlacementProgramType> placementPrograms) {
        return " ,\"placement_programs\": [\"" +
                placementPrograms.stream().map(Enum::name).collect(Collectors.joining("\",\"")) +
                "\"]}";
    }

    @ParameterizedTest
    @MethodSource("updatePartnerArgs")
    @DisplayName("Проверяем взаимодействие с эластиком при обновлении партнеров")
    @SuppressWarnings("checkstyle:parameterNumber")
    public void testUpdatePartner(Long campaignId, Long businessId, String internalName,
                                  Set<PartnerDataOuterClass.PlacementProgramType> placementPrograms,
                                  PartnerDataOuterClass.PartnerType partnerType, Long ownerUid
    ) throws IOException {
        ArgumentCaptor<SearchEntity> updateCaptor = ArgumentCaptor.forClass(SearchEntity.class);
        Mockito.when(elasticService.getByPartnerIds(Set.of(100L))).thenReturn(List.of(SEARCH_ENTITY));

        processPartnerChanges(GeneralData.ActionType.UPDATE, 100L, campaignId, businessId, internalName,
                partnerType, placementPrograms, ownerUid, false);
        Mockito.verify(elasticService).getByPartnerIds(Set.of(100L));
        Mockito.verify(elasticService).updateSearchEntity(updateCaptor.capture());

        SearchEntity updateEntity = updateCaptor.getValue();
        assertThat(updateEntity.getPartnerId()).isEqualTo(100L);
        assertThat(updateEntity.getBusinessId()).isEqualTo(businessId);
        assertThat(updateEntity.getInternalName()).isEqualTo(internalName);
        assertThat(updateEntity.getOwnerUid()).isEqualTo(ownerUid);
        if (placementPrograms != null) {
            assertThat(updateEntity.getPlacementProgramTypes()).containsExactlyElementsOf(placementPrograms);
        }
        assertThat(updateEntity.getPrimaryTerm()).isEqualTo(100);
        assertThat(updateEntity.getSeqNo()).isEqualTo(200);
    }

    @Test
    @DisplayName("Проверяем взаимодействие с БД при обновлении партнеров")
    @DbUnitDataSet(after = "PartnerDataProcessorTest.testUpdatePartner.after.csv")
    public void testUpdatePartner() throws IOException {
        ArgumentCaptor<SearchEntity> updateCaptor = ArgumentCaptor.forClass(SearchEntity.class);
        // Изначальное состояние партнера в эластике
        Mockito.when(elasticService.getByPartnerIds(Set.of(100L))).thenReturn(List.of(SEARCH_ENTITY));

        // Изменяется название магазина и программы размещения
        processPartnerChanges(GeneralData.ActionType.UPDATE, 100L, 9L, 999L, "ZimbabveDBS",
                PartnerDataOuterClass.PartnerType.SHOP,
                Set.of(PartnerDataOuterClass.PlacementProgramType.DROPSHIP_BY_SELLER), 14L, false);
        Mockito.verify(elasticService).getByPartnerIds(Set.of(100L));
    }

    @Test
    @DisplayName("Проверяем взаимодействие с БД при обновлении агентства")
    @DbUnitDataSet(after = "PartnerDataProcessorTest.testUpdatePartnerAgency.after.csv")
    public void testUpdatePartnerAgency() throws IOException {
        ArgumentCaptor<SearchEntity> updateCaptor = ArgumentCaptor.forClass(SearchEntity.class);
        // Изначальное состояние партнера в эластике
        Mockito.when(elasticService.getByPartnerIds(Set.of(100L))).thenReturn(List.of(SEARCH_ENTITY));

        PartnerDataOuterClass.AgencyData agencyData = PartnerDataOuterClass.AgencyData.newBuilder()
                .setName("Super agency")
                .setAgencyId(1337L)
                .build();
        //Добавилось агентство
        processPartnerChanges(GeneralData.ActionType.UPDATE, 100L, 9L, 999L, "ZimbabveDBS",
                PartnerDataOuterClass.PartnerType.SHOP,
                Set.of(PartnerDataOuterClass.PlacementProgramType.DROPSHIP_BY_SELLER), 14L, null,
                agencyData, false);
        Mockito.verify(elasticService).getByPartnerIds(Set.of(100L));
        Mockito.verify(elasticService).updateSearchEntity(updateCaptor.capture());
        SearchEntity actual = updateCaptor.getValue();
        assertThat(actual.getAgencyId()).isEqualTo(1337L);
        assertThat(actual.getAgencyName()).isEqualTo("Super agency");
    }

    @Test
    @DisplayName("Проверяем взаимодействие с БД при обновлении менеджера")
    @DbUnitDataSet(after = "PartnerDataProcessorTest.testUpdatePartnerManager.after.csv")
    public void testUpdatePartnerManager() throws IOException {
        ArgumentCaptor<SearchEntity> updateCaptor = ArgumentCaptor.forClass(SearchEntity.class);
        // Изначальное состояние партнера в эластике
        Mockito.when(elasticService.getByPartnerIds(Set.of(100L))).thenReturn(List.of(SEARCH_ENTITY));

        PartnerDataOuterClass.ManagerData managerData = PartnerDataOuterClass.ManagerData.newBuilder()
                .setName("Ivanov Ivan")
                .setLogin("yndx-ivan")
                .setManagerId(1337L)
                .build();
        //Добавился менеджер
        processPartnerChanges(GeneralData.ActionType.UPDATE, 100L, 9L, 999L, "ZimbabveDBS",
                PartnerDataOuterClass.PartnerType.SHOP,
                Set.of(PartnerDataOuterClass.PlacementProgramType.DROPSHIP_BY_SELLER), 14L, managerData,
                null, false);
        Mockito.verify(elasticService).getByPartnerIds(Set.of(100L));
        Mockito.verify(elasticService).updateSearchEntity(updateCaptor.capture());
        SearchEntity actual = updateCaptor.getValue();
        assertThat(actual.getManagerId()).isEqualTo(1337L);
        assertThat(actual.getManagerLogin()).isEqualTo("yndx-ivan");
        assertThat(actual.getManagerName()).isEqualTo("Ivanov Ivan");
    }

    @Test
    @DbUnitDataSet(after = "PartnerDataProcessorTest.before.csv")
    public void testRetriesAndTransactionRollback() throws IOException {
        Mockito.when(elasticService.getByPartnerIds(Set.of(100L))).then((Answer<List<SearchEntity>>) invocation -> {
            SearchEntity searchEntity = new SearchEntity(100L, 999L, "businessName");
            return List.of(searchEntity);
        });
        Mockito.doThrow(new IOException("Mock exception")).when(elasticService)
                .updateSearchEntity(Mockito.any());

        Assertions.assertThrows(RuntimeException.class,
                () -> processPartnerChanges(GeneralData.ActionType.UPDATE, 100L, 9L,
                        999L, "ZimbabveDBS", PartnerDataOuterClass.PartnerType.SHOP,
                        Set.of(PartnerDataOuterClass.PlacementProgramType.DROPSHIP_BY_SELLER), 14L, false));
        Mockito.verify(elasticService, Mockito.times(retryAttempts * retryAttempts))
                .updateSearchEntity(Mockito.any());
    }

    @Test
    @DbUnitDataSet(after = "PartnerDataProcessorTest.testDeletePartner.after.csv")
    public void testDeletePartnerNotExistsInElastic() throws IOException {
        processPartnerChanges(GeneralData.ActionType.DELETE, 100L, 9L, 999L, "ZimbabveShop",
                PartnerDataOuterClass.PartnerType.SHOP, null, 14L, false);
        Mockito.verify(elasticService, Mockito.times(1)).getByPartnerIds(Mockito.anyCollection());
        Mockito.verifyNoMoreInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(after = "PartnerDataProcessorTest.testDeletePartner.after.csv")
    public void testDeletePartner() throws IOException {
        Mockito.when(elasticService.getByPartnerIds(Set.of(100L))).then((Answer<List<SearchEntity>>) invocation -> {
            SearchEntity searchEntity = new SearchEntity(100L, 999L, "businessName");
            return List.of(searchEntity);
        });

        processPartnerChanges(GeneralData.ActionType.DELETE, 100L, 9L, 999L, "ZimbabveShop",
                PartnerDataOuterClass.PartnerType.SHOP, null, 14L, false);
        ArgumentCaptor<SearchEntity> deleteCaptor = ArgumentCaptor.forClass(SearchEntity.class);
        Mockito.verify(elasticService, Mockito.times(1)).deleteSearchEntity(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().getPartnerId()).isEqualTo(100);
    }

    @Test
    @DbUnitDataSet(after = "PartnerDataProcessorTest.testDeletePartnerNotExists.after.csv")
    public void testDeletePartnerNotExists() throws IOException {
        processPartnerChanges(GeneralData.ActionType.DELETE, 300L, 3L, 333L, "NotExistsSHOP",
                PartnerDataOuterClass.PartnerType.SHOP, null, 14L, false);
        Mockito.verify(elasticService, Mockito.times(1)).getByPartnerIds(Mockito.anyCollection());
        Mockito.verifyNoMoreInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(after = "PartnerDataProcessorTest.before.csv")
    public void testUpdateDeletedOlderPartner() {
        processPartnerChanges(GeneralData.ActionType.UPDATE, 101L, 42L, 4224L, "SomewhereSUPPLIER",
                PartnerDataOuterClass.PartnerType.SUPPLIER, null, 14L, true);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(after = "PartnerDataProcessorTest.testUpdateDeletedPartner.after.csv")
    public void testUpdateDeletedPartner() {
        processPartnerChanges(GeneralData.ActionType.UPDATE, 101L, 101L, 0L, "NowhereSupplier",
                PartnerDataOuterClass.PartnerType.SUPPLIER, null, 14L, false);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(before = "PartnerDataProcessorTest.testCreatePartner.before.csv")
    public void testChangeBusiness() throws IOException {
        Mockito.when(elasticService.getByPartnerIds(Set.of(100L))).thenReturn(List.of(SEARCH_ENTITY));
        processPartnerChanges(GeneralData.ActionType.UPDATE, 100L, 9L, 2000L, "VladivostokShop",
                PartnerDataOuterClass.PartnerType.SHOP, null, 14L, false);
        Mockito.verify(elasticService).getByPartnerIds(Set.of(100L));
        ArgumentCaptor<SearchEntity> captor = ArgumentCaptor.forClass(SearchEntity.class);
        Mockito.verify(elasticService).updateSearchEntity(captor.capture());
        SearchEntity searchEntity = captor.getValue();
        assertThat(searchEntity.getBusinessName()).isEqualTo("businessName");
        assertThat(searchEntity.getPartnerId()).isEqualTo(100L);
        assertThat(searchEntity.getInternalName()).isEqualTo("VladivostokShop");
        assertThat(searchEntity.getBusinessId()).isEqualTo(2000L);
        assertThat(searchEntity.getCampaignId()).isEqualTo(9L);
        assertThat(searchEntity.getPartnerType()).isEqualTo(PartnerDataOuterClass.PartnerType.SHOP);

        //Проверяем что бизнесовый(300) контакт пророс в поисковую сущность
        assertThat(searchEntity.getContacts().size()).isEqualTo(1L);
        assertThat(searchEntity.getContacts().stream()
                .map(SearchEntity.Contact::getContactId)
                .collect(Collectors.toSet())).isEqualTo(Set.of(300L));
        assertThat(searchEntity.getOwnerContact().getContactId()).isEqualTo(300L);
    }

    @Test
    @DbUnitDataSet(after = "PartnerDataProcessorTest.before.csv")
    public void testUpdateOlderPartner() {
        processPartnerChanges(GeneralData.ActionType.UPDATE, 100L, 10L, 1000L, "ZimbabveShop",
                PartnerDataOuterClass.PartnerType.SHOP, null, 14L, true);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(before = "PartnerDataProcessorTest.testCreatePartner.before.csv",
            after = "PartnerDataProcessorTest.testCreatePartner.after.csv")
    public void testCreatePartner() throws IOException {
        processPartnerChanges(GeneralData.ActionType.CREATE, 200L, 20L, 2000L, "VladivostokShop",
                PartnerDataOuterClass.PartnerType.SHOP, null, 14L, false);
        Mockito.verify(elasticService).getByPartnerIds(Set.of(200L));
        ArgumentCaptor<SearchEntity> captor = ArgumentCaptor.forClass(SearchEntity.class);
        Mockito.verify(elasticService).createSearchEntity(captor.capture());
        SearchEntity searchEntity = captor.getValue();
        assertThat(searchEntity.getBusinessName()).isEqualTo("businessName");
        assertThat(searchEntity.getPartnerId()).isEqualTo(200L);
        assertThat(searchEntity.getInternalName()).isEqualTo("VladivostokShop");
        assertThat(searchEntity.getBusinessId()).isEqualTo(2000L);
        assertThat(searchEntity.getCampaignId()).isEqualTo(20L);
        assertThat(searchEntity.getPartnerType()).isEqualTo(PartnerDataOuterClass.PartnerType.SHOP);

        //Проверяем что прямой контакт(100) и бизнесовый(300) пророс в поисковую сущность, а удаленный(200) не пророс
        assertThat(searchEntity.getContacts().size()).isEqualTo(2L);
        assertThat(searchEntity.getContacts().stream()
                .map(SearchEntity.Contact::getContactId)
                .collect(Collectors.toSet())).isEqualTo(Set.of(100L, 300L));
    }

    @Test
    @DbUnitDataSet(after = "PartnerDataProcessorTest.before.csv")
    public void testCreatePartnerNotImportingType() throws IOException {
        processPartnerChanges(GeneralData.ActionType.CREATE, 200L, 20L, 2000L, "VladivostokShop",
                PartnerDataOuterClass.PartnerType.EATS_AND_LAVKA, null, 14L, true);
        Mockito.verifyNoInteractions(elasticService);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void processPartnerChanges(GeneralData.ActionType actionType, long partnerId,
                                       Long campaignId, Long businessId, String name,
                                       PartnerDataOuterClass.PartnerType partnerType,
                                       Set<PartnerDataOuterClass.PlacementProgramType> placementPrograms,
                                       Long ownerUid,
                                       boolean older) {
        processPartnerChanges(actionType, partnerId, campaignId, businessId, name, partnerType, placementPrograms,
                ownerUid, null, null, older);
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    private void processPartnerChanges(GeneralData.ActionType actionType, long partnerId,
                                       Long campaignId, Long businessId, String name,
                                       PartnerDataOuterClass.PartnerType partnerType,
                                       Set<PartnerDataOuterClass.PlacementProgramType> placementPrograms,
                                       Long ownerUid, PartnerDataOuterClass.ManagerData managerData,
                                       PartnerDataOuterClass.AgencyData agencyData,
                                       boolean older) {
        GeneralData.GeneralDataInfo generalDataInfo = GeneralData.GeneralDataInfo.newBuilder()
                .setActionType(actionType)
                .setUpdatedAt(older ? OLDER : NEWER)
                .build();

        PartnerDataOuterClass.PartnerData.Builder builder = PartnerDataOuterClass.PartnerData.newBuilder()
                .setPartnerId(partnerId)
                .setCampaignId(campaignId)
                .setBusinessId(businessId)
                .setGeneralInfo(generalDataInfo)
                .setOwnerUid(ownerUid)
                .setType(partnerType);

        if (name != null) {
            builder.setInternalName(name);
        }

        if (placementPrograms != null) {
            builder.addAllPlacementPrograms(placementPrograms);
        }
        if (managerData != null) {
            builder.setManager(managerData);
        }
        if (agencyData != null) {
            builder.setAgency(agencyData);
        }
        partnerDataProcessor.accept(builder.build());

        if (!older) {
            Partner partner = partnerRepository.findById(partnerId).orElseThrow();
            try {
                String businessIdField = businessId == 0 ? "" : " ,\"business_id\":" + businessId;
                JSONAssert.assertEquals(
                        "{\"partner_id\":" + partnerId +
                                " ,\"campaign_id\":" + campaignId +
                                businessIdField +
                                (name == null
                                        ? ""
                                        : " ,\"internal_name\":\"" + name + "\"") +
                                " ,\"owner_uid\":" + ownerUid +
                                ",\"partner_type\": " + partnerType +
                                (managerData == null ? "" : " ,\"manager_id\":" + managerData.getManagerId() + "" +
                                        " ,\"manager_login\":\"" + managerData.getLogin() + "\"" +
                                        " ,\"manager_name\":\"" + managerData.getName() + "\"") +
                                (agencyData == null ? "" : " ,\"agency_id\":" + agencyData.getAgencyId() + "" +
                                        " ,\"agency_name\":\"" + agencyData.getName() + "\"") +
                                (placementPrograms == null
                                        ? "}"
                                        : getPlacementProgramsJson(placementPrograms)),
                        OBJECT_MAPPER.writeValueAsString(partner.getPartnerData()), JSONCompareMode.NON_EXTENSIBLE
                );
            } catch (JSONException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

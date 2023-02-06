package ru.yandex.market.mbi.partnersearch.data.processor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.data.ContactDataOuterClass;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.partnersearch.AbstractFunctionalTest;
import ru.yandex.market.mbi.partnersearch.data.elastic.ElasticService;
import ru.yandex.market.mbi.partnersearch.data.elastic.SearchEntity;
import ru.yandex.market.mbi.partnersearch.data.entity.Contact;
import ru.yandex.market.mbi.partnersearch.data.repository.ContactRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link ContactDataProcessor}.
 */
@DbUnitDataSet(before = "ContactDataProcessorTest.csv")
public class ContactDataProcessorTest extends AbstractFunctionalTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Timestamp NEWER = Timestamp.newBuilder().setSeconds(1635168817).build();
    private static final Timestamp OLDER = Timestamp.newBuilder().setSeconds(1632576816).build();

    @Autowired
    private ContactDataProcessor contactDataProcessor;

    @Autowired
    private ElasticService elasticService;

    @Autowired
    private ContactRepository contactRepository;

    @Test
    @DbUnitDataSet(after = "ContactDataProcessorTest.delete.after.csv")
    public void testDeleteContact() throws IOException {
        processContactChanges(GeneralData.ActionType.DELETE, 3010L, "new-yndx-ivan", Map.of(), false);
        Mockito.verify(elasticService).getByPartnerIds(Set.of(100L, 101L));
    }

    @Test
    @DbUnitDataSet(after = "ContactDataProcessorTest.testDeleteNotExists.after.csv")
    public void testDeleteContactNotExists() {
        processContactChanges(GeneralData.ActionType.DELETE, 3030L, "yndx-ivan", Map.of(), false);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(after = "ContactDataProcessorTest.csv")
    public void testUpdateDeletedOlderContact() {
        processContactChanges(GeneralData.ActionType.UPDATE, 3011L, "yndx-ivan", Map.of(), true);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(before = "ContactDataProcessorTest.update.before.csv",
            after = "ContactDataProcessorTest.update.after.csv")
    public void testUpdateContact() throws IOException {
        SearchEntity searchEntity1 = new SearchEntity(100L, 999L, "businessName");
        SearchEntity searchEntity2 = new SearchEntity(404L, 405L, "businessName");
        SearchEntity searchEntity3 = new SearchEntity(101L, 1011L, "businessName");
        searchEntity1.setPrimaryTerm(10L);
        searchEntity1.setSeqNo(20L);
        searchEntity2.setSeqNo(100L);
        searchEntity2.setPrimaryTerm(200L);
        searchEntity3.setSeqNo(1000L);
        searchEntity3.setPrimaryTerm(2000L);
        searchEntity3.setContacts(
                List.of(new SearchEntity.Contact(3010L, "yndx-ivan", "Ivan Ivanov")));

        ArgumentCaptor<List<SearchEntity>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.when(elasticService.getByPartnerIds(Set.of(100L, 404L, 101L)))
                .thenReturn(List.of(searchEntity1, searchEntity2, searchEntity3));
        processContactChanges(GeneralData.ActionType.UPDATE, 3010L, "yndx-ivan",
                Map.of(100L, Set.of(ContactDataOuterClass.ContactRole.SHOP_ADMIN), 405L,
                        Set.of(ContactDataOuterClass.ContactRole.BUSINESS_OWNER)), false);
        Mockito.verify(elasticService, Mockito.atLeastOnce()).getByPartnerIds(Set.of(100L, 404L, 101L));
        Mockito.verify(elasticService).updateAndCreateSearchEntities(captor.capture(), Mockito.anyCollection());
        List<SearchEntity> searchEntities = captor.getValue();
        assertThat(searchEntities.stream()
                .filter(e -> e.getPartnerId() != 101L)
                .allMatch(e -> e.getContacts().stream()
                        .anyMatch(c -> c.getContactId() == 3010L)))
                .isTrue();

        assertThat(searchEntities.stream()
                .filter(e -> e.getPartnerId() == 101L)
                .noneMatch(e -> e.getContacts().stream()
                        .anyMatch(c -> c.getContactId() == 3010L)))
                .isTrue();
    }

    @Test
    @DbUnitDataSet(after = "ContactDataProcessorTest.csv")
    public void testUpdateOlderContact() {
        processContactChanges(GeneralData.ActionType.UPDATE, 3010L, "new-yndx-ivan",
                Map.of(100L, Set.of(ContactDataOuterClass.ContactRole.SHOP_ADMIN), 999L,
                        Set.of(ContactDataOuterClass.ContactRole.BUSINESS_OWNER)), true);
        Mockito.verifyNoInteractions(elasticService);
    }

    @Test
    @DbUnitDataSet(after = "ContactDataProcessorTest.create.after.csv")
    public void testCreateContact() throws IOException {
        SearchEntity searchEntity1 = new SearchEntity(100L, 999L, "businessName");
        SearchEntity searchEntity2 = new SearchEntity(101L, 1011L, "businessName");
        searchEntity1.setPrimaryTerm(10L);
        searchEntity1.setSeqNo(20L);
        searchEntity2.setPrimaryTerm(100L);
        searchEntity2.setPrimaryTerm(200L);

        ArgumentCaptor<List<SearchEntity>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.when(elasticService.getByPartnerIds(Set.of(100L, 101L)))
                .thenReturn(List.of(searchEntity1, searchEntity2));
        processContactChanges(GeneralData.ActionType.CREATE, 3333L, "new-yndx-ivan",
                Map.of(100L, Set.of(ContactDataOuterClass.ContactRole.SHOP_ADMIN), 1011L,
                        Set.of(ContactDataOuterClass.ContactRole.BUSINESS_OWNER)), false);
        Mockito.verify(elasticService, Mockito.atLeastOnce()).getByPartnerIds(Set.of(100L, 101L));
        Mockito.verify(elasticService).updateAndCreateSearchEntities(captor.capture(), Mockito.anyCollection());
        List<SearchEntity> searchEntities = captor.getValue();
        assertThat(searchEntities.stream()
                .allMatch(e -> e.getContacts().stream()
                        .anyMatch(c -> c.getContactId() == 3333L)))
                .isTrue();
    }

    private void processContactChanges(GeneralData.ActionType actionType, long contactId, String login,
                                       Map<Long, Set<ContactDataOuterClass.ContactRole>> roles, boolean older) {
        GeneralData.GeneralDataInfo generalDataInfo = GeneralData.GeneralDataInfo.newBuilder()
                .setActionType(actionType)
                .setUpdatedAt(older ? OLDER : NEWER)
                .build();
        ContactDataOuterClass.ContactData contactData = ContactDataOuterClass.ContactData.newBuilder()
                .setContactId(contactId)
                .setLogin(login)
                .setFirstName("Ivan")
                .setLastName("Ivanov")
                .setUserId(1337L)
                .setGeneralInfo(generalDataInfo)
                .addAllContactLink(roles.entrySet().stream().map(r ->
                                ContactDataOuterClass.ContactLinkData.newBuilder()
                                        .setPartnerId(r.getKey())
                                        .addAllRoles(r.getValue())
                                        .build())
                        .collect(Collectors.toList()))
                .build();
        contactDataProcessor.accept(contactData);

        if (!older) {
            Contact contact = contactRepository.findById(contactId).orElseThrow();
            try {
                String strRoles = roles.entrySet().stream()
                        .map(l -> "\"" + l.getKey() + "\":[" + l.getValue().stream()
                                .map(r -> "\"" + r.name() + "\"")
                                .collect(Collectors.joining(",")) + "]")
                        .collect(Collectors.joining(","));
                JSONAssert.assertEquals(
                        "{\n" +
                                "  \"contact_id\": " + contactId + ",\n" +
                                "  \"user_id\": 1337,\n" +
                                "  \"first_name\": \"Ivan\",\n" +
                                "  \"last_name\": \"Ivanov\",\n" +
                                "  \"second_name\": \"\",\n" +
                                "  \"login\": \"" + login + "\",\n" +
                                "  \"roles\": {" + strRoles + "}\n" +
                                "}",
                        OBJECT_MAPPER.writeValueAsString(contact.getContactData()), JSONCompareMode.NON_EXTENSIBLE
                );
            } catch (JSONException | JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

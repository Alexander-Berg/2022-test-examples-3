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
import ru.yandex.market.mbi.data.ContactDataOuterClass;
import ru.yandex.market.mbi.data.GeneralData;
import ru.yandex.market.mbi.partnersearch.AbstractFunctionalTest;
import ru.yandex.market.mbi.partnersearch.data.elastic.ElasticService;

import static ru.yandex.market.mbi.data.ContactDataOuterClass.ContactRole.BUSINESS_ADMIN;
import static ru.yandex.market.mbi.data.ContactDataOuterClass.ContactRole.SHOP_ADMIN;

/**
 * Тесты для {@link ContactDataFromYtService}.
 */
@DbUnitDataSet(before = "ContactDataFromYtServiceTest.csv")
public class ContactDataFromYtServiceTest extends AbstractFunctionalTest {
    private static final Timestamp NEWER = Timestamp.newBuilder().setSeconds(1635168817).build();
    private static final Timestamp OLDER = Timestamp.newBuilder().setSeconds(1632576816).build();

    private static final Map<Long, ContactDataOuterClass.ContactData> TEST_DATA = Map.of(
            //новый контакт
            100L, buildContactData(100L, Map.of(1000L, Set.of(SHOP_ADMIN), 20000L, Set.of(BUSINESS_ADMIN)), false),
            //обновленный контакт
            200L, buildContactData(200L, Map.of(1000L, Set.of(SHOP_ADMIN), 20000L, Set.of(BUSINESS_ADMIN)), false),
            //старый контакт, но новое время обновления
            300L, buildContactData(300L, Map.of(1000L, Set.of(SHOP_ADMIN), 20000L, Set.of(BUSINESS_ADMIN)), false),
            //обновленный контакт, но старое время обновление
            400L, buildContactData(400L, Map.of(1000L, Set.of(SHOP_ADMIN), 20000L, Set.of(BUSINESS_ADMIN)), true),
            //обновление контакта - партнера нет в базе, все равно обновляем
            500L, buildContactData(500L, Map.of(1000L, Set.of(SHOP_ADMIN), 6666L, Set.of(SHOP_ADMIN)), false),
            //обновление контакта - бизнеса нет в базе, все равно обновляем
            600L, buildContactData(600L, Map.of(1000L, Set.of(SHOP_ADMIN), 66666L, Set.of(BUSINESS_ADMIN)), false),
            //обновление удаленного контакта
            700L, buildContactData(700L, Map.of(1000L, Set.of(SHOP_ADMIN), 20000L, Set.of(BUSINESS_ADMIN)), false)
    );

    private static final Set<Long> EXPECTED_ELASTIC_DATA = Set.of(2000L, 1000L, 3000L, 6666L, 66666L);

    @Autowired
    private ElasticService elasticService;

    @Autowired
    private ContactDataFromYtService contactDataFromYtService;

    @Test
    @DbUnitDataSet(after = "ContactDataFromYtServiceTest.testSave.after.csv")
    void testSave() throws IOException {
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(List.class);

        contactDataFromYtService.doSave(TEST_DATA);
        Mockito.verify(elasticService, Mockito.times(4)).getByPartnerIds(captor.capture());

        Set<Long> processedPartnerIds = captor.getAllValues().stream()
                .flatMap(Collection::stream).collect(Collectors.toSet());
        Assertions.assertEquals(EXPECTED_ELASTIC_DATA, processedPartnerIds);
    }

    private static ContactDataOuterClass.ContactData buildContactData(
            long contactId,
            Map<Long, Set<ContactDataOuterClass.ContactRole>> roles,
            boolean older) {
        GeneralData.GeneralDataInfo generalDataInfo = GeneralData.GeneralDataInfo.newBuilder()
                .setActionType(GeneralData.ActionType.READ)
                .setUpdatedAt(older ? OLDER : NEWER)
                .build();
        return ContactDataOuterClass.ContactData.newBuilder()
                .setContactId(contactId)
                .setUserId(contactId * 2)
                .setLogin("yndx" + contactId)
                .addAllContactLink(roles.entrySet().stream()
                        .map(e -> ContactDataOuterClass.ContactLinkData.newBuilder()
                                .setPartnerId(e.getKey())
                                .setCampaignId(e.getKey() * 2)
                                .setContactId(contactId).addAllRoles(e.getValue()).build())
                        .collect(Collectors.toList()))
                .setGeneralInfo(generalDataInfo)
                .build();
    }
}

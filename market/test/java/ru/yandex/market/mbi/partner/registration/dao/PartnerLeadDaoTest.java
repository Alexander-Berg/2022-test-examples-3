package ru.yandex.market.mbi.partner.registration.dao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.partner.registration.AbstractFunctionalTest;
import ru.yandex.market.mbi.partner.registration.lead.dao.ContactInfo;
import ru.yandex.market.mbi.partner.registration.lead.dao.PartnerInfo;
import ru.yandex.market.mbi.partner.registration.lead.dao.PartnerLeadDao;
import ru.yandex.market.mbi.partner.registration.lead.dao.PartnerLeadEntity;

@DbUnitDataSet(before = "PartnerLeadDaoTestBefore.csv")
public class PartnerLeadDaoTest extends AbstractFunctionalTest {

    @Autowired
    private PartnerLeadDao partnerLeadDao;

    @Test
    void getTest() {
        PartnerLeadEntity partnerLead = partnerLeadDao.getPartnerLead(0);
        Assertions.assertEquals(partnerLead,
                PartnerLeadEntity.builder()
                        .setContactInfo(ContactInfo.builder()
                                .setFirstName("name")
                                .setLastName("surname")
                                .setEmail("test@yandex.ru")
                                .setLogin("login")
                                .setPhone("+71234567890")
                                .setUid(1L)
                                .build())
                        .setPartnerInfo(PartnerInfo.builder()
                                .setAssortmentCnt(1L)
                                .setMainCategory("category")
                                .setId(1L)
                                .build())
                        .setMetrikaCntId(1L)
                        .setSourceName("name")
                        .setOwTicketUpdateRequired(false)
                        .setOwPartnerCreated(true)
                        .setOwTicketId("1")
                        .build());
    }

    @Test
    @DbUnitDataSet(after = "PartnerLeadDaoTestAfterInsert.csv")
    void insertTest() {

        partnerLeadDao.insert(PartnerLeadEntity.builder()
                .setContactInfo(ContactInfo.builder()
                        .setFirstName("name2")
                        .setLastName("surname2")
                        .setEmail("test2@yandex.ru")
                        .setLogin("login2")
                        .setPhone("+71234567891")
                        .setUid(2L)
                        .build())
                .setPartnerInfo(PartnerInfo.builder()
                        .setAssortmentCnt(2L)
                        .setMainCategory("category2")
                        .setId(2L)
                        .build())
                .setMetrikaCntId(2L)
                .setSourceName("name2")
                .setOwTicketUpdateRequired(false)
                .setOwPartnerCreated(true)
                .setOwTicketId("1")
                .build());
    }

    @DbUnitDataSet(after = "PartnerLeadDaoTestAfterUpdateSemanticData.csv")
    @Test
    void updateSemanticDataTest() {

       partnerLeadDao.updateSemanticData(
               0L,
               ContactInfo.builder()
                       .setFirstName("name2")
                       .setLastName("surname2")
                       .setEmail("test2@yandex.ru")
                       .setLogin("login2")
                       .setPhone("+71234567891")
                       .setUid(2L)
                       .build(),
               PartnerInfo.builder()
                       .setAssortmentCnt(2L)
                       .setMainCategory("category2")
                       .setId(2L)
                       .build()
       );
    }

    @DbUnitDataSet(after = "PartnerLeadDaoTestAfterUpdateTicket.csv")
    @Test
    void updateTicketTest() {

        partnerLeadDao.updateTicket(0L, "test");
    }
}

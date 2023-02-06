package ru.yandex.market.mbisfintegration.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mbisfintegration.config.LeadConvertProperties;
import ru.yandex.market.mbisfintegration.generated.sf.model.Account;
import ru.yandex.market.mbisfintegration.generated.sf.model.Lead;
import ru.yandex.market.mbisfintegration.generated.sf.model.LeadConvert;
import ru.yandex.market.mbisfintegration.generated.sf.model.LeadConvertResult;
import ru.yandex.market.mbisfintegration.generated.sf.model.LeadStatus;
import ru.yandex.market.mbisfintegration.generated.sf.model.Opportunity;
import ru.yandex.market.mbisfintegration.generated.sf.model.QueryResult;
import ru.yandex.market.mbisfintegration.generated.sf.model.SObject;
import ru.yandex.market.mbisfintegration.generated.sf.model.SaveResult;
import ru.yandex.market.mbisfintegration.salesforce.SObjectType;
import ru.yandex.market.mbisfintegration.salesforce.impl.LeadSfService;
import ru.yandex.mj.generated.client.self_client.api.LeadApiClient;
import ru.yandex.mj.generated.client.self_client.model.Comment;
import ru.yandex.mj.generated.client.self_client.model.OwLead;
import ru.yandex.mj.generated.client.self_client.model.OwLeadOut;
import ru.yandex.mj.generated.client.self_client.model.OwLeadStatus;
import ru.yandex.mj.generated.client.self_client.model.OwLeadStatusOut;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {LeadApiServiceTest.PropertiesConfig.class})
class LeadApiServiceTest extends AbstractApiTest {

    private static final String LEAD_ID = "123xyz";
    private static final String CLIENT_NAME = "Василий Пупкин";
    private static final String CLIENT_EMAIL = "pupkin@ya.ru";
    private static final String CLIENT_PHONE = "+7(999)8887766";
    private static final String LEAD_TITLE = "Лид февраль 2022; Email: pupkin@ya.ru; Login: pupkin";
    private static final String LEAD_INITIAL_STATUS = "Promo";
    private static final String OPPORTUNITY_INITIAL_STATUS = "Model Is Selected";
    private static final Integer ASSORTMENT = 1000;
    private static final Integer GOOD_CATEGORY = 198118; //Бытовая техника
    private static final String PARTNER_ID = "456qwe";
    private static final String PARTNER_NAME = "ООО ПупкинКорп";
    private static final String LEAD_FINAL_STATUS = "Qualified";
    private static final String OPPORTUNITY_ID = "789abc";

    @Autowired
    private LeadApiClient client;

    @Test
    void createLead() throws Exception {
        when(soap.query(anyString())).thenReturn(new QueryResult().withRecords(List.of()));
        when(soap.create(anyList())).thenReturn(List.of(new SaveResult().withId(LEAD_ID).withSuccess(true)));
        OwLead owLead = new OwLead()
                .clientName(CLIENT_NAME)
                .clientEmail(CLIENT_EMAIL)
                .clientPhone(CLIENT_PHONE)
                .title(LEAD_TITLE)
                .source(OwLead.SourceEnum.EXPRESS)
                .atComment(new Comment().body("IGNORED"))
                .gid("IGNORED")
                .service("IGNORED")
                .categories("IGNORED")
                .partner("IGNORED")
                .loginShop("IGNORED")
                .productCategory(0) //ignored
                .status(OwLeadStatus.REGISTERED) //ignored
                .assortment(0);//ignored
        assertThat(call(client.createLead(owLead))).isEqualTo(LEAD_ID);
        verify(soap, times(1)).query("SELECT Id FROM Lead WHERE" +
                " LastName = '" + CLIENT_NAME + "'" +
                " AND Email = '" + CLIENT_EMAIL + "'" +
                " AND Phone = '" + CLIENT_PHONE + "'" +
                " AND Status = '" + LEAD_INITIAL_STATUS + "'" +
                " AND Company = '" + LEAD_TITLE + "'" +
                " LIMIT 1"
        );
        verify(soap, times(1)).create(List.of(
                new Lead()
                        .withLastName(CLIENT_NAME)
                        .withEmail(CLIENT_EMAIL)
                        .withPhone(CLIENT_PHONE)
                        .withCompany(LEAD_TITLE)
                        .withStatus(LEAD_INITIAL_STATUS)
                        .withExpressC(true)
                        .withSourceC(OwLead.SourceEnum.EXPRESS.getValue())
        ));
    }

    @Test
    void createLeadWithDuplicate() throws Exception {
        when(soap.query(anyString()))
                .thenReturn(new QueryResult().withRecords(List.of(new Lead().withId(LEAD_ID))));
        OwLead owLead = new OwLead()
                .clientName(CLIENT_NAME)
                .clientEmail(CLIENT_EMAIL)
                .clientPhone(CLIENT_PHONE)
                .title(LEAD_TITLE)
                .atComment(new Comment().body("IGNORED"))
                .source(OwLead.SourceEnum.EXPRESS);
        assertThat(call(client.createLead(owLead))).isEqualTo(LEAD_ID);
        verify(soap, times(1)).query("SELECT Id FROM Lead WHERE" +
                " LastName = '" + CLIENT_NAME + "'" +
                " AND Email = '" + CLIENT_EMAIL + "'" +
                " AND Phone = '" + CLIENT_PHONE + "'" +
                " AND Status = '" + LEAD_INITIAL_STATUS + "'" +
                " AND Company = '" + LEAD_TITLE + "'" +
                " LIMIT 1"
        );
        verify(soap, times(0)).create(anyList());
    }

    @ParameterizedTest
    @CsvSource({
            "true, false, ANY_STATUS, RESOLVED",
            "false, true, ANY_STATUS, RESOLVED",
            "false, false, " + LEAD_INITIAL_STATUS + ", REGISTERED",
            "false, false, NOT_INITIAL_STATUS, PROCESSING",
    })
    void getLead(boolean isConverted, boolean isDeleted, String leadStatus, OwLeadStatus owStatus) throws Exception {
        mockRetrieveLead(isConverted, isDeleted, leadStatus, null);
        assertThat(call(client.getLead(LEAD_ID))).isEqualTo(
                new OwLeadOut().gid(LEAD_ID).status(new OwLeadStatusOut().code(owStatus))
        );
        verify(soap, times(1)).retrieve(LeadSfService.RETRIEVE_FIELDS, SObjectType.LEAD.getId(), List.of(LEAD_ID));
    }

    @Test
    void testUpdateCategoriesAndAssortment() throws Exception {
        mockRetrieveLead();
        var owLead = new OwLead()
                .status(null)
                .partner(null)
                .assortment(ASSORTMENT)
                .productCategory(GOOD_CATEGORY);
        call(client.updateLead(LEAD_ID, owLead));
        verify(soap, times(1)).update(List.of(
                new Lead().withId(LEAD_ID)
                        .withSKUC("500-1000")
                        .withGoodsCategoriesC(GOOD_CATEGORY.toString())
                        .withCategoryC("Cehac")
        ));
    }

    @Test
    void testConvertLead() throws Exception {
        mockRetrieveLead();
        mockRetrieve(SObjectType.ACCOUNT, new Account().withId(PARTNER_ID).withName(PARTNER_NAME));
        mockQuery("FROM LeadStatus", new LeadStatus().withApiName(LEAD_FINAL_STATUS));
        //Не находим существующую сделку, создадим новую
        mockQuery("FROM Opportunity");

        when(soap.convertLead(anyList())).thenReturn(List.of(
                new LeadConvertResult().withOpportunityId(OPPORTUNITY_ID).withSuccess(true)
        ));

        var owLead = new OwLead().partner(PARTNER_ID);
        call(client.updateLead(LEAD_ID, owLead));
        verify(soap, times(1)).update(List.of(
                new Lead().withId(LEAD_ID).withCompany(PARTNER_NAME)
        ));
        verify(soap, times(1)).convertLead(List.of(
                new LeadConvert()
                        .withLeadId(LEAD_ID)
                        .withAccountId(PARTNER_ID)
                        .withContactId(null)
                        .withConvertedStatus(LEAD_FINAL_STATUS)
                        .withOpportunityName(PARTNER_NAME)
        ));
        verify(soap, times(1)).update(List.of(
                new Opportunity().withId(OPPORTUNITY_ID).withStageName(OPPORTUNITY_INITIAL_STATUS)
        ));
    }

    @Test
    void testConvertLeadWithExistedOpportunity() throws Exception {
        mockRetrieveLead();
        mockRetrieve(SObjectType.ACCOUNT, new Account().withId(PARTNER_ID).withName(PARTNER_NAME));
        mockQuery("FROM LeadStatus", new LeadStatus().withApiName(LEAD_FINAL_STATUS));
        //Нашли существующую сделку, привяжем лид к ней
        mockQuery("FROM Opportunity", new Opportunity().withId(OPPORTUNITY_ID));

        when(soap.convertLead(anyList())).thenReturn(List.of(
                new LeadConvertResult().withOpportunityId(OPPORTUNITY_ID).withSuccess(true)
        ));

        var owLead = new OwLead().partner(PARTNER_ID);
        call(client.updateLead(LEAD_ID, owLead));
        verify(soap, times(1)).update(List.of(
                new Lead().withId(LEAD_ID).withCompany(PARTNER_NAME)
        ));
        verify(soap, times(1)).convertLead(List.of(
                new LeadConvert()
                        .withLeadId(LEAD_ID)
                        .withAccountId(PARTNER_ID)
                        .withContactId(null)
                        .withConvertedStatus(LEAD_FINAL_STATUS)
                        .withOpportunityId(OPPORTUNITY_ID)
                        .withDoNotCreateOpportunity(true)
                        .withOpportunityName(null)
        ));
        verify(soap, times(1)).update(List.of(
                new Opportunity().withId(OPPORTUNITY_ID).withStageName(OPPORTUNITY_INITIAL_STATUS)
        ));
    }

    @Test
    void testRetriedConvertLead() throws Exception {
        mockRetrieveLead(true, false, LEAD_FINAL_STATUS, PARTNER_ID);
        call(client.updateLead(LEAD_ID, new OwLead().partner(PARTNER_ID)));
        verify(soap, times(0)).update(anyList());
        verify(soap, times(0)).convertLead(anyList());
        verify(soap, times(0)).update(anyList());
    }

    @Test
    void testIgnoredStatusTransition() throws Exception {
        mockRetrieveLead();
        var owLead = new OwLead()
                .status(OwLeadStatus.PROCESSING);
        call(client.updateLead(LEAD_ID, owLead));
        Mockito.verify(soap, times(1)).retrieve(LeadSfService.RETRIEVE_FIELDS, "Lead", List.of(LEAD_ID));
        Mockito.verify(soap, times(0)).update(anyList());
    }

    @Test
    void testIgnoreedCloseLead() throws Exception {
        mockRetrieveLead();
        var owLead = new OwLead().status(OwLeadStatus.RESOLVED);
        call(client.updateLead(LEAD_ID, owLead));
        Mockito.verify(soap, times(1)).retrieve(LeadSfService.RETRIEVE_FIELDS, "Lead", List.of(LEAD_ID));
        Mockito.verify(soap, times(0)).update(anyList());
    }

    private void mockQuery(String queryPart, SObject... records) throws Exception {
        when(soap.query(contains(queryPart))).thenReturn(new QueryResult().withRecords(List.of(records)));
    }

    private void mockRetrieveLead() throws Exception {
        mockRetrieveLead(false, false, LEAD_INITIAL_STATUS, null);
    }

    private void mockRetrieveLead(
            boolean isConverted,
            boolean isDeleted,
            String leadStatus,
            String accountId
    ) throws Exception {
        mockRetrieve(
                SObjectType.LEAD,
                new Lead()
                        .withId(LEAD_ID)
                        .withIsConverted(isConverted)
                        .withIsDeleted(isDeleted)
                        .withStatus(leadStatus)
                        .withConvertedAccountId(accountId)
        );
    }

    private void mockRetrieve(SObjectType type, SObject... records) throws Exception {
        when(soap.retrieve(anyString(), eq(type.getId()), anyList())).thenReturn(List.of(records));
    }

    @TestConfiguration
    public static class PropertiesConfig {
        @Bean
        @Primary
        public LeadConvertProperties leadConvertTestProperties() {
            var properties = new LeadConvertProperties();
            properties.setLeadInitialStatus(LEAD_INITIAL_STATUS);
            properties.setOpportunityInitialStatus(OPPORTUNITY_INITIAL_STATUS);
            return properties;
        }
    }
}
package ru.yandex.market.mbisfintegration.salesforce.query;

import java.time.Duration;
import java.util.List;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbisfintegration.AbstractFunctionalTest;
import ru.yandex.market.mbisfintegration.generated.sf.model.QueryResult;
import ru.yandex.market.mbisfintegration.generated.sf.model.Soap;
import ru.yandex.market.mbisfintegration.salesforce.SObjectType;
import ru.yandex.market.mbisfintegration.salesforce.SoapHolder;
import ru.yandex.market.mbisfintegration.salesforce.query.condition.Condition;
import ru.yandex.market.mbisfintegration.salesforce.query.impl.SfQueryServiceImpl;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SfQueryServiceTest extends AbstractFunctionalTest {

    @Autowired
    private SoapHolder soapHolder;

    private Soap soap;

    private SfQueryService service;

    @BeforeEach
    void setUp() throws Exception {
        soap = soapHolder.getSoap();
        when(soap.query(anyString())).thenReturn(new QueryResult().withRecords(List.of()));
        service = new SfQueryServiceImpl(soapHolder, Duration.ofMinutes(10));
    }

    @AfterEach
    void tearDown() {
        clearInvocations(soap);
    }

    @Test
    void testNoFields() {
        service.getQueryBuilder(SObjectType.ACCOUNT).find();
        verifyQuery("SELECT Id FROM Account");
    }

    @Test
    void testPredefinedFields() {
        service.getQueryBuilder(SObjectType.ACCOUNT).addAllFields().find();
        verifyQuery("SELECT FIELDS(ALL) FROM Account");
        service.getQueryBuilder(SObjectType.LEAD).addStandardFields().find();
        verifyQuery("SELECT FIELDS(STANDARD) FROM Lead");
        service.getQueryBuilder(SObjectType.CONTACT).addCustomFields().find();
        verifyQuery("SELECT FIELDS(CUSTOM) FROM Contact");
    }

    @Test
    void testComplexFields() {
        service.getQueryBuilder(SObjectType.ACCOUNT)
                .addStandardFields()
                .addFields("First", "Second")
                .addFields("Third")
                .find();
        verifyQuery("SELECT FIELDS(STANDARD),First,Second,Third FROM Account");
    }

    @Test
    void testWhere() {
        service.getQueryBuilder(SObjectType.ACCOUNT)
                .addStandardFields()
                .addConditions(Condition.eq("Id", 123))
                .addConditions(
                        Condition.or(
                                Condition.notEq("Name", "NameValue"),
                                Condition.like("City__c", "%CityValue%"),
                                Condition.and(
                                        Condition.in("Cases", List.of(1, 2)),
                                        Condition.notIn("Cases", List.of(3, 4))
                                )
                        )
                )
                .find();
        verifyQuery(
                "SELECT FIELDS(STANDARD) " +
                        "FROM Account " +
                        "WHERE Id = 123 AND " +
                        "(Name != 'NameValue' OR City__c LIKE '%CityValue%' OR (Cases IN (1,2) AND Cases NOT IN (3,4)))"
        );
    }

    @Test
    void testLimit() {
        service.getQueryBuilder(SObjectType.ACCOUNT)
                .addStandardFields()
                .addLimit(10)
                .find();
        verifyQuery("SELECT FIELDS(STANDARD) FROM Account LIMIT 10");

        //limit ignored on findOne
        service.getQueryBuilder(SObjectType.LEAD)
                .addStandardFields()
                .addLimit(10)
                .findOne();
        verifyQuery("SELECT FIELDS(STANDARD) FROM Lead LIMIT 1");
    }

    @Test
    void testUsesCache() {
        service.getQueryBuilder(SObjectType.ACCOUNT)
                .addStandardFields()
                .findOneCached();
        service.getQueryBuilder(SObjectType.ACCOUNT)
                .addStandardFields()
                .findOneCached();
        verifyQuery("SELECT FIELDS(STANDARD) FROM Account LIMIT 1");

        service.getQueryBuilder(SObjectType.LEAD)
                .addStandardFields()
                .findCached();
        service.getQueryBuilder(SObjectType.LEAD)
                .addStandardFields()
                .findCached();
        verifyQuery("SELECT FIELDS(STANDARD) FROM Lead");
    }

    @Test
    void testCacheNotUsed() throws Exception {
        service.getQueryBuilder(SObjectType.ACCOUNT)
                .addStandardFields()
                .findOne();
        service.getQueryBuilder(SObjectType.ACCOUNT)
                .addStandardFields()
                .findOne();
        verify(soap, times(2)).query("SELECT FIELDS(STANDARD) FROM Account LIMIT 1");

        service.getQueryBuilder(SObjectType.LEAD)
                .addStandardFields()
                .find();
        service.getQueryBuilder(SObjectType.LEAD)
                .addStandardFields()
                .find();
        verify(soap, times(2)).query("SELECT FIELDS(STANDARD) FROM Lead");
    }

    @SneakyThrows
    private void verifyQuery(String queryString) {
        verify(soap, times(1)).query(queryString);
    }
}
package ru.yandex.market.loyalty.core.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.accounting.OperationContextDao;
import ru.yandex.market.loyalty.core.model.OperationContext;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.uidOperationContext;
import static ru.yandex.market.loyalty.core.utils.OperationContextFactory.yandexUidOperationContext;

/**
 * @author dinyat
 * 01/06/2017
 */
public class OperationContextDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private OperationContextDao operationContextDao;

    @Test
    public void testSave() {
        OperationContext operationContext = uidOperationContext();

        OperationContext saved = operationContextDao.get(operationContextDao.save(operationContext));
        assertThat(operationContext,
                samePropertyValuesAs(saved, "creationTime", "id"));
    }

    @Test
    public void testGetUidOperationContext() {
        long result = operationContextDao.save(uidOperationContext());

        assertThat(uidOperationContext(),
                samePropertyValuesAs(operationContextDao.get(result), "creationTime", "id"));
    }

    @Test
    public void testGetYandexUidOperationContext() {
        long result = operationContextDao.save(yandexUidOperationContext());

        assertThat(yandexUidOperationContext(),
                samePropertyValuesAs(operationContextDao.get(result), "creationTime", "id"));
    }

    @Test
    public void testGetWithNullFields() {
        OperationContext operationContext = uidOperationContext();
        operationContext.setUid(null);
        operationContext.setRegionId(null);

        long result = operationContextDao.save(operationContext);

        assertThat(operationContext,
                samePropertyValuesAs(operationContextDao.get(result), "creationTime", "id"));
    }

    @Test
    public void testGetPersonalInformation() {
        OperationContext operationContext = OperationContextFactory
                .withUidBuilder(123)
                .buildOperationContext();

        assertThat(operationContext,
                allOf(hasProperty("personalEmailId", is(notNullValue())),
                        hasProperty("personalFullNameId", is(notNullValue())),
                        hasProperty("personalPhoneId", is(notNullValue()))
                        ));
        long result = operationContextDao.save(operationContext);

        assertThat(operationContext,
                samePropertyValuesAs(operationContextDao.get(result), "creationTime", "id"));
    }

    @Test
    public void testGetIsB2BUser() {
        OperationContext operationContext = OperationContextFactory
                .withUidBuilder(123)
                .buildOperationContext();
        operationContext.setIsB2B(true);

        assertThat(operationContext, hasProperty("isB2B", is(notNullValue())));
        long result = operationContextDao.save(operationContext);

        assertThat(operationContext,
                samePropertyValuesAs(operationContextDao.get(result), "creationTime", "id"));
    }
}

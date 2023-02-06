package ru.yandex.market.notification.safe.dao.impl;

import org.junit.Test;

import ru.yandex.market.notification.safe.dao.impl.AbstractDao.CommonQueryConstants;
import ru.yandex.market.notification.test.model.AbstractModelTest;
import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link AbstractDao}.
 *
 * @author Vladislav Bauer
 */
public class AbstractDaoTest extends AbstractModelTest {

    private static final String CONSTANT = "test";


    @Test
    public void testCommonQueryConstantsContract() {
        ClassUtils.checkConstructor(CommonQueryConstants.class);
    }

    @Test
    public void testCommonQueryConstantsMethods() {
        assertThat(CommonQueryConstants.xmlserialize(CONSTANT), notNullValue());
        assertThat(CommonQueryConstants.xmltype(CONSTANT), notNullValue());
        assertThat(CommonQueryConstants.xmltypeParam(CONSTANT), notNullValue());
    }

}

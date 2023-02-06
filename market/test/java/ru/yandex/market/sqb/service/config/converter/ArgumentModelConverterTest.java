package ru.yandex.market.sqb.service.config.converter;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.model.conf.ArgumentModel;
import ru.yandex.market.sqb.model.vo.ArgumentVO;
import ru.yandex.market.sqb.service.config.converter.common.AbstractNameModelConverterTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit-тесты для {@link ArgumentModelConverter}.
 *
 * @author Vladislav Bauer
 */
class ArgumentModelConverterTest extends AbstractNameModelConverterTest<ArgumentVO, ArgumentModel> {

    private static final String TEST_VALUE = "test argument value";
    private static final String TEST_NAME_POSITIVE = "TEST_ARGUMENT_NAME";


    @Test
    void testSinglePositive() {
        final ArgumentModelConverter converter = createConverter();
        final ArgumentVO argumentVO = createObject();
        final ArgumentModel argumentModel = converter.convert(argumentVO);

        assertThat(argumentModel.getName(), equalTo(argumentVO.getName()));
        assertThat(argumentModel.getValue(), equalTo(argumentVO.getValue()));
    }


    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    protected Collection<String> getIllegalNames() {
        return CollectionUtils.union(super.getIllegalNames(), ArgumentModelConverter.FORBIDDEN_NAMES);
    }

    @Nonnull
    @Override
    protected ArgumentVO createObject() {
        final ArgumentVO argumentVO = new ArgumentVO();
        argumentVO.setName(TEST_NAME_POSITIVE);
        argumentVO.setValue(TEST_VALUE);
        return argumentVO;
    }

    @Nonnull
    @Override
    protected ArgumentModelConverter createConverter() {
        return new ArgumentModelConverter();
    }

}

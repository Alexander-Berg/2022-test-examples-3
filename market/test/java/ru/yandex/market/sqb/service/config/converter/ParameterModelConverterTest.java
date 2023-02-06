package ru.yandex.market.sqb.service.config.converter;

import java.util.Collections;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.model.conf.ParameterModel;
import ru.yandex.market.sqb.model.vo.ParameterVO;
import ru.yandex.market.sqb.service.config.converter.common.AbstractNameModelConverterTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit-тесты для {@link ParameterModelConverter}.
 *
 * @author Vladislav Bauer
 */
class ParameterModelConverterTest extends AbstractNameModelConverterTest<ParameterVO, ParameterModel> {

    private static final String TEST_NAME = "TEST_PARAM_NAME";
    private static final String TEST_VALUE = "test param value";
    private static final String TEST_DESCRIPTION = "Test description";
    private static final String TEST_CONDITION = "test condition";
    private static final String TEST_SQL = "test sql";


    @Test
    void testSinglePositive() {
        final ParameterModelConverter converter = createConverter();
        final ParameterVO parameterVO = createObject();
        final ParameterModel parameterModel = converter.convert(parameterVO);

        assertThat(parameterModel.getName(), equalTo(parameterVO.getName()));
        assertThat(parameterModel.getValue(), equalTo(parameterVO.getValueAttr()));
        assertThat(parameterModel.getDescription(), equalTo(parameterVO.getDescriptionAttr()));
        assertThat(parameterModel.getCondition().orElse(null), equalTo(parameterVO.getCondition()));
        assertThat(parameterModel.getSql().orElse(null), equalTo(parameterVO.getSql()));
    }


    @Nonnull
    @Override
    protected ParameterVO createObject() {
        final ParameterVO parameterVO = new ParameterVO();
        parameterVO.setName(TEST_NAME);
        parameterVO.setValueAttr(TEST_VALUE);
        parameterVO.setDescriptionAttr(TEST_DESCRIPTION);
        parameterVO.setCondition(TEST_CONDITION);
        parameterVO.setSql(TEST_SQL);
        parameterVO.setArguments(Collections.emptyList());
        return parameterVO;
    }

    @Nonnull
    @Override
    protected ParameterModelConverter createConverter() {
        return new ParameterModelConverter(new ArgumentModelConverter(), Collections.emptyList());
    }

}

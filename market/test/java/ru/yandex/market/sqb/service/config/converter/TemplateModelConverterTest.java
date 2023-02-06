package ru.yandex.market.sqb.service.config.converter;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.model.conf.TemplateModel;
import ru.yandex.market.sqb.model.vo.TemplateVO;
import ru.yandex.market.sqb.service.config.converter.common.AbstractNameModelConverterTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit-тесты для {@link TemplateModelConverter}.
 *
 * @author Vladislav Bauer
 */
class TemplateModelConverterTest extends AbstractNameModelConverterTest<TemplateVO, TemplateModel> {

    private static final String TEST_NAME = "TEST_TEMPLATE_NAME";
    private static final String TEST_SQL = "test sql query";
    private static final String TEST_CONDITION = "test condition";

    @Test
    void testSinglePositive() {
        final TemplateModelConverter converter = createConverter();
        final TemplateVO templateVO = createObject();
        final TemplateModel templateModel = converter.convert(templateVO);

        assertThat(templateModel.getName(), equalTo(templateVO.getName()));
        assertThat(templateModel.getSql(), equalTo(templateVO.getSql()));
        assertThat(templateModel.getCondition(), equalTo(templateVO.getCondition()));
    }


    @Nonnull
    @Override
    protected TemplateVO createObject() {
        final TemplateVO templateVO = new TemplateVO();
        templateVO.setName(TEST_NAME);
        templateVO.setSql(TEST_SQL);
        templateVO.setCondition(TEST_CONDITION);
        return templateVO;
    }

    @Nonnull
    @Override
    protected TemplateModelConverter createConverter() {
        return new TemplateModelConverter();
    }

}

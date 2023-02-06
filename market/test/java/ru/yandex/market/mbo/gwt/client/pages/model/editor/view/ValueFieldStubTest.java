package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ParamMeta;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValueField;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;

/**
 * Тесты проверяют, что {@link ValueFieldStub} будут работать также как и настоящая реализация.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ValueFieldStubTest {

    private CategoryParam categoryParam;
    private Option option1;
    private Option option2;
    private Option option3;
    private ParamMeta paramMeta;
    private ValueField<Option> valueField;

    @Before
    public void setUp() throws Exception {
        categoryParam = CategoryParamBuilder.newBuilder(1, "test")
            .setType(Param.Type.ENUM)
            .addOption(new OptionImpl(1, "test1"))
            .addOption(new OptionImpl(2, "test2"))
            .addOption(new OptionImpl(3, "test3"))
            .build();
        option1 = categoryParam.getOption(0);
        option2 = categoryParam.getOption(1);
        option3 = categoryParam.getOption(2);

        paramMeta = new ParamMeta();
        paramMeta.setType(categoryParam.getType());
        paramMeta.setParamId(categoryParam.getId());
        paramMeta.setXslName(categoryParam.getXslName());

        valueField = new ValueFieldStub<>(paramMeta);
    }

    @Test
    public void testSetValueWithoutSettingValueDomainWontEffect() {
        valueField.setValue(option1, false);

        Assertions.assertThat(valueField.getValue()).isNull();
    }

    @Test
    public void testSetValueAfterSettigValueDomainWillBeSuccessful() {
        valueField.setValueDomain(categoryParam.getOptions());
        valueField.setValue(option1, false);

        Assertions.assertThat(valueField.getValue()).isEqualTo(option1);
    }

    @Test
    public void testSetValueNotInValueDomainWontEffect() {
        valueField.setValueDomain(categoryParam.getOptions().subList(1, 2));
        valueField.setValue(option1, false);

        Assertions.assertThat(valueField.getValue()).isNull();
    }

    @Test
    public void testChangeValueDomainWillAlsoChangeValue() {
        valueField.setValueDomain(categoryParam.getOptions());
        valueField.setValue(option1, false);
        Assertions.assertThat(valueField.getValue()).isEqualTo(option1);

        valueField.setValueDomain(categoryParam.getOptions().subList(1, 2));
        Assertions.assertThat(valueField.getValue()).isNull();
    }
}

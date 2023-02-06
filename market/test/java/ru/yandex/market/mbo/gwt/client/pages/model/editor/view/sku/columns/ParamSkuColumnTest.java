package ru.yandex.market.mbo.gwt.client.pages.model.editor.view.sku.columns;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ValueFieldStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.ViewFactoryStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.valuewidget.interfaces.ValuesWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class ParamSkuColumnTest {

    @Spy
    private ViewFactoryStub viewFactory;
    private CommonModel sku;
    private ValuesWidget<Object> widget;
    private CategoryParam param;

    @Before
    public void setup() {
        CategoryParamBuilder colorBuilder = CategoryParamBuilder.newBuilder()
            .setCategoryHid(13L)
            .setId(100500L)
            .setType(Param.Type.ENUM);

        for (int i = 0; i < 100; i++) {
            colorBuilder.addOption(new OptionImpl(i));
        }

        sku = CommonModelBuilder.newBuilder()
            .id(9001L)
            .category(100500L)
            .currentType(CommonModel.Source.SKU)
            .getModel();

        param = colorBuilder.build();
        widget = viewFactory.createValuesWidget(param, sku, null);
        doReturn(widget).when(viewFactory).createValuesWidget(any(), any(), any());
    }

    @Test
    public void testCreateCellWidget() {
        ParamSkuColumn<Option> column = new ParamSkuColumn<>(param, viewFactory, null);
        //Метод упадёт, т.к. попытается кастануть стабу к реальному гвт-виджету. Тем не менее, все необходимые
        //действия он над стабами успеет проделать, что и проверим.
        assertThatThrownBy(() -> column.createCellWidget(sku)).isInstanceOf(ClassCastException.class);
        assertFalse(widget.getValueWidgets().isEmpty());
        widget.getValueWidgets().forEach(w -> {
            ValueFieldStub vfield = (ValueFieldStub) w.getValueField();
            assertEquals(1, vfield.getValueDomainCallsCount());
        });
    }
}

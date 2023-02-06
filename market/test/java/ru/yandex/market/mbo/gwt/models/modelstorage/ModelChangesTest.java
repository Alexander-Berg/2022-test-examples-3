package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mbo.utils.MboAssertions.assertThat;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ModelChangesTest {

    private static final CommonModel MODEL = CommonModelBuilder.newBuilder().id(555).getModel();
    private static final CommonModel MODEL_TO_DELETE = CommonModelBuilder.newBuilder().id(555).deleted(true).getModel();


    public CommonModelBuilder<CommonModel> model() {
        return ParametersBuilder
            .startParameters(p -> CommonModelBuilder.builder(Function.identity()).parameters(p))

            .startParameter()
            .id(1).xslAndName("string").type(Param.Type.STRING)
            .endParameter()

            .startParameter()
            .id(2).xslAndName("enum").type(Param.Type.ENUM)
            .endParameter()
            .endParameters();
    }

    @Test
    public void validateCreateOperation() {
        ModelChanges modelChanges = new ModelChanges(null, MODEL);
        assertEquals(ModelChanges.Operation.CREATE, modelChanges.getOperation());
    }

    @Test
    public void validateUpdateOperation() {
        ModelChanges modelChanges = new ModelChanges(MODEL, MODEL);
        assertEquals(ModelChanges.Operation.UPDATE, modelChanges.getOperation());
    }

    @Test
    public void validateDeleteOperation() {
        ModelChanges modelChanges = new ModelChanges(MODEL, MODEL_TO_DELETE);
        assertEquals(ModelChanges.Operation.DELETE, modelChanges.getOperation());
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateIllegalState() {
        new ModelChanges(null, null);
    }

    @Test
    public void multivalueStringChange() {
        CommonModel before = model().param("string").setString("cat", "dog").endModel();
        CommonModel after = model().param("string").setString("cat", "dog", "bird").endModel();
        ModelChanges modelChanges = new ModelChanges(before, after);

        assertThat(modelChanges)
            .parameterChanged("string", pv -> pv.values("cat", "dog", "bird"))
            .noMoreChanges();
    }

    @Test
    public void multivalueStringRemove() {
        CommonModel before = model().param("string").setString("cat", "dog").endModel();
        CommonModel after = model().endModel();
        ModelChanges modelChanges = new ModelChanges(before, after);

        assertThat(modelChanges)
            .parameterChanged("string", pv -> pv.isEmpty())
            .noMoreChanges();
    }

    @Test
    public void multivalueAddEnumChange() {
        CommonModel before = model()
            .param("enum").setOption(1)
            .param("enum").setOption(2)
            .endModel();

        CommonModel after = model()
            .param("enum").setOption(1)
            .param("enum").setOption(2)
            .param("enum").setOption(3)
            .endModel();

        ModelChanges modelChanges = new ModelChanges(before, after);

        assertThat(modelChanges)
            .parameterChanged("enum", pv -> pv.values(1L, 2L, 3L))
            .noMoreChanges();
    }

    @Test
    public void multivalueRemoveSingleEnum() {
        CommonModel before = model()
            .param("enum").setOption(1)
            .param("enum").setOption(2)
            .endModel();

        CommonModel after = model()
            .param("enum").setOption(2)
            .endModel();

        ModelChanges modelChanges = new ModelChanges(before, after);

        assertThat(modelChanges)
            .parameterChanged("enum", pv -> pv.values(2L))
            .noMoreChanges();
    }

    @Test
    public void multivalueRemoveEnum() {
        CommonModel before = model()
            .param("enum").setOption(1)
            .param("enum").setOption(2)
            .endModel();

        CommonModel after = model().endModel();

        ModelChanges modelChanges = new ModelChanges(before, after);

        assertThat(modelChanges)
            .parameterChanged("enum", pv -> pv.isEmpty())
            .noMoreChanges();
    }

    @Test
    public void validateOperationWithCustomType() {
        ModelChanges modelChanges1 = new ModelChanges(null, MODEL, ModelChanges.Operation.CREATE);
        assertEquals(ModelChanges.Operation.CREATE, modelChanges1.getOperation());

        ModelChanges modelChanges2 = new ModelChanges(null, MODEL, ModelChanges.Operation.UPDATE);
        assertEquals(ModelChanges.Operation.UPDATE, modelChanges2.getOperation());

        ModelChanges modelChanges3 = new ModelChanges(null, MODEL, ModelChanges.Operation.DELETE);
        assertEquals(ModelChanges.Operation.DELETE, modelChanges3.getOperation());
    }

}

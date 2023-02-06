package ru.yandex.market.mbo.db.modelstorage.generalization;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class GeneralizationGroupTest {

    @Test
    public void testParentModelIdIsValid() throws Exception {
        GeneralizationGroup generalizationGroup = new GeneralizationGroup(1);
        assertEquals(1, generalizationGroup.getParentModelId());
    }

    @Test
    public void testSetOfParentModel() throws Exception {
        CommonModel parentModel = CommonModelBuilder.newBuilder(1, 1, 1).getModel();

        GeneralizationGroup generalizationGroup = new GeneralizationGroup(1);
        generalizationGroup.setParentModel(parentModel);

        assertEquals(parentModel, generalizationGroup.getParentModel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSetOfParentModel() throws Exception {
        CommonModel parentModel = CommonModelBuilder.newBuilder(2, 2, 2).getModel();

        GeneralizationGroup generalizationGroup = new GeneralizationGroup(1);
        generalizationGroup.setParentModel(parentModel);
    }

    @Test
    public void testAddOfModifications() throws Exception {
        CommonModel modification1 = CommonModelBuilder.newBuilder(2, 2, 2).parentModelId(1).getModel();
        CommonModel modification2 = CommonModelBuilder.newBuilder(3, 2, 2).parentModelId(1).getModel();
        CommonModel modification3 = CommonModelBuilder.newBuilder(4, 2, 2).parentModelId(1).getModel();

        GeneralizationGroup generalizationGroup = new GeneralizationGroup(1);
        generalizationGroup.addModification(modification1);
        generalizationGroup.addModification(modification2);
        generalizationGroup.addModification(modification3);

        assertEquals(3, generalizationGroup.getModifications().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalAddOfModification() throws Exception {
        CommonModel modification1 = CommonModelBuilder.newBuilder(2, 2, 2).parentModelId(1).getModel();

        GeneralizationGroup generalizationGroup = new GeneralizationGroup(3);
        generalizationGroup.addModification(modification1);
    }

    @Test
    public void testDoubleEnteringOfModification() throws Exception {
        CommonModel modification1 = CommonModelBuilder.newBuilder(2, 2, 2).parentModelId(1).getModel();
        CommonModel modification2 = CommonModelBuilder.newBuilder(2, 2, 2).parentModelId(1).getModel();

        GeneralizationGroup generalizationGroup = new GeneralizationGroup(1);
        generalizationGroup.addModification(modification1);
        try {
            generalizationGroup.addModification(modification2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        assertEquals(1, generalizationGroup.getModifications().size());
    }

    @Test
    public void testAliveModifications() throws Exception {
        CommonModel modification1 = CommonModelBuilder.newBuilder(2, 2, 2).parentModelId(1).getModel();
        CommonModel modification2 = CommonModelBuilder.newBuilder(3, 2, 2).parentModelId(1)
            .deleted(true).getModel();

        GeneralizationGroup generalizationGroup = new GeneralizationGroup(1);
        generalizationGroup.addModification(modification1);
        generalizationGroup.addModification(modification2);

        assertEquals(2, generalizationGroup.getModifications().size());
        assertEquals(1, generalizationGroup.getAliveModifications().size());
    }
}

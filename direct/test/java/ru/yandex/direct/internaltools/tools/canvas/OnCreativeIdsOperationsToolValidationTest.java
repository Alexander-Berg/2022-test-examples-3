package ru.yandex.direct.internaltools.tools.canvas;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.canvas.model.OnCreativeIdsOperationParameter;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class OnCreativeIdsOperationsToolValidationTest {
    @Autowired
    OnCreativeIdsOperationsTool onCreativeIdsOperationsTool;

    @Test
    public void validateEmptyCreativeIds() {
        OnCreativeIdsOperationParameter params = new OnCreativeIdsOperationParameter();
        params.setOperationName(OnCreativeIdsOperationParameter.OperationName.REBUILD);
        params.setCreativeIds("");
        ValidationResult<OnCreativeIdsOperationParameter, Defect> vr =
                onCreativeIdsOperationsTool.validate(params);

        assertTrue(vr.hasAnyErrors());
    }

    @Test
    public void validateDecimalCreativeId() {
        OnCreativeIdsOperationParameter params = new OnCreativeIdsOperationParameter();
        params.setOperationName(OnCreativeIdsOperationParameter.OperationName.REBUILD);
        params.setCreativeIds("0.0");
        ValidationResult<OnCreativeIdsOperationParameter, Defect> vr =
                onCreativeIdsOperationsTool.validate(params);

        assertTrue(vr.hasAnyErrors());
    }

    @Test
    public void validateStringCreativeId() {
        OnCreativeIdsOperationParameter params = new OnCreativeIdsOperationParameter();
        params.setOperationName(OnCreativeIdsOperationParameter.OperationName.REBUILD);
        params.setCreativeIds("adf");
        ValidationResult<OnCreativeIdsOperationParameter, Defect> vr =
                onCreativeIdsOperationsTool.validate(params);

        assertTrue(vr.hasAnyErrors());
    }

    @Test
    public void validationSuccess() {
        OnCreativeIdsOperationParameter params = new OnCreativeIdsOperationParameter();
        params.setOperationName(OnCreativeIdsOperationParameter.OperationName.REBUILD);
        params.setCreativeIds("123, 355, 7318, 99999");
        ValidationResult<OnCreativeIdsOperationParameter, Defect> vr =
                onCreativeIdsOperationsTool.validate(params);

        assertFalse(vr.hasAnyErrors());
    }
}

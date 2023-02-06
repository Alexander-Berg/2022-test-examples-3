package ru.yandex.market.mbo.gwt.models.gurulight;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.exceptions.ValidateMessages;

import java.util.Collections;

public class AffectedGlRulesByParamErrorTest {

    @Test
    public void modifyTextTest() {
        Assert.assertEquals("test:", new AffectedGlRulesByParamError(ValidateMessages.PARAM_UNPUBLISH, "test:", -1,
            Collections.singletonList(-1L), true).getText());
        Assert.assertEquals("test (не опубликованы):", new AffectedGlRulesByParamError(ValidateMessages.PARAM_UNPUBLISH,
            "test:", -1, Collections.singletonList(-1L), false).getText());
    }

}

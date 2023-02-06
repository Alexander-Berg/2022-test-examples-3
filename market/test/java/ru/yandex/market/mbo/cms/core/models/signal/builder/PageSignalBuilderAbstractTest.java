package ru.yandex.market.mbo.cms.core.models.signal.builder;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.signal.model.Signal;
import ru.yandex.market.mbo.cms.signal.model.SignalType;
import ru.yandex.market.mbo.cms.signal.model.SignalValidator;

public abstract class PageSignalBuilderAbstractTest {

    protected abstract Map<String, Object> getExpectedData();

    protected abstract Signal getSignalForTest();

    @Test
    public void testBuild() {
        Signal signal = getSignalForTest();
        SignalType signalType = signal.getType();

        SignalValidator.validateAndThrow(signal);
        signalType.getFieldDescriptions().forEach(signalFieldDescription -> {
            String fieldName = signalFieldDescription.getFieldName();
            Assert.assertThat(
                signalType.toString() + "->" + fieldName + " is equals to " + getExpectedData().get(fieldName),
                signalFieldDescription.get(signal),
                Matchers.equalTo(getExpectedData().get(fieldName)));
        });
    }
}

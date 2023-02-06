package ru.yandex.chemodan.app.psbilling.core.features;

import lombok.Getter;
import lombok.Setter;
import org.junit.Test;

import ru.yandex.chemodan.app.psbilling.core.config.featureflags.AbstractFeature;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class FeatureFlagsTest {
    @Test
    public void strictOn() {
        TestFeature feature = new TestFeature();
        feature.setValue("true");
        Assert.isTrue(feature.isEnabled());

        feature.setValue("True");
        Assert.isTrue(feature.isEnabled());

        feature.setValue(" True ");
        Assert.isTrue(feature.isEnabled());
    }

    @Test
    public void isEnabledForUid() {
        TestFeature feature = new TestFeature();
        feature.setValue("True");
        Assert.isTrue(feature.isEnabledForUid(PassportUid.MAX_VALUE));

        feature.setValue("false");
        Assert.isFalse(feature.isEnabledForUid(PassportUid.MAX_VALUE));

        feature.setValue("123, 456");
        Assert.isFalse(feature.isEnabledForUid(PassportUid.MAX_VALUE));

        feature.setValue("123, 456, " + PassportUid.MAX_VALUE);
        Assert.isTrue(feature.isEnabledForUid(PassportUid.MAX_VALUE));

        feature.setValue(" 123, 456,, " + PassportUid.MAX_VALUE + "  ");
        Assert.isTrue(feature.isEnabledForUid(PassportUid.MAX_VALUE));
    }


    @Test
    public void invalidValue() {
        TestFeature feature = new TestFeature();
        feature.setValue("tru");

        Assert.isFalse(feature.isEnabled());
        Assert.isFalse(feature.isEnabledForUid(PassportUid.MAX_VALUE));
    }

    @Getter
    @Setter
    private class TestFeature extends AbstractFeature {
        private String value;
    }
}

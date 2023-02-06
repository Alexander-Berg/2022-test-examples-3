package ru.yandex.market.api.abtest;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.common.client.ClientVersionInfoResolver;
import ru.yandex.market.api.common.client.KnownMobileClientVersionInfo;
import ru.yandex.market.api.controller.v2.startup.ActiveExperiment;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.common.DeviceType;
import ru.yandex.market.api.internal.common.Platform;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

import java.util.Collections;

@WithContext
public class AbTestAppVersionConditionVerifierTest extends UnitTestBase {

    private static final String TEST_ID = "123";
    private static final String BUCKET_ID = "456";
    private final AbTestAppVersionConditionVerifier versionConditionVerifier = new AbTestAppVersionConditionVerifier();

    @Test
    public void android() {
        setAndroid("1.0.0");
        assertSatisfied("android && version == '1.0.0'");
    }

    @Test
    public void androidWithDifferentVersion() {
        setAndroid("2.0.0");
        assertNotSatisfied("android && version == '1.0.0'");
    }

    @Test
    public void compositeCondition() {
        setAndroid("1.0.0");
        assertSatisfied("(android && version == '1.0.0') || (iphone && version == '2.0.0')");

        setIPhone("2.0.0");
        assertSatisfied("(android && version == '1.0.0') || (iphone && version == '2.0.0')");
    }

    @Test
    public void emptyCondition_waitSatisfied() {
        setClient(Platform.ANDROID, DeviceType.SMARTPHONE, "1.0.0");
        assertSatisfied("");
    }

    @Test
    public void errorMessageContainsUndefinedVariable() {
        setAndroid("1.0.0");
        String errorMessage = assertError("ios && version='2.0.0'");
        Assert.assertTrue(errorMessage.contains("ios"));
    }

    @Test
    public void invalidJavascript() {
        setAndroid("1.0.0");
        assertError("trash");
    }

    @Test
    public void ipad() {
        setIPad("2.0.0");
        assertSatisfied("ipad && version <= '2.0.0'");
    }

    @Test
    public void iphone() {
        setIPhone("2.0.0");
        assertSatisfied("iphone && version >= '1.0.0'");
    }

    @Test
    public void iphoneTablet() {
        setIPhoneTablet("2.0.0");
        assertSatisfied("iphone && version >= '1.0.0'");
    }

    @Test
    public void nullCondition_waitSatisfied() {
        setClient(Platform.ANDROID, DeviceType.SMARTPHONE, "1.0.0");
        assertSatisfied(null);
    }

    @Test
    public void rawVersion() {
        String condition = "android && v >= '2.71'";

        setAndroid("2.7");
        assertNotSatisfied(condition);

        setAndroid("2.71");
        assertSatisfied(condition);

        setAndroid("2.72");
        assertSatisfied(condition);
    }

    @Test
    public void tablet() {
        setAndroidTablet("1.0.0");
        assertSatisfied("tablet && version == '1.0.0'");
        setIPad("1.0.0");
        assertSatisfied("tablet && version == '1.0.0'");
    }

    private String assertError(String condition) {
        Result<Boolean, String> result = versionConditionVerifier.isSatisfied(getExperiment(condition));
        Assert.assertFalse(result.isOk());
        return result.getError();
    }

    private void assertNotSatisfied(String condition) {
        Result<Boolean, String> result = versionConditionVerifier.isSatisfied(getExperiment(condition));
        Assert.assertTrue(result.isOk());
        Assert.assertFalse(result.getValue());
    }

    private void assertSatisfied(String condition) {
        Result<Boolean, String> result = versionConditionVerifier.isSatisfied(getExperiment(condition));
        Assert.assertTrue(result.isOk());
        Assert.assertTrue(result.getValue());
    }

    private ActiveExperiment getExperiment(String condition) {
        return new ActiveExperiment(TEST_ID, BUCKET_ID, "ALIAS", "HANDLER", condition, Collections.emptyList(), false);
    }

    private void setAndroid(String version) {
        setClient(Platform.ANDROID, DeviceType.SMARTPHONE, version);
    }

    private void setAndroidTablet(String version) {
        setClient(Platform.ANDROID, DeviceType.TABLET, version);
    }

    private void setClient(Platform platform, DeviceType deviceType, String version) {
        ContextHolder.update(x ->
            x.setClientVersionInfo(new KnownMobileClientVersionInfo(platform,
                deviceType,
                ClientVersionInfoResolver.parse(version),
                version,
                Collections.emptyList())));
    }


    private void setIPad(String version) {
        setClient(Platform.IOS, DeviceType.IPAD, version);
    }

    private void setIPhone(String version) {
        setClient(Platform.IOS, DeviceType.SMARTPHONE, version);
    }

    private void setIPhoneTablet(String version) {
        setClient(Platform.IOS, DeviceType.TABLET, version);
    }
}

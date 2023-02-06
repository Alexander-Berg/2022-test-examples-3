package ru.yandex.chemodan.uploader.installer;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.util.test.JsonTestUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class InstallerModifierTest {

    @Test
    public void patchInstaller() throws Exception {
        byte[] data = prepareInstallerData();
        InstallerModifier modifier = new InstallerModifier();

        InstallerModifierParams params = InstallerModifierParams
                .build(Option.of(""), "", Option.of("yadisk://test.download"))
                .setKey("secret-key");

        modifier.patchInstaller(data, params);

        checkResult(data);
    }

    public static byte[] prepareInstallerData() {
        byte[] data = new byte[
                InstallerModifier.ZERO_COUNT +
                InstallerModifier.START_MARKER.length +
                InstallerModifier.END_MARKER.length];

        System.arraycopy(
                InstallerModifier.START_MARKER, 0,
                data, 0, InstallerModifier.START_MARKER.length);
        System.arraycopy(
                InstallerModifier.END_MARKER, 0,
                data, InstallerModifier.ZERO_COUNT + InstallerModifier.START_MARKER.length,
                InstallerModifier.END_MARKER.length);
        return data;
    }

    public static void checkResult(byte[] data) {
        byte[] jsonKey = extractJsonKey(data);

        Map<String, Object> resultParams = JsonTestUtils.parseJsonToMap(jsonKey);

        Assert.equals("secret-key", resultParams.get("key"));
        Assert.equals("yadisk://test.download", resultParams.get("open_url_after_install"));
    }

    public static byte[] extractJsonKey(byte[] data) {
        return Arrays.copyOfRange(data,
                    InstallerModifier.START_MARKER.length,
                    InstallerModifier.START_MARKER.length + InstallerModifier.ZERO_COUNT);
    }
}

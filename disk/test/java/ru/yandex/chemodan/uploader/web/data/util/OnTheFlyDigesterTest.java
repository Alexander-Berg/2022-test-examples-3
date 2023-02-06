package ru.yandex.chemodan.uploader.web.data.util;

import java.io.ByteArrayOutputStream;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.chemodan.uploader.registry.record.Digests;
import ru.yandex.misc.bytes.ByteArrayByteSequence;
import ru.yandex.misc.io.ByteArrayInputStreamSource;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.IoUtils;
import ru.yandex.misc.io.OutputStreamOutputStreamSource;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestDirRule;
import ru.yandex.misc.webdav.WebDavDigest;

/**
 * @author bursy
 */
public class OnTheFlyDigesterTest {

    @Rule
    public final TestDirRule testDir = new TestDirRule();

    @Test
    public void testDigests() {
        File2 file = testDir.testDir.child("webdav-digests");
        ByteArrayInputStreamSource source = new ByteArrayInputStreamSource(generateTestBytes());

        byte[] webDavDirectDigest = getWebDavDigestDirectly(source);
        Digests digests = getOnTheFlyDigests(file, source);

        Assert.equals("accf07fe601c77a0cd249c252921936d", digests.md5);
        Assert.equals("a06acc93e84c7fbfd9601d993184b73716db45d57e6358fddb1e33279588ee38", digests.sha256);

        Assert.assertArrayEquals(webDavDirectDigest, digests.webDavDigestFile.readBytes());
    }

    private byte[] getWebDavDigestDirectly(final InputStreamSource source) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WebDavDigest.digest(source, new OutputStreamOutputStreamSource(out));
        return out.toByteArray();
    }

    private Digests getOnTheFlyDigests(File2 file, InputStreamSource source) {
        OnTheFlyDigester digester = new OnTheFlyDigester(file, source.lengthO().get());

        return IoUtils.iterablateStream(source, (bytes) -> {
            for (ByteArrayByteSequence b : bytes) {
                digester.update(b);
            }

            return digester.completeAndGetDigests();
        });
    }

    private byte[] generateTestBytes() {
        // 2^20 bytes, more than enough to produce multiple chunks
        String str = "some digest data";
        for (int i = 0; i < 16; i++) {
            str += str;
        }
        return str.getBytes();
    }
}

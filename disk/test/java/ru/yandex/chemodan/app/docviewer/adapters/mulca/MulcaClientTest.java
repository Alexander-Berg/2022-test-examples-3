package ru.yandex.chemodan.app.docviewer.adapters.mulca;

import java.io.ByteArrayOutputStream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.adapters.AdaptersTestBase;
import ru.yandex.inside.mulca.MulcaClient;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.io.InputStreamSourceUtils;
import ru.yandex.misc.io.OutputStreamOutputStreamSource;
import ru.yandex.misc.test.Assert;

public class MulcaClientTest extends AdaptersTestBase {
    private static final String TEST_MULCA_IDENTIFIER = "tmp";

    @Autowired
    private MulcaClient mulcaClient;

    @Test
    public void uploadAndDownload() {
        byte[] data = "hello, mulco".getBytes();
        MulcaId id = mulcaClient.upload(InputStreamSourceUtils.bytes(data), TEST_MULCA_IDENTIFIER);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mulcaClient.download(id).readTo(new OutputStreamOutputStreamSource(out));
        Assert.assertArrayEquals(data, out.toByteArray());

        mulcaClient.delete(id);
    }
}

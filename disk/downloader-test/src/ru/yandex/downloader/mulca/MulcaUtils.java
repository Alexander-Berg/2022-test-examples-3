package ru.yandex.downloader.mulca;

import ru.yandex.inside.mulca.MulcaClient;
import ru.yandex.inside.mulca.MulcaTestManager;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClientUtils;

/**
 * @author akirakozov
 */
public class MulcaUtils {
    private static final String MULCA_GATE_URL = "http://storagetest.mail.yandex.net:10010/gate";

    public static MulcaTestManager createMulcaTestManager() {
        MulcaClient mulcaClient = new MulcaClient(
                        ApacheHttpClientUtils.singleConnectionClient(new Timeout(300, 300)),
                        MULCA_GATE_URL);
        return new MulcaTestManager(mulcaClient);
    }
}

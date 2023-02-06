package ru.yandex.chemodan.test;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.function.Function;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.inside.mulca.MulcaClient;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.misc.io.InputStreamSource;

/**
 * @author akirakozov
 */
public class MulcaTestManager {
    @Autowired
    private MulcaClient mulcaClient;

    public MulcaTestManager(MulcaClient mulcaClient) {
        this.mulcaClient = mulcaClient;
    }

    public void withUploadedToMulcaFile(InputStreamSource in, boolean asMessage, Function1V<MulcaId> handler) {
        MulcaId mulcaId = mulcaClient.upload(in, "tmp", asMessage);
        try {
            handler.apply(mulcaId);
        } finally {
            mulcaClient.delete(mulcaId);
        }
    }

    public <R> R withUploadedToMulcaFile(InputStreamSource in, boolean asMessage, Function<MulcaId, R> handler) {
        MulcaId mulcaId = mulcaClient.upload(in, "tmp", asMessage);
        try {
            return handler.apply(mulcaId);
        } finally {
            mulcaClient.delete(mulcaId);
        }
    }
}

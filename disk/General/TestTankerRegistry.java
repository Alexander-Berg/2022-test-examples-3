package ru.yandex.chemodan.app.notifier.tanker;

import ru.yandex.commune.zk2.ZkPath;
import ru.yandex.inside.tanker.TankerClient;
import ru.yandex.inside.tanker.model.TankerResponse;
import ru.yandex.inside.tanker.model.parser.TankerResponseParser;
import ru.yandex.misc.io.ClassPathResourceInputStreamSource;

/**
 * @author buberman
 */
public class TestTankerRegistry extends TankerRegistry {
    public TestTankerRegistry(ZkPath zkPath) {
        super(zkPath, new TankerClient(null, ""), "", "");
    }

    // File stored under ru.yandex.chemodan.app.notifier classpath. If necessary, refresh file contents via this link:
    // http://tanker-api.tools.yandex.net:3000/keysets/tjson/?project-id=disk_notifiier&keyset-id=notifier-notifications&all-forms=1&status=unapproved
    private static final String STORED_TANKER_RESPONSE = "tanker-response.json";

    @Override
    protected TankerResponse getTankerResponse() {
        return TankerResponseParser.parseJson(getTestJson());
    }

    @Override
    protected void updateCachedResponse(TankerResponse response) {
        // Do nothing, no cached responses
    }

    private String getTestJson() {
        ClassPathResourceInputStreamSource iss = new ClassPathResourceInputStreamSource(
                getClass(), STORED_TANKER_RESPONSE);
        return iss.readText();
    }
}

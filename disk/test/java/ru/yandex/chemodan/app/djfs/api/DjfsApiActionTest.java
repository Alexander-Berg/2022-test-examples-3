package ru.yandex.chemodan.app.djfs.api;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.app.djfs.core.index.IndexContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.app.djfs.core.test.TestContextConfiguration;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.chemodan.util.web.A3JettyContextConfiguration;
import ru.yandex.commune.alive2.location.TestLocationResolverConfiguration;

@ContextConfiguration(classes = {
        A3JettyContextConfiguration.class,
        TestLocationResolverConfiguration.class,
        ChemodanInitContextConfiguration.class,
        IndexContextConfiguration.class,
        TestContextConfiguration.class,
        DjfsApiTestContextConfiguration.class
})
public abstract class DjfsApiActionTest extends DjfsTestBase {

    public static final ObjectMapper mapper = new ObjectMapper();

    @Value("${a3.port}")
    private int port;

    private A3TestHelper a3TestHelper;

    @PostConstruct
    public void startServers() {
        this.a3TestHelper = new A3TestHelper(port);
        getA3TestHelper().startServers(applicationContext);
    }

    protected A3TestHelper getA3TestHelper() {
        return a3TestHelper;
    }
}

package ru.yandex.market.cocon;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.application.properties.AppPropertyContextInitializer;
import ru.yandex.market.cocon.model.CabinetType;
import ru.yandex.market.cocon.util.MockServerContextInitializer;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.security.core.CachedKampferFactory;
import ru.yandex.market.security.core.KampferFactory;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {Cocon.class}
)
@ActiveProfiles(profiles = {"functionalTest", "development"})
@ContextConfiguration(
        initializers = {
                PropertiesDirInitializer.class,
                AppPropertyContextInitializer.class,
                MockServerContextInitializer.class,
        }
)
@SpyBean(CabinetService.class)
@TestPropertySource("classpath:functional-test.properties")
public abstract class FunctionalTest extends JupiterDbUnitTest {

    private static final String BASE_URL = "http://localhost:";

    @Autowired
    private ClientAndServer mockServer;

    @Autowired
    private KampferFactory kampferFactory;

    @LocalServerPort
    private int port;

    @AfterEach
    void tearDown() {
        mockServer.reset();
        if (kampferFactory instanceof CachedKampferFactory) {
            ((CachedKampferFactory) kampferFactory).clearCache();
        }
    }

    protected String baseUrl() {
        return BASE_URL + port;
    }

    protected String pagesUrl(CabinetType cabinetType, long uid) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/cabinet/{cabinet}/pages?uid={uid}")
                .buildAndExpand(cabinetType.getId(), uid)
                .toUriString();
    }

    protected String pageUrl(CabinetType cabinetType, String page, long uid) {
        return UriComponentsBuilder.fromUriString(baseUrl())
                .path("/cabinet/{cabinet}/page?uid={uid}&page={page}")
                .buildAndExpand(cabinetType.getId(), uid, page)
                .toUriString();
    }

    protected String getPages(CabinetType cabinetType, long uid) {
        String pagesUrl = pagesUrl(cabinetType, uid);
        return FunctionalTestHelper.get(pagesUrl).getBody();
    }

    protected String getPage(CabinetType cabinetType, String page, long uid) {
        String pagesUrl = pageUrl(cabinetType, page, uid);
        return FunctionalTestHelper.get(pagesUrl).getBody();
    }

}

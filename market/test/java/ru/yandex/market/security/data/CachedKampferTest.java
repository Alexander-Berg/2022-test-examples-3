package ru.yandex.market.security.data;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.yandex.market.security.data.kampfer.AuthorityLinkModel;
import ru.yandex.market.security.data.kampfer.AuthorityModel;
import ru.yandex.market.security.data.kampfer.EntityNotFoundException;
import ru.yandex.market.security.data.kampfer.impl.cached.ReadOnlyCachedKampfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class CachedKampferTest extends JavaSecTest {

    @BeforeAll
    static void init() {
        JavaSecTest.init(JavaSecTest.class);
    }

    @Test
    void getDomainId() {
        tester.insertDataSet("domain.single.csv");
        Kampfer kampfer = getKampfer("test");
        assertEquals(kampfer.domain().getDomainId(), 1);
        assertThrows(EntityNotFoundException.class,
                () -> getKampfer("notexist").domain().getDomainId());
    }

    @Test
    void getAuthId() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        assertEquals(4L, kampfer.authority().getAuthId("auth4"));
        assertThrows(EntityNotFoundException.class, () -> kampfer.authority().getAuthId("notexist"));
    }

    @Test
    void getAuthority() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        AuthorityModel auth = kampfer.authority().getAuthority(4);
        assertEquals("auth4", auth.getName());
        assertEquals("checker", auth.getChecker());
        assertThrows(EntityNotFoundException.class, () -> kampfer.authority().getAuthId("notexist"));
    }

    @Test
    void getAuthLinks() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        List<AuthorityLinkModel> links = kampfer.authority().getAuthLinks("auth4");
        assertEquals(1, links.size());
        AuthorityLinkModel link = links.get(0);
        assertEquals(4, link.getMainAuthId());
        assertEquals(5, link.getLinkedAuthId());
        assertThrows(EntityNotFoundException.class, () -> kampfer.authority().getAuthId("notexist"));
    }

    @Test
    void getOperationId() {
        tester.insertDataSet("domain.full.csv");
        Kampfer kampfer = getKampfer("test2");
        assertEquals(3L, kampfer.operation().getOperationId("op"));
        assertThrows(EntityNotFoundException.class,
                () -> kampfer.operation().getOperationId("notexist"));
    }

    Kampfer getKampfer(String domain) {
        Kampfer kampfer = new ReadOnlyCachedKampfer(dataSource, domain);
        kampfer.heatUp();
        return kampfer;
    }

}

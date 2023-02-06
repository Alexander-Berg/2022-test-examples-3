package ru.yandex.market.sberlog_tms.scheduled;

import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.market.sberlog_tms.SberlogtmsConfig;
import ru.yandex.market.sberlog_tms.dao.SberlogDbDao;
import ru.yandex.market.sberlog_tms.lock.LockService;
import ru.yandex.passport.tvmauth.NativeTvmClient;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 29.10.19
 */
@Disabled
@SpringJUnitConfig(SberlogtmsConfig.class)
public class MigrateMarketidToPuidTest {
    private final static String URL_TEMPLATE = "TEMPLATE";
    @Value("#{${sberlogtms.scheduled.migratemarketidtopuid.url.map.with.property}}")
    Map<String, Map<String, String>> urlMapWithProperty;
    @Value("#{${sberlogtms.scheduled.migratemarketidtopuid.BAD.url.map.with.property}}")
    Map<String, Map<String, String>> badUrlMapWithProperty;
    @Value("${sberlogtms.scheduled.migratemarketidtopuid.duration.minutes}")
    private long durationMinutes; // на сколько минут брать транзакцию
    @Autowired
    private LockService lockService;
    @Autowired
    private SberlogDbDao sberlogDbDao;
    @Autowired
    private Server server;


    private MigrateMarketidToPuidScheduled migrateMarketidToPuidScheduled;

    @BeforeEach
    public void MigrateMarketidToPuidTestInitial() {
        NativeTvmClient tvmClient = Mockito.mock(NativeTvmClient.class);
        Mockito.when(tvmClient.getServiceTicketFor("sberlog_dev")).thenReturn("AAAA:BBBB");
        Mockito.when(tvmClient.getServiceTicketFor("carter_testing")).thenReturn("AAAA:BBBB");
        Mockito.when(tvmClient.getServiceTicketFor("bad_alias")).thenReturn("YYYY:ZZZZ");
        this.migrateMarketidToPuidScheduled = new MigrateMarketidToPuidScheduled(lockService, sberlogDbDao, tvmClient);
        migrateMarketidToPuidScheduled.setDurationMinutes(durationMinutes);
    }

    @Test
    @DisplayName("happy path: how linked user is working")
    public void isUserLinking() {
        //  <column name="marketid" value="2190550858753009288"/>
        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(0));
        //  <column name="marketid" value="2190550858753009289"/>
        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(1));

        migrateMarketidToPuidScheduled.setUrlMapWithProperty(generateUrlList(urlMapWithProperty));
        migrateMarketidToPuidScheduled.MigrateMarketidToPuid();

        Assertions.assertNull(sberlogDbDao.getUnlinkedUser(0));
    }

    @Test
    @DisplayName("empty url list from property")
    public void emptyUrlList() {
        //<column name="marketid" value="2190550858753009288"/><column name="puid" value="12313125"/>
        //<column name="marketid" value="2190550858753009289"/><column name="puid" value="12313126"/>
        sberlogDbDao.setUserStatusIsUnLink("2190550858753009288", "12313125");
        sberlogDbDao.setUserStatusIsUnLink("2190550858753009289", "12313126");
        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(0));
        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(1));

        migrateMarketidToPuidScheduled.setUrlMapWithProperty(new HashMap<>());
        migrateMarketidToPuidScheduled.MigrateMarketidToPuid();

        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(0));
        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(1));

        migrateMarketidToPuidScheduled.setUrlMapWithProperty(generateUrlList(urlMapWithProperty));
        migrateMarketidToPuidScheduled.MigrateMarketidToPuid();

        Assertions.assertNull(sberlogDbDao.getUnlinkedUser(0));
    }

    @Test
    @DisplayName("url with bad backend")
    public void urlWithBadBackend() {
        sberlogDbDao.setUserStatusIsUnLink("2190550858753009288", "12313125");
        sberlogDbDao.setUserStatusIsUnLink("2190550858753009289", "12313126");
        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(0));
        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(1));

        migrateMarketidToPuidScheduled.setUrlMapWithProperty(generateUrlList(badUrlMapWithProperty));
        migrateMarketidToPuidScheduled.MigrateMarketidToPuid();

        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(0));
        Assertions.assertNotNull(sberlogDbDao.getUnlinkedUser(1));

    }

    private Map<String, Map<String, String>> generateUrlList(
            Map<String, Map<String, String>> urlMapWithPropertyBeforeTemplater) {
        Map<String, Map<String, String>> urlMapWithPropertyAfterTemplater = new HashMap<>();

        for (String key : urlMapWithPropertyBeforeTemplater.keySet()) {
            String newKey = key.replaceAll(URL_TEMPLATE, server.getURI().toString());
            urlMapWithPropertyAfterTemplater.put(newKey, urlMapWithPropertyBeforeTemplater.get(key));
        }

        return urlMapWithPropertyAfterTemplater;
    }
}

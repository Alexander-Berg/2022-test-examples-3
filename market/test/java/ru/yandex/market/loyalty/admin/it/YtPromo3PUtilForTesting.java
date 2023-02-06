package ru.yandex.market.loyalty.admin.it;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.loyalty.admin.config.ITConfig;
import ru.yandex.market.loyalty.admin.yt.YtClient;
import ru.yandex.market.loyalty.admin.yt.YtHelper;
import ru.yandex.market.loyalty.admin.yt.model.Promo3pYt;
import ru.yandex.market.loyalty.admin.yt.service.YtExportHelper;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;
import ru.yandex.qe.yt.cypress.exceptions.YTHttpException;
import ru.yandex.qe.yt.cypress.ypath.ObjectYPath;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.admin.utils.YtUtils.assertYtEquals;

/**
 * @author dinyat
 * 31/07/2017
 */
@Ignore("this test suite should be run manually because it uses real YT")
@ContextConfiguration(classes = ITConfig.class)
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
public class YtPromo3PUtilForTesting {

    @Autowired
    @YtHahn
    private YtClient ytClient;
    @Autowired
    @YtHahn
    YtExportHelper ytExportHelper;

    @Autowired
    private YPath basePath;

    private YPath tablePath;
    private YPath anotherTablePath;
    private YPath linkPath;

    private final Promo3pYt promo3pYt = Promo3pYt.builder()
            .setPromoMd5("promoMd5")
            .setDescription("description")
            .setStartDate(new GregorianCalendar(2017, Calendar.FEBRUARY, 1).getTime())
            .setEndDate(new GregorianCalendar(2020, Calendar.FEBRUARY, 1).getTime())
            .setRegions(Collections.singleton(1L))
            .build();


    @Before
    public void setUp() {
        tablePath = basePath.child("promo_3p_1");
        anotherTablePath = basePath.child("promo_3p_2");
        linkPath = basePath.child("promo_3p_link");

        ytClient.remove(basePath);
    }

    @After
    public void tearDown() {
        ytClient.remove(basePath);
    }

    @Test
    public void testCreateTable() {
        ytClient.createTable(tablePath);

        assertTrue(ytClient.exists(tablePath));
    }

    @Test
    public void testWriteTable() throws Exception {
        ytClient.createTable(tablePath);

        ytClient.append(tablePath, Collections.singletonList(promo3pYt));

        List<Promo3pYt> result = ytClient.read(tablePath, Promo3pYt.class);
        assertEquals(1, result.size());
        assertYtEquals(promo3pYt, result.get(0));
    }

    @Test
    public void testDropTable() {
        ytClient.createTable(tablePath);
        assertTrue(ytClient.exists(tablePath));

        ytClient.remove(tablePath);

        assertFalse(ytClient.exists(tablePath));
    }

    @Test
    public void testCreateLink() throws Exception {
        ytClient.createTable(tablePath);
        ytClient.append(tablePath, Collections.singletonList(promo3pYt));

        ytClient.createLink(tablePath, linkPath, Optional.empty());

        List<Promo3pYt> result = ytClient.read(linkPath, Promo3pYt.class);
        assertEquals(1, result.size());
        assertYtEquals(promo3pYt, result.get(0));
    }

    @Test(expected = YTHttpException.class)
    public void testCreateLinkTwice() throws Exception {
        Promo3pYt anotherPromo3pYt = Promo3pYt.builder()
                .setPromoMd5("anotherPromoMd5")
                .setDescription("description")
                .setStartDate(new GregorianCalendar(2017, Calendar.FEBRUARY, 1).getTime())
                .setEndDate(new GregorianCalendar(2020, Calendar.FEBRUARY, 1).getTime())
                .setRegions(Collections.singleton(1L))
                .build();
        ytClient.createTable(tablePath);
        ytClient.createTable(anotherTablePath);
        ytClient.append(tablePath, Collections.singletonList(promo3pYt));
        ytClient.append(anotherTablePath, Collections.singletonList(anotherPromo3pYt));

        ytClient.createLink(tablePath, linkPath, Optional.empty());

        List<Promo3pYt> result = ytClient.read(tablePath, Promo3pYt.class);
        assertEquals(1, result.size());
        assertYtEquals(promo3pYt, result.get(0));

        ytClient.createLink(anotherTablePath, linkPath, Optional.empty());
    }

    @Test
    public void testRemoveLink() {
        ytClient.createTable(tablePath);
        ytClient.createLink(tablePath, linkPath, Optional.empty());
        assertTrue(ytClient.exists(linkPath));

        ytClient.removeLink(linkPath, Optional.empty());

        assertFalse(ytClient.exists(linkPath));
    }

    @Test
    public void testRemoveBrokenLink() {
        ytClient.createTable(tablePath);
        ytClient.createLink(tablePath, linkPath, Optional.empty());
        ytClient.remove(tablePath);

        assertFalse(ytClient.exists(linkPath));

        ytClient.removeLink(linkPath, Optional.empty());
    }

    @Test
    public void testRemoveNotExistedLink() {
        ytClient.createFolder(basePath);
        ytClient.createTable(tablePath);
        ytClient.removeLink(linkPath, Optional.empty());
    }

    @Test
    public void testDereferenceLink() {
        ytClient.createTable(tablePath);
        ytClient.createLink(tablePath, linkPath, Optional.empty());
        assertEquals(tablePath, ytClient.dereferenceLink(linkPath, Optional.empty()));
    }

    @Test
    public void testList() {
        ytClient.createTable(tablePath);
        ytClient.createTable(anotherTablePath);

        List<String> list = ytClient.list(basePath);

        assertEquals(2, list.size());
        assertThat(YtHelper.getName(anotherTablePath), equalTo(list.get(0)));
        assertThat(YtHelper.getName(tablePath), equalTo(list.get(1)));
    }

    @Test
    public void shouldReturnRowsCount() {
        YPath testTable = basePath.child("some_test_table");
        ytClient.createTable(testTable);

        ytClient.append(testTable, List.of(promo3pYt));

        assertEquals(1, ytExportHelper.getWrittenRowsCount(testTable));
    }

    @Test
    public void shouldReturnModificationTime() {
        YPath testTable = basePath.child("some_test_table");
        ytClient.createTable(testTable);
        ytClient.append(testTable, List.of(promo3pYt));
        String creationTime = ytExportHelper.getModificationTime(testTable);
        assertNotNull(creationTime);
    }
}

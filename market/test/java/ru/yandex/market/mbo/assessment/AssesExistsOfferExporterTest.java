package ru.yandex.market.mbo.assessment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.core.export.yt.YtCustomQueryExporter;
import ru.yandex.market.mbo.core.join.yt.YtJoinService;
import ru.yandex.market.mbo.yt.TestYt;

@RunWith(MockitoJUnitRunner.class)
public class AssesExistsOfferExporterTest {

    public static final YPath YT_DIR = YPath.simple("//home/mbo/temp");

    private AssessExistsOfferExporter exporter;
    private TestYt testYt;
    private YtCustomQueryExporter ytCustomQueryExporter;
    private YtJoinService ytJoinService;

    @Before
    public void setUp() {
        testYt = new TestYt();
        ytCustomQueryExporter = Mockito.mock(YtCustomQueryExporter.class);
        ytJoinService = Mockito.mock(YtJoinService.class);
        exporter = new AssessExistsOfferExporter(testYt, ytCustomQueryExporter, ytJoinService);
        ReflectionTestUtils.setField(exporter, "tempOffersPath", YT_DIR.toString());
        ReflectionTestUtils.setField(exporter, "mediumLogTable", YT_DIR.child("medium_log").toString());
    }

    @Test
    public void testExporter() {
        exporter.exportAndJoinExistsOffers();
        Mockito.verify(ytCustomQueryExporter, Mockito.times(1))
            .exportDataToTable(Mockito.any(),
                Mockito.eq("exists_offers_temp"),
                Mockito.any(),
                Mockito.anyString(),
                Mockito.any());
        Mockito.verify(ytJoinService, Mockito.times(1))
            .joinTable(Mockito.any(),
                Mockito.eq(YT_DIR.child("exists_offers")),
                Mockito.eq(YT_DIR),
                Mockito.any(),
                Mockito.eq("classifier_magic_id"),
                Mockito.any(),
                Mockito.eq(YT_DIR.child("exists_offers_temp")),
                Mockito.eq(YT_DIR.child("medium_log"))
            );
    }
}

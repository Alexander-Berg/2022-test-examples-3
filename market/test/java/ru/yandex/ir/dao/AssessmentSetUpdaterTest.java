package ru.yandex.ir.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AssessmentSetUpdaterTest {
    @Mock
    private Yt ytApi;
    @Mock
    private Cypress cypress;

    @Before
    public void setup() {
        when(ytApi.cypress()).thenReturn(cypress);

        when(cypress.exists(eq(YPath.simple("//not/empty")))).thenReturn(true);
        when(cypress.list(eq(YPath.simple("//not/empty")))).thenReturn(
            Cf.toList(
                Cf.list(
                    "offer_pairZ_20160125_0000.tsv",
                    "offer_pairs_20170125_0000.tsv",
                    "offer_pairs_20170225_0000.tsv",
                    "offer_pairs_20170325_0000.tsv",
                    "offer_pairs_20170425_0000.tsv",
                    "offer_pairs_20170525_0000.tsv",
                    "offer_pairZ_20180125_0000.tsv"
                ).map(x -> new YTreeStringNodeImpl(x, Cf.map()))
                    .shuffle()
                    .map(x -> x)
            )
        );

        when(cypress.exists(eq(YPath.simple("//empty")))).thenReturn(true);
        when(cypress.list(eq(YPath.simple("//empty")))).thenReturn(Cf.list());


        when(cypress.exists(eq(YPath.simple("//does/not/exist")))).thenReturn(false);
        when(cypress.list(eq(YPath.simple("//does/not/exist")))).thenThrow(IllegalStateException.class);
    }

    @Test
    public void tsvExists() {
        assertEquals(
            "offer_pairs_20170525_0000.tsv",
            AssessmentSetUpdater.getLatestSetFileNameFromYt(ytApi, YPath.simple("//not/empty"))
        );
    }

    @Test
    public void tsvDoesNotExist() {
        assertNull(AssessmentSetUpdater.getLatestSetFileNameFromYt(ytApi, YPath.simple("//empty")));
    }

    @Test
    public void folderDoesNotExist() {
        assertNull(AssessmentSetUpdater.getLatestSetFileNameFromYt(ytApi, YPath.simple("//does/not/exist")));
    }
}
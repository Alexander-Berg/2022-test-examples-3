package ru.yandex.direct.jobs.advq.offline.export;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.ytwrapper.model.YtField;
import ru.yandex.direct.ytwrapper.model.YtTableRow;
import ru.yandex.direct.ytwrapper.model.YtYield;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OfflineAdvqReducerTest {
    private static final Long CID1 = 11L;
    private static final Long CID2 = 11L;
    private static final Long CID3 = 11L;
    private static final Long PID1 = 21L;
    private static final Long PID2 = 22L;
    private static final Long PID3 = 23L;
    private static final Long PID4 = 24L;
    private static final Long BID_ID1 = 31L;
    private static final Long BID_ID2 = 32L;
    private static final Long BID_ID3 = 33L;
    private static final Long BID_ID4 = 34L;

    private static final Set<String> stopWords = singleton("в");

    private OfflineAdvqReducer reducer;

    @Captor
    private ArgumentCaptor<YtTableRow> captor;
    @Mock
    private YtYield ytYield;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        reducer = new OfflineAdvqReducer(ytYield, stopWords);
    }

    @Test
    void testOneCorrectIteration() {
        IteratorF<YtTableRow> nodeIterator = Cf.wrap(Arrays.asList(
                OfflineAdvqTestUtils.campaignsRow(CID1, "No"),
                OfflineAdvqTestUtils.phrasesRow(CID1, PID1, "[\"привет\", \" конь\"]", "text", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID1, "привет мир"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID2, "конный мир"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID3, "серый конь -красный"),
                OfflineAdvqTestUtils.phrasesRow(CID1, PID2, "[\"бесхозный\"]", "text", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils
                        .phrasesRow(CID1, PID3, "[\"красный\", \"свет\"]", "mobile_content", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID3, BID_ID4, "белый цвет")
        )).iterator();
        reducer.reduce(CID1, nodeIterator);

        verify(ytYield, times(4)).yield(anyInt(), captor.capture());
        List<YtTableRow> values = captor.getAllValues();
        List<YtTableRow> expected = Arrays.asList(
                OfflineAdvqTestUtils
                        .resultRow(BID_ID1, PID1, "привет мир", "привет мир", "1,2,-3", Collections.emptyList(),
                                Collections.singletonList("конь")),
                OfflineAdvqTestUtils
                        .resultRow(BID_ID2, PID1, "конный мир", "конный мир", "1,2,-3", Collections.emptyList(),
                                Arrays.asList("привет", "конь")),
                OfflineAdvqTestUtils.resultRow(BID_ID3, PID1, "серый конь -красный", "серый конь", "1,2,-3",
                        Collections.emptyList(),
                        Arrays.asList("привет", "красный")),
                OfflineAdvqTestUtils
                        .resultRow(BID_ID4, PID3, "белый цвет", "белый цвет", "1,2,-3",
                                Arrays.asList("phone", "tablet"), Arrays.asList("красный", "свет"))
        );
        for (int i = 0; i < values.size(); i++) {
            checkValues(values.get(i), expected.get(i));
        }
    }

    @Test
    void testThreeCorrectIterations() {
        IteratorF<YtTableRow> nodeIterator = Cf.wrap(Arrays.asList(
                OfflineAdvqTestUtils.campaignsRow(CID1, "No"),
                OfflineAdvqTestUtils.phrasesRow(CID1, PID1, "[\"привет\", \"конь\"]", "text", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID1, "привет мир"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID2, "конный мир")
        )).iterator();
        reducer.reduce(CID1, nodeIterator);

        nodeIterator = Cf.wrap(Arrays.asList(
                OfflineAdvqTestUtils.campaignsRow(CID2, "No"),
                OfflineAdvqTestUtils.phrasesRow(CID2, PID2, "[\"привет\", \"конь\"]", "text", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils
                        .phrasesRow(CID2, PID3, "[\"красный\", \"свет\"]", "mobile_content", "1,2,-3", "Processed")
        )).iterator();
        reducer.reduce(CID2, nodeIterator);

        nodeIterator = Cf.wrap(Arrays.asList(
                OfflineAdvqTestUtils.campaignsRow(CID3, "No"),
                OfflineAdvqTestUtils.phrasesRow(CID3, PID4, "[\"бесхозный\"]", "text", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils.bidsRow(CID3, PID4, BID_ID3, "белый цвет")
        )).iterator();
        reducer.reduce(CID3, nodeIterator);

        verify(ytYield, times(3)).yield(anyInt(), captor.capture());
        List<YtTableRow> values = captor.getAllValues();
        List<YtTableRow> expected = Arrays.asList(
                OfflineAdvqTestUtils
                        .resultRow(BID_ID1, PID1, "привет мир", "привет мир", "1,2,-3", Collections.emptyList(),
                                Collections.singletonList("конь")),
                OfflineAdvqTestUtils
                        .resultRow(BID_ID2, PID1, "конный мир", "конный мир", "1,2,-3", Collections.emptyList(),
                                Arrays.asList("привет", "конь")),
                OfflineAdvqTestUtils
                        .resultRow(BID_ID3, PID4, "белый цвет", "белый цвет", "1,2,-3", Collections.emptyList(),
                                Arrays.asList("бесхозный"))
        );
        for (int i = 0; i < values.size(); i++) {
            checkValues(values.get(i), expected.get(i));
        }
    }

    @Test
    void testNoPidData() {
        IteratorF<YtTableRow> nodeIterator = Cf.wrap(Arrays.asList(
                OfflineAdvqTestUtils.campaignsRow(CID1, "No"),
                OfflineAdvqTestUtils.phrasesRow(CID2, PID2, "[\"привет\", \"конь\"]", "text", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID1, "привет мир")
        )).iterator();
        reducer.reduce(CID1, nodeIterator);

        verify(ytYield, never()).yield(anyInt(), any());
    }

    @Test
    void testShowsForecastArchivedPidData() {
        IteratorF<YtTableRow> nodeIterator = Cf.wrap(Arrays.asList(
                OfflineAdvqTestUtils.campaignsRow(CID1, "No"),
                OfflineAdvqTestUtils.phrasesRow(CID2, PID1, "[\"привет\", \"конь\"]", "text", "1,2,-3", "Archived"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID1, "привет мир")
        )).iterator();
        reducer.reduce(CID1, nodeIterator);

        verify(ytYield, never()).yield(anyInt(), any());
    }

    @Test
    void testNoCampaignData() {
        IteratorF<YtTableRow> nodeIterator = Cf.wrap(Arrays.asList(
                OfflineAdvqTestUtils.phrasesRow(CID1, PID1, "[\"привет\", \"конь\"]", "text", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID1, "привет мир"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID2, "конный мир"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID3, "серый конь -красный"),
                OfflineAdvqTestUtils.phrasesRow(CID1, PID2, "[\"бесхозный\"]", "text", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils
                        .phrasesRow(CID1, PID3, "[\"красный\", \"свет\"]", "mobile_content", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID3, BID_ID4, "белый цвет")
        )).iterator();
        reducer.reduce(CID1, nodeIterator);

        verify(ytYield, never()).yield(anyInt(), any());
    }

    @Test
    void testArchivedCampaignData() {
        IteratorF<YtTableRow> nodeIterator = Cf.wrap(Arrays.asList(
                OfflineAdvqTestUtils.campaignsRow(CID1, "Yes"),
                OfflineAdvqTestUtils.phrasesRow(CID1, PID1, "[\"привет\", \"конь\"]", "text", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID1, "привет мир"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID2, "конный мир"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID1, BID_ID3, "серый конь -красный"),
                OfflineAdvqTestUtils.phrasesRow(CID1, PID2, "[\"бесхозный\"]", "text", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils
                        .phrasesRow(CID1, PID3, "[\"красный\", \"свет\"]", "mobile_content", "1,2,-3", "Processed"),
                OfflineAdvqTestUtils.bidsRow(CID1, PID3, BID_ID4, "белый цвет")
        )).iterator();
        reducer.reduce(CID1, nodeIterator);

        verify(ytYield, never()).yield(anyInt(), any());
    }

    private void checkValues(YtTableRow got, YtTableRow expected) {
        for (YtField<?> ytField : Arrays
                .asList(OfflineAdvqExportOutputTableRow.ID, OfflineAdvqExportOutputTableRow.ORIGINAL_KEYWORD,
                        OfflineAdvqExportOutputTableRow.GEO,
                        OfflineAdvqExportOutputTableRow.KEYWORD, OfflineAdvqExportOutputTableRow.DEVICES,
                        OfflineAdvqExportOutputTableRow.GROUP_ID)) {
            assertThat("Значения совпадают", got.valueOf(ytField), equalTo(expected.valueOf(ytField)));
        }
        String[] gotMW = JsonUtils.fromJson(got.valueOf(OfflineAdvqExportOutputTableRow.MINUS_WORDS), String[].class);
        String[] expectedMW =
                JsonUtils.fromJson(expected.valueOf(OfflineAdvqExportOutputTableRow.MINUS_WORDS), String[].class);
        assertThat("Значения совпадают", Arrays.asList(gotMW), containsInAnyOrder(expectedMW));
    }
}

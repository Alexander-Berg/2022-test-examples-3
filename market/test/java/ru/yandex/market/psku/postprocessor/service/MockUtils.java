package ru.yandex.market.psku.postprocessor.service;

import org.mockito.Mockito;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.SessionState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.Session;
import ru.yandex.market.psku.postprocessor.service.uc.UCQueryRequest;
import ru.yandex.market.psku.postprocessor.service.uc.UCQueryResponse;
import ru.yandex.market.psku.postprocessor.service.uc.UCService;
import ru.yandex.market.psku.postprocessor.service.yt.session.SessionParam;
import ru.yandex.market.psku.postprocessor.service.yt.YtDataService;
import ru.yandex.market.psku.postprocessor.service.yt.session.YtEnrichSessionService;
import ru.yandex.market.psku.postprocessor.service.yt.session.YtPskuSessionService;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * @author Fedor Dergachev <a href="mailto:dergachevfv@yandex-team.ru"></a>
 */
public class MockUtils {

    public static final String TEST_SESSION_NAME = "test_session";
    public static final Long TEST_SESSION_ID = 10L;
    public static final SessionParam TEST_SESSION_PARAM = new SessionParam(TEST_SESSION_ID, TEST_SESSION_NAME);
    public static final String RECENT_EXPORT_CREATION_DATE = "2019-12-05T01:57:02.301776Z";
    public static final Long LAST_ENRICHED_SESSION_ID = 8L;

    private MockUtils() {
    }

    public static void mockYtPskuSessionService(YtPskuSessionService ytPskuSessionService) {
        Mockito.when(ytPskuSessionService.startNewPskuSession()).thenReturn(TEST_SESSION_PARAM);
        Mockito.when(ytPskuSessionService.getLastSessionExportDate()).thenReturn("2001-12-05T01:57:02.301776Z");
        Mockito.when(ytPskuSessionService.getLastSessionId()).thenReturn(TEST_SESSION_ID);
    }

    public static void mockYtEnrichSessionService(YtEnrichSessionService ytEnrichSessionService) {
        Mockito.when(ytEnrichSessionService.startNewEnrichSession(Mockito.anyString())).thenReturn(true);
        Mockito.when(ytEnrichSessionService.getLastSessionExportDate()).thenReturn("2001-12-05T01:57:02.301776Z");
        Mockito.when(ytEnrichSessionService.getLastSessionId()).thenReturn(LAST_ENRICHED_SESSION_ID);
    }

    public static void mockYtDataService(YtDataService ytDataService,
                                         List<PskuInfo> testPskuInfos,
                                         Map<PskuInfo, UltraController.EnrichedOffer>
                                             pskuInfoToUCOffer) {
        Mockito.doAnswer(invocation -> {
            Consumer<PskuInfo> consumer = invocation.getArgument(1);
            testPskuInfos.forEach(consumer);
            return null;
        }).when(ytDataService).processPskus(eq(TEST_SESSION_NAME), any());

        List<EnrichedPskuInfoWithAdditionalInfo> enrichedPskuInfos = testPskuInfos.stream()
            .map(pskuInfo -> EnrichedPskuInfoWithAdditionalInfo.builder()
                .setPskuInfo(convertPskuForPskuWithAdditionalInfo(pskuInfo))
                .setUcEnrichedOffer(pskuInfoToUCOffer.get(pskuInfo))
                .setSessionId(TEST_SESSION_ID)
                .build())
            .collect(Collectors.toList());

        Mockito.when(ytDataService.getEnrichedPskusToHandle(any()))
            .thenReturn(enrichedPskuInfos::forEach);

        Mockito.when(ytDataService.getRecentExportCreationDate())
                .thenReturn(RECENT_EXPORT_CREATION_DATE);
    }

    public static void mockYtEnrichedPskusToHandle(YtDataService ytDataService,
                                                   List<PskuInfo> testPskuInfos,
                                                   Map<PskuInfo, UltraController.EnrichedOffer>
                                                       pskuInfoToUCOffer) {

        List<EnrichedPskuInfoWithAdditionalInfo> enrichedPskuInfos = testPskuInfos.stream()
            .map(pskuInfo -> EnrichedPskuInfoWithAdditionalInfo.builder()
                .setPskuInfo(pskuInfo)
                .setUcEnrichedOffer(pskuInfoToUCOffer.get(pskuInfo))
                .setSessionId(TEST_SESSION_ID)
                .build())
            .collect(Collectors.toList());

        Mockito.doAnswer(invocation -> {
            Consumer<PskuInfo> consumer = invocation.getArgument(1);
            testPskuInfos.forEach(consumer);
            return null;
        }).when(ytDataService).processPskus(eq(TEST_SESSION_NAME), any());

        Mockito.when(ytDataService.getEnrichedPskusToHandle(any()))
            .thenReturn(enrichedPskuInfos::forEach);
    }

    public static void mockUCService(UCService ucService,
                                     List<PskuInfo> testPskuInfos,
                                     Map<PskuInfo, UltraController.EnrichedOffer> pskuInfoToUCOffer) {
        UCQueryResponse.Builder ucResponseBuilder = UCQueryResponse.builder();
        testPskuInfos.forEach(pskuInfo -> {
            UltraController.EnrichedOffer ucOffer = pskuInfoToUCOffer.get(pskuInfo);
            ucResponseBuilder.setUCOfferForPsku(pskuInfo, ucOffer);
        });

        Mockito.when(ucService.callUC(any(UCQueryRequest.class)))
            .thenReturn(ucResponseBuilder.build());
    }

    public static PskuInfo convertPskuForPskuWithAdditionalInfo(PskuInfo pskuInfo) {
        return PskuInfo.builder()
            .setCategoryId(pskuInfo.getCategoryId())
            .setCategoryName(pskuInfo.getCategoryName())
            .setId(pskuInfo.getId())
            .setModel(pskuInfo.getModel())
            .setTitle(pskuInfo.getTitle())
            .setRefilledDate(pskuInfo.getRefilledDate())
            .build();
    }

    public static Session getTestSession(SessionState sessionState) {
        return new Session(
                MockUtils.TEST_SESSION_ID,
                MockUtils.TEST_SESSION_NAME,
                sessionState,
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now()),
                0,
                0
        );
    }
}

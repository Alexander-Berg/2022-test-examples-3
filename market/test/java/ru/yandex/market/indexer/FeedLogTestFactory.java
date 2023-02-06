package ru.yandex.market.indexer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import ru.yandex.market.proto.indexer.v2.FeedLog;

public class FeedLogTestFactory {
    public static final String DOMAINS = "yandex.ru,yandex-team.ru,yandex.net";
    public static final String URL_IN_ARCHIVE = "url_in_archive";
    public static final String YML_DATE = "yml_date";
    public static final FeedLog.ParseStats CORRECT_PARSE_STATS = FeedLog.ParseStats.newBuilder()
            .setTotalOffers(11)
            .setValidOffers(13)
            .setWarnOffers(17)
            .setErrorOffers(19)
            .build();
    private static final DateTimeFormatter SESSION_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final ZoneId DEFAULT_ZONE = Clock.systemDefaultZone().getZone();

    public static FeedLog.Feed.Builder getCorrectFeedLogBase() {
        Instant startTime = Instant.now();
        return FeedLog.Feed.newBuilder()
                .setLastSession(
                        FeedLog.RobotFeedSession.newBuilder()
                                .setSessionName(getSessionName(startTime))
                                .setUrlInArchive(URL_IN_ARCHIVE)
                                .setYmlDate(YML_DATE)
                                .setStartDate((int) startTime.minusSeconds(10).getEpochSecond())
                                .setDownloadDate((int) startTime.getEpochSecond())
                                .setParseRetcode(0)
                                .build()
                )
                .setIndexation(FeedLog.ProcessingSummary.newBuilder().setStatistics(CORRECT_PARSE_STATS))
                .setOffersHosts(DOMAINS)
                .setDownloadRetcode(0)
                .setDownloadStatus("200 OK")
                .setIndexedStatus("ok")
                .setFeedProcessingType(FeedLog.FeedProcessingType.PULL);
    }

    public static FeedLog.Feed.Builder getWarningFeedLogBase() {
        FeedLog.Feed.Builder correctFeedLogBase = getCorrectFeedLogBase();
        return correctFeedLogBase
                .setLastSession(
                        FeedLog.RobotFeedSession.newBuilder(correctFeedLogBase.getLastSession())
                                .setParseRetcode(1)
                                .build()
                );
    }

    /**
     * Последняя сессия пофейленная, но есть кэшированная хорошая (с варнингом).
     */
    public static FeedLog.Feed.Builder getFailedCachedFeedLogBase() {
        Instant cachedStartTime = Instant.now().minusSeconds(3000);
        FeedLog.Feed.Builder correctFeedLogBase = getCorrectFeedLogBase();
        return correctFeedLogBase
                .setLastSession(
                        FeedLog.RobotFeedSession.newBuilder(correctFeedLogBase.getLastSession())
                                .setParseRetcode(3)
                                .build()
                )
                .setCachedSession(
                        FeedLog.RobotFeedSession.newBuilder()
                                .setSessionName(getSessionName(cachedStartTime))
                                .setUrlInArchive(URL_IN_ARCHIVE)
                                .setStartDate((int) cachedStartTime.minusSeconds(2).getEpochSecond())
                                .setDownloadDate((int) cachedStartTime.getEpochSecond())
                                .setParseRetcode(1)
                                .build()
                )
                .setIndexedStatus("cached");
    }

    private static String getSessionName(Instant time) {
        return SESSION_NAME_FORMATTER.format(time.atZone(DEFAULT_ZONE));
    }
}

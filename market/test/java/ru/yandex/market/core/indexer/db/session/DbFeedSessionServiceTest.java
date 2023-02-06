package ru.yandex.market.core.indexer.db.session;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.indexer.model.FeedSession;
import ru.yandex.market.core.indexer.model.IndexerType;
import ru.yandex.market.core.indexer.model.ReturnCode;
import ru.yandex.market.core.indexer.parser.FeedLogCodeStats;
import ru.yandex.market.core.indexer.parser.FeedLogStats;
import ru.yandex.market.core.indexer.parser.ParseLogParsed;

import static org.assertj.core.api.Assertions.assertThat;

class DbFeedSessionServiceTest extends FunctionalTest {
    static final Instant START_TIME = Instant.parse("2000-01-01T00:00:00Z");
    static final String PARSE_LOG = "parse_log";
    static final ParseLogParsed PARSE_LOG_PARSED = new ParseLogParsed(
            List.of(FeedLogCodeStats.builder()
                    .setCode("code")
                    .setSubcode("subcode")
                    .build()),
            List.of(new FeedLogStats("code", Map.of("arg", "value")))
    );

    @Autowired
    JdbcTemplate jdbcTemplate;

    FeedSessionService feedSessionService;

    @BeforeEach
    void setUp() {
        feedSessionService = new DbFeedSessionService(jdbcTemplate);
    }

    @Test
    @DbUnitDataSet(before = "DbFeedSessionServiceTest.csv")
    void hasSession() {
        assertThat(feedSessionService.hasSession(makeSessionId(1L))).isTrue();
        assertThat(feedSessionService.hasSession(new FeedSession.FeedSessionId(
                2,
                IndexerType.MAIN,
                "nonExistingSessionName",
                START_TIME,
                "cluster"
        ))).isFalse();
    }

    @Test
    @DbUnitDataSet(before = "DbFeedSessionServiceTest.csv")
    void getSession() {
        var expectedWithoutRetCode = makeSession(1L, null, null, null, null);
        var actualWithoutRetCode = feedSessionService.getSession(expectedWithoutRetCode.getId());
        assertThat(actualWithoutRetCode).contains(expectedWithoutRetCode);

        var expectedWithRetCode = makeSession(2L, ReturnCode.OK, ReturnCode.ERROR, null, null);
        var actualWithRetCode = feedSessionService.getSession(expectedWithRetCode.getId());
        assertThat(actualWithRetCode).contains(expectedWithRetCode);
    }

    @Test
    @DbUnitDataSet(before = "DbFeedSessionServiceTest.csv")
    void getSessionWithLogs() {
        // given
        var expectedWithoutRetCode = makeSession(1L, null, null, PARSE_LOG, PARSE_LOG_PARSED);
        var expectedWithRetCode = makeSession(2L, ReturnCode.OK, ReturnCode.ERROR, PARSE_LOG, PARSE_LOG_PARSED);

        // when
        var actualWithoutRetCode = feedSessionService.getSessionWithLogs(expectedWithoutRetCode.getId());
        var actualWithRetCode = feedSessionService.getSessionWithLogs(expectedWithRetCode.getId());

        // then
        assertThat(actualWithoutRetCode).contains(expectedWithoutRetCode);
        assertThat(actualWithRetCode).contains(expectedWithRetCode);
    }

    @Test
    @DbUnitDataSet(before = "DbFeedSessionServiceTest.csv")
    void getSessionsWithLogs() {
        // given
        var expectedWithoutRetCode = makeSession(1L, null, null, PARSE_LOG, PARSE_LOG_PARSED);
        var expectedWithRetCode = makeSession(2L, ReturnCode.OK, ReturnCode.ERROR, PARSE_LOG, PARSE_LOG_PARSED);
        var nonExisting = makeSessionId(3L);

        // when
        var result = feedSessionService.getSessionsWithLogs(Set.of(
                expectedWithoutRetCode.getId(),
                expectedWithRetCode.getId(),
                nonExisting
        ));

        // then
        assertThat(result).isEqualTo(Map.of(
                expectedWithoutRetCode.getId(), expectedWithoutRetCode,
                expectedWithRetCode.getId(), expectedWithRetCode
        ));
    }

    @Test
    @DbUnitDataSet(after = "DbFeedSessionServiceTest.csv")
    void getSessionInserter() {
        feedSessionService.getSessionInserter(0).processAndFlush(List.of(
                makeSession(1L, null, null, PARSE_LOG, PARSE_LOG_PARSED),
                makeSession(2L, ReturnCode.OK, ReturnCode.ERROR, PARSE_LOG, PARSE_LOG_PARSED)
        ));
    }

    static FeedSession makeSession(
            long feedId,
            @Nullable ReturnCode parseRetCode,
            @Nullable ReturnCode returnCode,
            String parseLog,
            ParseLogParsed parseLogParsed
    ) {
        return new FeedSession.Builder()
                .setId(makeSessionId(feedId))
                .setParseLog(parseLog)
                .setParseLogParsed(parseLogParsed)
                .setDownloadTime(Instant.parse("2001-01-01T00:00:00Z"))
                .setDownloadReturnCode(ReturnCode.OK)
                .setDownloadStatus("200 ok")
                .setParseReturnCode(parseRetCode)
                .setUrlInArchive("url")
                .setYmlDate("yml_date")
                .setTotalOffersCount(11L)
                .setValidOffersCount(13L)
                .setWarnOffersCount(17L)
                .setErrorOffersCount(19L)
                .setPublished(true)
                .setReturnCode(returnCode)
                .build();
    }

    static FeedSession.FeedSessionId makeSessionId(long feedId) {
        return new FeedSession.FeedSessionId(
                feedId,
                IndexerType.MAIN,
                "session",
                START_TIME,
                "cluster"
        );
    }

}

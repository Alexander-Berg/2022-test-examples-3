package ru.yandex.market.core.indexer.db.session;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.indexer.model.ReturnCode;
import ru.yandex.market.yt.binding.BindingTable;
import ru.yandex.market.yt.client.YtClientProxy;
import ru.yandex.market.yt.client.YtClientProxySource;

import static org.mockito.Mockito.description;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.core.indexer.db.session.DbFeedSessionServiceTest.PARSE_LOG;
import static ru.yandex.market.core.indexer.db.session.DbFeedSessionServiceTest.PARSE_LOG_PARSED;
import static ru.yandex.market.core.indexer.db.session.DbFeedSessionServiceTest.START_TIME;
import static ru.yandex.market.core.indexer.db.session.DbFeedSessionServiceTest.makeSession;
import static ru.yandex.market.core.indexer.db.session.DbFeedSessionServiceTest.makeSessionId;

class YtFeedSessionServiceTest {
    private static final BindingTable<YtFeedSession> BINDING_TABLE = new BindingTable<>("T", YtFeedSession.class);
    private YtClientProxy ytClient = mock(YtClientProxy.class);
    private FeedSessionService feedSessionService;

    @BeforeEach
    void setUp() {
        feedSessionService = new YtFeedSessionService(
                BINDING_TABLE,
                ytClient,
                YtClientProxySource.singleSource(ytClient)
        );
    }

    @Test
    void hasSession() {
        // given
        var sessionId = makeSessionId(1L);

        // when
        feedSessionService.hasSession(sessionId);

        // then
        verify(ytClient, description("смотрим наличие ключа через lookup")).lookupRows(
                BINDING_TABLE.getTable(),
                YtFeedSessionService.KEY_BINDER,
                List.of(new YtFeedSession.Id(sessionId))
        );
    }

    @Test
    void getSession() {
        // when
        feedSessionService.getSession(makeSessionId(1L));

        // then
        verify(ytClient).selectRows(
                "feedId, startTime, sessionName, indexerType, cluster, downloadReturnCode, downloadStatus," +
                        " downloadTime, parseReturnCode, published, returnCode, urlInArchive, ymlDate," +
                        " totalOffersCount, validOffersCount, warnOffersCount, errorOffersCount," +
                        " string(null) as parseLog, string(null) as parseLogParsed" +
                        " from [T]" +
                        " where feedId = 1" +
                        "   and startTime = 946684800000" +
                        "   and indexerType = 0" +
                        "   and sessionName = \"session\"" +
                        "   and cluster = \"cluster\"" +
                        "   limit 1",
                BINDING_TABLE.getBinder()
        );
    }

    @Test
    void getSessionWithLogs() {
        // given
        var sessionId = makeSessionId(1L);

        // when
        feedSessionService.getSessionWithLogs(sessionId);

        // then
        verify(ytClient).lookupRows(
                BINDING_TABLE.getTable(),
                YtFeedSessionService.KEY_BINDER,
                List.of(new YtFeedSession.Id(sessionId)),
                BINDING_TABLE.getBinder()
        );
    }

    @Test
    void getSessionsWithLogs() {
        // given
        var keys = Set.of(
                makeSessionId(1L),
                makeSessionId(2L)
        );

        // when
        feedSessionService.getSessionsWithLogs(keys);

        // then
        verify(ytClient).lookupRows(
                BINDING_TABLE.getTable(),
                YtFeedSessionService.KEY_BINDER,
                keys.stream().map(YtFeedSession.Id::new).collect(Collectors.toList()),
                BINDING_TABLE.getBinder()
        );
    }

    @Test
    void getNonFatalSessions() {
        // when
        feedSessionService.getNonFatalSessions(123L, START_TIME);

        // then
        verify(ytClient).selectRows(
                "feedId, startTime, sessionName, indexerType, cluster, downloadReturnCode, downloadStatus," +
                        " downloadTime, parseReturnCode, published, returnCode, urlInArchive, ymlDate," +
                        " totalOffersCount, validOffersCount, warnOffersCount, errorOffersCount," +
                        " string(null) as parseLog, string(null) as parseLogParsed" +
                        " from [T]" +
                        " where feedId = 123" +
                        "   and startTime >= 946684800000" +
                        "   and (not is_null(returnCode) and returnCode < 3)" +
                        "   and (published" +
                        " or not is_null(parseReturnCode)" +
                        " or (not is_null(returnCode) and returnCode > 0)" +
                        ")",
                BINDING_TABLE.getBinder()
        );
    }

    @Test
    void getSessionInserter() {
        // when
        var feedSessions = List.of(
                makeSession(1L, null, null, PARSE_LOG, PARSE_LOG_PARSED),
                makeSession(2L, ReturnCode.OK, ReturnCode.ERROR, PARSE_LOG, PARSE_LOG_PARSED)
        );
        feedSessionService.getSessionInserter(0).processAndFlush(feedSessions);

        // then
        verify(ytClient).insertRows("T", BINDING_TABLE.getBinder(), feedSessions.stream()
                .map(YtFeedSession::new)
                .collect(Collectors.toList()));
    }
}

package ru.yandex.market.api.integration;

import java.util.Arrays;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import ru.yandex.market.api.common.ApiConstants;
import ru.yandex.market.api.common.ApiErrorLogger;
import ru.yandex.market.api.test.infrastructure.prerequisites.PrerequisitesRule;
import ru.yandex.market.api.util.concurrent.ApiFuturesHelper;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.concurrent.FuturesConf;
import ru.yandex.market.api.util.parser2.ContentApiParserBuilders;
import ru.yandex.market.api.util.parser2.ParserBuilders;

public abstract class UnitTestBase {

    private static ContentApiParserBuilders parsers;
    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    @Rule
    public PrerequisitesRule prerequisites = new PrerequisitesRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    protected void expectMessage(String... fragments) {
        Arrays.stream(fragments)
            .forEach(m -> exception.expectMessage(m));
    }

    private static final FuturesConf futuresConf = new FuturesConf(
            ApiFuturesHelper.getInstance(),
            ApiConstants.DEFAULT_TIMEOUT,
            ApiConstants.MAX_TIMEOUT,
            ApiErrorLogger::doLog
    );

    private static final ParserBuilders parserBuilders = new ContentApiParserBuilders();

    @BeforeClass
    public static void setUpAll() throws Exception {
        Futures.futuresConf = futuresConf;
        ParserBuilders.Parsers2 = parserBuilders;
    }

    public static void tearDownAll() throws Exception {
        Futures.futuresConf = futuresConf;
        ParserBuilders.Parsers2 = parserBuilders;
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

}

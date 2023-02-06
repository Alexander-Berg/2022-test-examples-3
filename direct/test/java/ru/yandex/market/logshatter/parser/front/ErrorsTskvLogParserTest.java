package ru.yandex.market.logshatter.parser.front;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.util.Date;

import static ru.yandex.market.logshatter.parser.trace.Environment.DEVELOPMENT;

public class ErrorsTskvLogParserTest {
    private LogParserChecker checker;

    @Before
    public void setUp() {
        checker = new LogParserChecker(new ErrorsTskvLogParser());
        checker.setFile("/var/log/yandex/market-skubi/market_front_desktop-errors.log");
        checker.setParam("logbroker://market-health-dev", DEVELOPMENT.toString());
        checker.setOrigin("market-health-dev");
    }

    @Test
    public void parseRequiredFields() throws Exception {
        String line = "tskv\ttimestamp=1502450699257\tservice=market_front_desktop\tcode=Code\tlevel=error\t" +
                "message=ololo";
        checker.check(
            line,
            new Date(1502450699257L), "market_front_desktop", checker.getHost(), "", "Code",
                "error", "ololo", "", "", "", 0, new String[]{}, new String[]{},
                new String[]{}, "", DEVELOPMENT, "server"
        );
    }

    @Test
    public void setServiceFromFileName() throws Exception {
        String line = "tskv\ttimestamp=1502450699257\tservice=market_front_desktop\tcode=Code\tlevel=error\t" +
                "message=ololo";
        checker.setFile("/var/log/yandex/market-skubi/test-errors.log");

        checker.check(
            line,
            new Date(1502450699257L), "test", checker.getHost(), "", "Code",
                "error", "ololo", "", "", "", 0, new String[]{}, new String[]{},
                new String[]{}, "", DEVELOPMENT, "server"
        );
    }

    @Test(expected = ru.yandex.market.logshatter.parser.ParserException.class)
    public void failOnMissingRequiredFields() throws Exception {
        String line = "tskv\ttimestamp=1502450699257\tservice=market_front_desktop\tcode=Code\tlevel=error\t";
        checker.check(line);
    }

    @Test
    public void parseAllFields() throws Exception {
        String line = "tskv\ttimestamp=1502450699257\tservice=market_front_desktop\tcode=Code\tlevel=error\t" +
                "message=ololo\tstack_trace=pishpish\t" +
                "stack_trace_hash=49aae5be6191c82de5e2aafc451b2d894893df2aaefc0293a6fc5ec3f0972fe2\tfile=123.js\t" +
                "line_no=5\ttags=a,b,c\textra_keys=a,b,c\textra_values=1,2,3\trevision=pppp\t" +
                "request_id=ddddd";
        checker.check(
            line,
            new Date(1502450699257L), "market_front_desktop", checker.getHost(), "ddddd", "Code",
                "error", "ololo", "pishpish", "49aae5be6191c82de5e2aafc451b2d894893df2aaefc0293a6fc5ec3f0972fe2",
                "123.js", 5, new String[]{"a", "b", "c"}, new String[]{"a", "b", "c"},
                new String[]{"1", "2", "3"}, "pppp", DEVELOPMENT, "server"
        );
    }
}

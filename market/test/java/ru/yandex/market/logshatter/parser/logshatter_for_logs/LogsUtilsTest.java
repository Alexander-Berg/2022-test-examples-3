package ru.yandex.market.logshatter.parser.logshatter_for_logs;

import de.bwaldvogel.mongo.backend.Assert;
import org.junit.jupiter.api.Test;

public class LogsUtilsTest {
    private static final int CUT_LEN = 10;
    private static final String CUT_MARK = "...";

    @Test
    public void cutShorterMessage() {
        String s = "short";
        s = LogsUtils.cutMessage(s, CUT_LEN, CUT_MARK);
        Assert.equals(s, "short");
    }

    @Test
    public void cutLongerMessage() {
        String s = "longlonglonglonglong";
        s = LogsUtils.cutMessage(s, CUT_LEN, CUT_MARK);
        Assert.equals(s, "longlonglo...");
    }
}

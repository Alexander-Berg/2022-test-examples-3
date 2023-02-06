package ru.yandex.market.checker.check;

import org.apache.http.Header;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.TestHelper;
import ru.yandex.market.checker.EmptyTest;

/**
 * @author imelnikov
 */
public class JSCheckerTest extends EmptyTest {

    @Autowired
    private JSChecker jsChecker;

    @Test
    public void loadJs() {
        var checkerTask = TestHelper.createTask("yandex.ru");
        jsChecker.doCheck(checkerTask, new byte[0], new Header[0], "100000000");
    }
}

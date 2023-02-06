package ru.yandex.autotests.innerpochta.tests.matchers;

import ch.ethz.ssh2.Connection;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.io.IOException;

import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static ru.yandex.autotests.innerpochta.utils.SSHCommands.executeCommAndResturnResultAsString;

/**
 * Created by alex89 on 30.08.18.
 */
public class EmptyLogMatcher extends TypeSafeDiagnosingMatcher<Connection> {

    private static final Logger LOG = LogManager.getLogger(EmptyLogMatcher.class);
    ;
    private String grepCommand;

    public EmptyLogMatcher(String grepCommand) {
        this.grepCommand = grepCommand;
    }

    @Override
    protected boolean matchesSafely(Connection connection, Description description) {
        String grepResult = "";
        try {
            grepResult = executeCommAndResturnResultAsString(connection, grepCommand, LOG).trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (isEmptyOrNullString().matches(grepResult)) {
            description.appendText("Пустой результат выполнения команды " + grepCommand);
            return false;
        }
        return true;
    }


    @Override
    public void describeTo(Description description) {
        description.appendText("Ожидаем непустой результат выполнения команды " + grepCommand);
    }

    public static EmptyLogMatcher hasNoEmptyGrepResult(String grepCommand) {
        return new EmptyLogMatcher(grepCommand);
    }

}

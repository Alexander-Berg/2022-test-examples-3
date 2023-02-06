package ru.yandex.market.deliverycalculator.indexer.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;

/**
 * Базовый класс для тестов TMS-комманд компонента.
 *
 * @author Vladislav Bauer
 */
public abstract class AbstractTmsCommandTest extends FunctionalTest {

    @Autowired
    private Terminal terminal;

    private StringWriter terminalWriter;


    @BeforeEach
    public void setUpConfigurations() {
        terminalWriter = new StringWriter();

        Mockito.when(terminal.getWriter()).thenReturn(Mockito.spy(new PrintWriter(terminalWriter)));
        Mockito.when(terminal.areYouSure()).thenReturn(true);
    }

    protected final CommandInvocation commandInvocation(final String name, final Object... arguments) {
        final String[] args = Arrays.stream(arguments)
                .map(String::valueOf)
                .toArray(String[]::new);

        return new CommandInvocation(name, args, Collections.emptyMap());
    }

    protected final CommandInvocation commandInvocation(final String name, Map<String, String> options) {
        return new CommandInvocation(name, new String[]{}, options);
    }

    protected final Terminal terminal() {
        return terminal;
    }

    protected final String terminalData() {
        final String data = terminalWriter.toString();
        return StringUtils.trimToEmpty(data);
    }

}

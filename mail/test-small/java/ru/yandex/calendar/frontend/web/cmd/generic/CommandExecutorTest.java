package ru.yandex.calendar.frontend.web.cmd.generic;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.misc.reflection.ClassX;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandExecutorTest {
    @Test
    @DisplayName("Check that the lookup for action classes via Spring resolver really works")
    public void findActionClasses() {
        val actionClasses = CommandExecutor.findActionClasses();
        assertThat(actionClasses).contains(ClassX.wrap(CmdTestCmd.class));
    }
}

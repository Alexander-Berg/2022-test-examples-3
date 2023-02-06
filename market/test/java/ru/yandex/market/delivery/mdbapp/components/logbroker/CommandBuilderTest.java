package ru.yandex.market.delivery.mdbapp.components.logbroker;

import java.util.AbstractMap;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CommandBuilderTest {

    public static final String METHOD_NAME = "some_method_name";

    @Test
    public void builder() {
        Assertions.assertThat(Command.builder())
                .as("New Builder instance")
                .isExactlyInstanceOf(Command.CommandBuilder.class);
    }

    @Test
    public void hasCorrectDefaultVersion() {
        Command.CommandBuilder builder = getCommandBuilder();

        Assertions.assertThat(builder.build())
                .as("Command")
                .hasFieldOrPropertyWithValue("version", "1");
    }

    @Test
    public void hasCorrectVersion() {
        Command.CommandBuilder builder = getCommandBuilder().withVersion("2");

        Assertions.assertThat(builder.build())
                .as("Command")
                .hasFieldOrPropertyWithValue("version", "2");
    }

    @Test
    public void hasCorrectMethod() {
        Command.CommandBuilder builder = getCommandBuilder();

        Assertions.assertThat(builder.build())
                .as("Command")
                .hasFieldOrPropertyWithValue("method", METHOD_NAME);
    }

    @Test
    public void hasCorrectParams() {
        Command.CommandBuilder builder = getCommandBuilder();

        Assertions.assertThat(builder.build().getParams())
                .as("Command params")
                .contains(new AbstractMap.SimpleEntry<>("param_name", "param_value"));
    }

    private Command.CommandBuilder getCommandBuilder() {
        return Command.builder()
                .withMethod(METHOD_NAME)
                .addParam("param_name", "param_value");
    }
}

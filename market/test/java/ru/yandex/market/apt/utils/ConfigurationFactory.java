package ru.yandex.market.apt.utils;

import java.util.Arrays;

import javax.annotation.Nonnull;

import ru.yandex.market.apt.Configuration;

public final class ConfigurationFactory {
    private ConfigurationFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static Configuration defaultConfiguration() {
        return new Configuration()
            .setInputDirectories(Arrays.asList("input/dir/1", "input/dir/2", "input/dir/3"))
            .setOutputDirectory("path/to/output/directory")
            .setProcessorClassName("fully.qualified.ProcessorName");
    }
}

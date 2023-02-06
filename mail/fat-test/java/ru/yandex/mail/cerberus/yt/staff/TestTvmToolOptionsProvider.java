package ru.yandex.mail.cerberus.yt.staff;

import ru.yandex.mail.tvmlocal.Utils;
import ru.yandex.mail.tvmlocal.junit_jupiter.TvmToolOptionsProvider;
import ru.yandex.mail.tvmlocal.options.ConfigLocation;
import ru.yandex.mail.tvmlocal.options.Mode;
import ru.yandex.mail.tvmlocal.options.ResourceConfigLocation;
import ru.yandex.mail.tvmlocal.options.TvmToolOptions;

import java.util.OptionalInt;

import static java.util.Collections.emptyMap;

public class TestTvmToolOptionsProvider implements TvmToolOptionsProvider {
    private static final ConfigLocation LOCATION = new ResourceConfigLocation("tvmtool.conf");
    private static final int PORT = Utils.selectRandomPort();
    private static final String AUTH_TOKEN = "01234567890123456789012345678901";

    static {
        System.setProperty("tvmtool-port", String.valueOf(PORT));
    }

    @Override
    public TvmToolOptions getOptions() {
        return new TvmToolOptions(OptionalInt.of(PORT), LOCATION, Mode.REAL, emptyMap(), AUTH_TOKEN);
    }
}

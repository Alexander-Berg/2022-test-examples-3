package ru.yandex.mail.tvmlocal.junit_jupiter;

import lombok.val;
import ru.yandex.mail.tvmlocal.Utils;
import ru.yandex.mail.tvmlocal.options.Mode;
import ru.yandex.mail.tvmlocal.options.ResourceConfigLocation;
import ru.yandex.mail.tvmlocal.options.TvmToolOptions;

import java.util.OptionalInt;

import static java.util.Collections.emptyMap;

public class TestTvmToolOptionsProvider implements TvmToolOptionsProvider {
    public static int PORT = Utils.selectRandomPort();

    @Override
    public TvmToolOptions getOptions() {
        val configLocation = new ResourceConfigLocation("tvmtool.conf");
        val authToken = TvmToolOptions.generateAuthToken();
        return new TvmToolOptions(OptionalInt.of(PORT), configLocation, Mode.UNITTEST, emptyMap(), authToken);
    }
}

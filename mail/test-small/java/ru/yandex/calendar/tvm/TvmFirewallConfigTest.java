package ru.yandex.calendar.tvm;

import java.util.Collections;

import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;

import ru.yandex.calendar.frontend.a3.bind.JsonBinder;
import ru.yandex.calendar.frontend.api.ApiBender;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.misc.bender.BenderMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class TvmFirewallConfigTest {
    private final BenderMapper mapper = new JsonBinder(ApiBender.getConfiguration(), ApiBender.getConfigurationForLogging()).getMapper();

    private TvmFirewallConfig config() {
        val sources = StreamEx.of(ActionSource.DISPLAY, ActionSource.MAILHOOK);
        val configs = StreamEx.of(
                new TvmFirewallConfigEntry(Collections.emptyList(), Collections.emptyList(), true),
                new TvmFirewallConfigEntry(Collections.emptyList(), Collections.emptyList(), false));

        val map = sources.zipWith(configs).toImmutableMap();
        return new TvmFirewallConfig(map);
    }

    @Test
    public void checkSerialization() {
        mapper.serializeJson(config());
    }

    @Test
    public void checkParse() {
        val text = "{\"config\":{\"mailhook\":{\"whiteList\":[],\"blackList\":[],\"allowWithoutTicket\":false},\"display\":{\"whiteList\":[],\"blackList\":[],\"allowWithoutTicket\":true}}}";
        val config = mapper.parseJson(TvmFirewallConfig.class, text);
        assertThat(config).isEqualTo(config());
    }
}

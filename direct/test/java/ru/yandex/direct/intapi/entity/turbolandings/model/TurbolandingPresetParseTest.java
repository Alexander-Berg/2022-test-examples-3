package ru.yandex.direct.intapi.entity.turbolandings.model;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class TurbolandingPresetParseTest {
    @Parameter
    public String presetName;

    @Parameter(1)
    public TurbolandingPreset expected;

    @Parameters(name = "{0}")
    public static Iterable<Object[]> parameters() {
        return StreamEx.of(TurbolandingPreset.values())
                .map(t -> new Object[] {t.getName(), t})
                .append(new Object[] {"unknown", TurbolandingPreset.EMPTY})
                .toList();
    }

    @Test
    public void parseTest() {
        assertThat(TurbolandingPreset.parse(presetName), equalTo(expected));
    }
}

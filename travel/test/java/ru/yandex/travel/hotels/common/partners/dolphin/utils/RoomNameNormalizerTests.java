package ru.yandex.travel.hotels.common.partners.dolphin.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.dolphin.proto.TNormalizeRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Ignore
public class RoomNameNormalizerTests {

    private RoomNameNormalizerRules normalizeRules = mock(RoomNameNormalizerRules.class);

    private RoomNameNormalizer normalizer = new RoomNameNormalizer(normalizeRules);

    private List<TNormalizeRule> getRuleList() throws IOException {
        var json = Resources.toString(Resources.getResource("dolphin-room-renames.json"), StandardCharsets.UTF_8);
        List<List<String>> loaded = new ObjectMapper().readerFor(List.class).readValue(json);
        List<TNormalizeRule> ruleList = new ArrayList<>();
        for (int i = 0; i < loaded.size(); i++) {
            ruleList.add(TNormalizeRule.newBuilder()
                    .setId(i)
                    .setFrom(loaded.get(i).get(0))
                    .setTo(loaded.get(i).get(1))
                    .build());
        }
        return ruleList;
    }

    @Test
    public void testNormalization() throws IOException {
        when(normalizeRules.getAll()).thenReturn(getRuleList());
        assertThat(normalizer.normalize("Номер станд. 2-мест.")).isEqualTo("Номер стандартный 2-местный");
        assertThat(normalizer.normalize("Номер станд., корп. 1 2-мест.")).isEqualTo("Номер стандартный, корпус 1 " +
                "2-местный");
        assertThat(normalizer.normalize("Номер 2-комн. полулюкс 2-мест.")).isEqualTo("Номер 2-комнатный полулюкс " +
                "2-местный");
        assertThat(normalizer.normalize("Номер станд. улучш. (больш. кроват. и кресло), Green Park 2-мест.(разм. в " +
                "3-мест.)"))
                .isEqualTo("Номер стандартный улучшенный (большая кровать и кресло), Green Park 2-местный(размещение " +
                        "в 3-местный)");
    }
}

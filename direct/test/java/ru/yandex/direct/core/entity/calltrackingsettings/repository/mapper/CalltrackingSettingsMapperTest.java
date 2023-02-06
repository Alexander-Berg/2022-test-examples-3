package ru.yandex.direct.core.entity.calltrackingsettings.repository.mapper;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.calltracking.model.SettingsPhone;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
public class CalltrackingSettingsMapperTest {

    @Test
    public void phonesToTrackFromJson_oldFormat() {
        String json = getJsonOldFormat();
        List<SettingsPhone> expected = getPhonesOldFormat();
        List<SettingsPhone> actual = CalltrackingSettingsMapper.phonesToTrackFromJson(json);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void phonesToTrackFromJson_newFormat() {
        String json = getJsonNewFormat();
        List<SettingsPhone> expected = getPhonesNewFormat();
        List<SettingsPhone> actual = CalltrackingSettingsMapper.phonesToTrackFromJson(json);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void phonesToTrackToJson() {
        List<SettingsPhone> phones = getPhonesNewFormat();
        String expected = getJsonNewFormat();
        String actual = CalltrackingSettingsMapper.phonesToTrackToJson(phones);
        assertThat(actual).isEqualTo(expected);
    }

    private String getJsonOldFormat() {
        return "[\"+79000000080\",\"+79000000081\",\"+79000000082\",\"+79000000083\"]";
    }

    private String getJsonNewFormat() {
        return "[" +
                "{\"phone\":\"+79000000080\",\"createTime\":\"2021-02-06 12:00:00\"}," +
                "{\"phone\":\"+79000000081\",\"createTime\":\"2021-02-07 12:00:00\"}," +
                "{\"phone\":\"+79000000082\",\"createTime\":\"2021-02-08 12:00:00\"}," +
                "{\"phone\":\"+79000000083\",\"createTime\":\"2021-02-09 12:00:00\"}" +
                "]";
    }

    private List<SettingsPhone> getPhonesOldFormat() {
        return List.of(
                new SettingsPhone().withPhone("+79000000080").withCreateTime(CalltrackingSettingsMapper.DEFAULT_LAST_UPDATE),
                new SettingsPhone().withPhone("+79000000081").withCreateTime(CalltrackingSettingsMapper.DEFAULT_LAST_UPDATE),
                new SettingsPhone().withPhone("+79000000082").withCreateTime(CalltrackingSettingsMapper.DEFAULT_LAST_UPDATE),
                new SettingsPhone().withPhone("+79000000083").withCreateTime(CalltrackingSettingsMapper.DEFAULT_LAST_UPDATE)
        );
    }


    private List<SettingsPhone> getPhonesNewFormat() {
        return List.of(
                new SettingsPhone()
                        .withPhone("+79000000080")
                        .withCreateTime(LocalDateTime.of(2021, 2, 6, 12, 0)),
                new SettingsPhone()
                        .withPhone("+79000000081")
                        .withCreateTime(LocalDateTime.of(2021, 2, 7, 12, 0)),
                new SettingsPhone()
                        .withPhone("+79000000082")
                        .withCreateTime(LocalDateTime.of(2021, 2, 8, 12, 0)),
                new SettingsPhone()
                        .withPhone("+79000000083")
                        .withCreateTime(LocalDateTime.of(2021, 2, 9, 12, 0))
        );
    }
}

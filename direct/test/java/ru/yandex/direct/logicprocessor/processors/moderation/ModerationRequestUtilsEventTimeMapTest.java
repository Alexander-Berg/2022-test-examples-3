package ru.yandex.direct.logicprocessor.processors.moderation;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.ess.logicobjects.moderation.asset.BannerAssetModerationEventsObject;

import static ru.yandex.direct.logicprocessor.processors.moderation.ModerationRequestUtils.getIdToEventTimeMap;

public class ModerationRequestUtilsEventTimeMapTest {

    private static final Long ID_1 = 2L;
    private static final Long ID_2 = 7L;
    private static final Long ID_3 = 19L;

    private static final Long TIME_1 = 123L;
    private static final Long TIME_2 = 234L;
    private static final Long TIME_3 = 345L;

    @Test
    public void oneEvent() {
        List<BannerAssetModerationEventsObject> events = List.of(
                new BannerAssetModerationEventsObject("", TIME_1, ID_1, false)
        );

        Map<Long, Long> result = getIdToEventTimeMap(events, BannerAssetModerationEventsObject::getBannerId);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(1);
            softly.assertThat(result.get(ID_1)).isEqualTo(TIME_1);
        });
    }

    @Test
    public void twoEventsWithDifferentIds() {
        List<BannerAssetModerationEventsObject> events = List.of(
                new BannerAssetModerationEventsObject("", TIME_1, ID_1, false),
                new BannerAssetModerationEventsObject("", TIME_2, ID_2, false)
        );

        Map<Long, Long> result = getIdToEventTimeMap(events, BannerAssetModerationEventsObject::getBannerId);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(2);
            softly.assertThat(result.get(ID_1)).isEqualTo(TIME_1);
            softly.assertThat(result.get(ID_2)).isEqualTo(TIME_2);
        });
    }

    @Test
    public void severalEventsWithDifferentAndSameIds() {
        Long smallTime = 1238L;
        Long greatestTime = 1239L;
        List<BannerAssetModerationEventsObject> events = List.of(
                new BannerAssetModerationEventsObject("", TIME_1, ID_1, false),
                new BannerAssetModerationEventsObject("", greatestTime, ID_2, false),
                new BannerAssetModerationEventsObject("", TIME_3, ID_3, false),
                new BannerAssetModerationEventsObject("", smallTime, ID_2, false)
        );

        Map<Long, Long> result = getIdToEventTimeMap(events, BannerAssetModerationEventsObject::getBannerId);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(3);
            softly.assertThat(result.get(ID_1)).isEqualTo(TIME_1);
            softly.assertThat(result.get(ID_2)).isEqualTo(greatestTime);
            softly.assertThat(result.get(ID_3)).isEqualTo(TIME_3);
        });
    }

    @Test
    public void timeMustBeGreatestWhenManyEventsWithDuplicatedIds() {
        Long greatestTime = 17238L;
        List<BannerAssetModerationEventsObject> events = List.of(
                new BannerAssetModerationEventsObject("", 123L, ID_1, false),
                new BannerAssetModerationEventsObject("", 178L, ID_1, false),
                new BannerAssetModerationEventsObject("", greatestTime, ID_1, false),
                new BannerAssetModerationEventsObject("", 8232L, ID_1, false),
                new BannerAssetModerationEventsObject("", 0L, ID_1, false),
                new BannerAssetModerationEventsObject("", -1L, ID_1, false),
                new BannerAssetModerationEventsObject("", 1728L, ID_1, false),
                new BannerAssetModerationEventsObject("", 17L, ID_1, false)
        );

        Map<Long, Long> result = getIdToEventTimeMap(events, BannerAssetModerationEventsObject::getBannerId);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(1);
            softly.assertThat(result.get(ID_1)).isEqualTo(greatestTime);
        });
    }

    @Test
    public void emptyEvents() {
        List<BannerAssetModerationEventsObject> events = List.of();

        Map<Long, Long> result = getIdToEventTimeMap(events, BannerAssetModerationEventsObject::getBannerId);
        Assertions.assertThat(result).hasSize(0);
    }

    @Test
    public void noFailOnNullTimes() {
        List<BannerAssetModerationEventsObject> events = List.of(
                new BannerAssetModerationEventsObject("", TIME_1, ID_1, false),
                new BannerAssetModerationEventsObject("", TIME_2, ID_2, false),
                new BannerAssetModerationEventsObject("", null, ID_2, false),
                new BannerAssetModerationEventsObject("", null, ID_3, false)
        );

        Map<Long, Long> result = getIdToEventTimeMap(events, BannerAssetModerationEventsObject::getBannerId);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(2);
            softly.assertThat(result.get(ID_1)).isEqualTo(TIME_1);
            softly.assertThat(result.get(ID_2)).isEqualTo(TIME_2);
        });
    }
}

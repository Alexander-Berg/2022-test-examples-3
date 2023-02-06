package ru.yandex.direct.intapi.entity.metrika.model.objectinfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.intapi.entity.metrika.model.objectinfo.MetrikaObjectInfoResponse.COMPARATOR;

@SuppressWarnings("ConstantConditions")
public class MetrikaObjectInfoComparatorTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    private static final BannerInfo INFO_NOW = new BannerInfo(129394L, null, null, null, null, NOW);
    private static final BannerInfo INFO_PAST = new BannerInfo(3841L, null, null, null, null, NOW.minusMinutes(1));
    private static final BannerInfo INFO_FUTURE = new BannerInfo(1239302L, null, null, null, null, NOW.plusMinutes(1));
    private static final BannerInfo INFO_FUTURE_2 = new BannerInfo(2302404L, null, null, null, null,
            NOW.plusMinutes(1));
    private static final BannerInfo INFO_FUTURE_3 = new BannerInfo(2302404L, null, null, null, null,
            NOW.plusMinutes(1));
    private static final BannerInfo INFO_FUTURE_4 =
            new BannerInfo(22_131_302_404L, null, null, null, null, NOW.plusMinutes(1));

    @Test
    public void compare_FirstDateTimeIsLessThanSecond() {
        assertThat(COMPARATOR.compare(INFO_NOW, INFO_FUTURE), lessThan(0));
    }

    @Test
    public void compare_FirstDateTimeIsGreaterThanSecond() {
        assertThat(COMPARATOR.compare(INFO_NOW, INFO_PAST), greaterThan(0));
    }

    @Test
    public void compare_FirstDateTimeIsEqualToSecondAndFirstIdIsLessThanSecond() {
        assertThat(COMPARATOR.compare(INFO_FUTURE, INFO_FUTURE_2), lessThan(0));
    }

    @Test
    public void compare_FirstDateTimeIsEqualToSecondAndFirstIdIsGreaterThanSecond() {
        assertThat(COMPARATOR.compare(INFO_FUTURE_2, INFO_FUTURE), greaterThan(0));
    }

    @Test
    public void compare_FirstDateTimeIsEqualToSecondAndFirstIdIsEqualToSecond() {
        assertThat(COMPARATOR.compare(INFO_FUTURE_2, INFO_FUTURE_3), equalTo(0));
    }

    @Test
    public void compare_SameDateTimeAndLongSecondId() {
        assertThat(COMPARATOR.compare(INFO_FUTURE_3, INFO_FUTURE_4), lessThan(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sort_ValidOrder() {
        List<BannerInfo> sourceList = new ArrayList<>(
                Arrays.asList(INFO_FUTURE_2, INFO_NOW, INFO_PAST, INFO_FUTURE_3, INFO_FUTURE));
        Collections.sort(sourceList, COMPARATOR);
        assertThat(sourceList, contains(
                sameInstance(INFO_PAST),
                sameInstance(INFO_NOW),
                sameInstance(INFO_FUTURE),
                sameInstance(INFO_FUTURE_2),
                sameInstance(INFO_FUTURE_3)));
    }
}

package ru.yandex.direct.core.entity.bidmodifiers.service;

import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.DEMOGRAPHY_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.DESKTOP_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.DESKTOP_ONLY_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.EXPRESS_CONTENT_DURATION_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.EXPRESS_TRAFFIC_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.GEO_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.MOBILE_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.PRISMA_INCOME_GRADE_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.RETARGETING_FILTER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.RETARGETING_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.SMARTTV_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.TABLET_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.TRAFARET_POSITION_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.VIDEO_MULTIPLIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierType.WEATHER_MULTIPLIER;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class BidModifierServiceIdTypeTest {

    public static Object provideDataForGetRealType() {
        return new Object[][]{
                {99, null},
                {100, MOBILE_MULTIPLIER},
                {110, DEMOGRAPHY_MULTIPLIER},
                {120, RETARGETING_MULTIPLIER},
                {130, GEO_MULTIPLIER},
                {140, VIDEO_MULTIPLIER},
                {190, DESKTOP_MULTIPLIER},
                {200, WEATHER_MULTIPLIER},
                {210, EXPRESS_TRAFFIC_MULTIPLIER},
                {220, EXPRESS_CONTENT_DURATION_MULTIPLIER},
                {230, TRAFARET_POSITION_MULTIPLIER},
                {240, PRISMA_INCOME_GRADE_MULTIPLIER},
                {250, RETARGETING_FILTER},
                {260, SMARTTV_MULTIPLIER},
                {270, TABLET_MULTIPLIER},
                {280, DESKTOP_ONLY_MULTIPLIER},
                {290, null}
        };
    }

    @Test
    @Parameters(method = "provideDataForGetRealType")
    public void getRealType(int var, BidModifierType result) {
        assertEquals(BidModifierService.getRealType(var), result);
    }

    public static Object provideDataForGetRealId() {
        return new Object[][]{
                {99L, null},
                {1234L, 34L},
        };
    }

    @Test
    @Parameters(method = "provideDataForGetRealId")
    public void getRealId(Long externalId, Long id) {
        assertEquals(BidModifierService.getRealId(externalId), id);
    }

    public static Object provideDataForgetExternalId() {
        return new Object[][]{
                {1L, MOBILE_MULTIPLIER, 101L},
                {1L, DEMOGRAPHY_MULTIPLIER, 111L},
                {1L, RETARGETING_MULTIPLIER, 121L},
                {1L, GEO_MULTIPLIER, 131L},
                {1L, DESKTOP_MULTIPLIER, 191L},
                {1L, WEATHER_MULTIPLIER, 201L},
                {1L, SMARTTV_MULTIPLIER, 261L},
                {1L, TABLET_MULTIPLIER, 271L},
                {1L, DESKTOP_ONLY_MULTIPLIER, 281L},
        };
    }

    @Test
    @Parameters(method = "provideDataForgetExternalId")
    public void getExternalId(long realId, BidModifierType type, long externalId) {
        assertEquals((Object) BidModifierService.getExternalId(realId, type), externalId);
    }

    @Test
    public void getRealIdsGroupedByType() {
        List<Long> ids = asList(99L, 100L, 110L, 267L, 2742L, 2843L, 111L, 1234L, 1256L, 13123L, 13456L, 19001L, 20001L);

        Multimap<BidModifierType, Long> expected = ArrayListMultimap.create();
        expected.putAll(MOBILE_MULTIPLIER, ImmutableSet.of(0L));
        expected.putAll(DEMOGRAPHY_MULTIPLIER, ImmutableSet.of(0L, 1L));
        expected.putAll(SMARTTV_MULTIPLIER, ImmutableSet.of(7L));
        expected.putAll(TABLET_MULTIPLIER, ImmutableSet.of(42L));
        expected.putAll(DESKTOP_ONLY_MULTIPLIER, ImmutableSet.of(43L));
        expected.putAll(RETARGETING_MULTIPLIER, ImmutableSet.of(34L, 56L));
        expected.putAll(GEO_MULTIPLIER, ImmutableSet.of(123L, 456L));
        expected.putAll(DESKTOP_MULTIPLIER, ImmutableSet.of(1L));
        expected.putAll(WEATHER_MULTIPLIER, ImmutableSet.of(1L));

        assertEquals(expected, BidModifierService.getRealIdsGroupedByType(ids));
    }
}

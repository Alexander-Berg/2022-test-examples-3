package ru.yandex.direct.ess.router.rules.moderation.banner;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.ess.logicobjects.moderation.banner.BannerModerationEventsObject;
import ru.yandex.direct.ess.router.testutils.BannersTableChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.ess.router.testutils.BannersTableChange.createBannersEvent;

class BannerModerationRuleTest {

    static Stream<Arguments> params() {
        return Stream.of(
                arguments(new CpmAudioBannerModerationRule(), BannersBannerType.cpm_audio, BannersBannerType.text),
                arguments(new CpmBannerModerationRule(), BannersBannerType.cpm_banner, BannersBannerType.text),
                arguments(new ContentPromotionBannerModerationRule(), BannersBannerType.content_promotion,
                        BannersBannerType.text),
                arguments(new CpmGeoPinBannerModerationRule(), BannersBannerType.cpm_geo_pin, BannersBannerType.text));
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("params")
    void mapBinlogEventTest_InsertIntoBanners(BaseBannerModerationRule rule, BannersBannerType bannerType,
                                              BannersBannerType anotherBannerType) {
        List<BannersTableChange> bannersTableChanges = new ArrayList<>();
        BannersTableChange bannerTableChange =
                new BannersTableChange().withBid(1L).withCid(2L).withPid(3L).withBannerType(bannerType);
        bannerTableChange.addInsertedColumn(BANNERS.STATUS_MODERATE, "Ready");
        bannersTableChanges.add(bannerTableChange);

        // баннер черновик не выбирается
        BannersTableChange draftBannerTableChange =
                new BannersTableChange().withBid(2L).withCid(3L).withPid(4L).withBannerType(bannerType);
        draftBannerTableChange.addInsertedColumn(BANNERS.STATUS_MODERATE, "New");
        bannersTableChanges.add(draftBannerTableChange);

        // Отправка на модерацию другого тип баннера
        BannersTableChange anotherBannerTableChange =
                new BannersTableChange().withBid(3L).withCid(4L).withPid(5L).withBannerType(anotherBannerType);
        anotherBannerTableChange.addInsertedColumn(BANNERS.STATUS_MODERATE, "Ready");
        bannersTableChanges.add(anotherBannerTableChange);

        LocalDateTime timestamp = LocalDateTime.now();
        long timestampLong = timestamp.toInstant(ZoneOffset.UTC).toEpochMilli();
        BinlogEvent binlogEvent = createBannersEvent(bannersTableChanges, INSERT, timestamp);
        List<BannerModerationEventsObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        BannerModerationEventsObject[] expected = new BannerModerationEventsObject[]{
                new BannerModerationEventsObject(null, timestampLong, 2L, 3L, 1L, bannerType, false, false)
        };
        assertThat(resultObjects).hasSize(1);
        assertThat(resultObjects).containsExactly(expected);
    }

    @ParameterizedTest
    @MethodSource("params")
    void mapBinlogEventTest_UpdateBanners(BaseBannerModerationRule rule, BannersBannerType bannerType,
                                          BannersBannerType anotherBannerType) {
        List<BannersTableChange> bannersTableChanges = new ArrayList<>();

        // Поля statusModerate нет в списке изменившихся полей
        BannersTableChange bannersTableChangeWithoutStatusModerate =
                new BannersTableChange().withBid(1L).withCid(2L).withPid(3L).withBannerType(bannerType);
        bannersTableChanges.add(bannersTableChangeWithoutStatusModerate);

        // Поле statusModerate поменялось на Ready, только это изменение должно учесться в результате
        BannersTableChange bannersTableChangeWithStatusModerate =
                new BannersTableChange().withBid(2L).withCid(3L).withPid(4L).withBannerType(bannerType);
        bannersTableChangeWithStatusModerate.addChangedColumn(BANNERS.STATUS_MODERATE, "New", "Ready");
        bannersTableChanges.add(bannersTableChangeWithStatusModerate);

        // Поле statusModerate есть в списке изменившихся полей, но его значение не изменилось
        BannersTableChange bannersTableChangeStatusModerateEquals =
                new BannersTableChange().withBid(3L).withCid(4L).withPid(5L).withBannerType(bannerType);
        bannersTableChangeStatusModerateEquals.addChangedColumn(BANNERS.STATUS_MODERATE, "Ready", "Ready");

        BinlogEvent binlogEvent = createBannersEvent(bannersTableChanges, UPDATE);
        List<BannerModerationEventsObject> resultObjects = rule.mapBinlogEvent(binlogEvent);
        BannerModerationEventsObject[] expected = new BannerModerationEventsObject[]{
                new BannerModerationEventsObject(null, null, 3L, 4L, 2L, bannerType, false, false)
        };

        assertThat(resultObjects).hasSize(1);
        assertThat(resultObjects).containsExactly(expected);
    }
}

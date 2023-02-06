package ru.yandex.market.core.outlet.moderation;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.delivery.DeliveryRule;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.OutletVisibility;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleLine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Функциональные тесты для {@link ManageOutletInfoService}.
 *
 * @author ivmelnik
 * @since 12.04.18
 */
@DbUnitDataSet(before = "ManageOutletInfoService.before.csv")
class ManageOutletInfoServiceTest extends FunctionalTest {
    private static final int ACTION_ID = 100500;

    @Autowired
    private ManageOutletInfoService manageOutletInfoService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DbUnitDataSet(before = "shopOutlets.csv")
    void listShopOwnOutletIdsForExport() {
        Set<Long> shopIds = manageOutletInfoService.getShopsWithOwnOutletsForExport();
        assertThat(shopIds, containsInAnyOrder(11L, 12L));
    }

    @Test
    @DbUnitDataSet(before = "shopOutletsForOutletsFilter.csv")
    void listAllShopOwnOutletInfos() {
        List<OutletInfo> outlets = manageOutletInfoService.listShopOwnOutletInfos(
                Arrays.asList(11L, 12L));

        assertThat(outlets, notNullValue());
        assertThat(outlets, containsInAnyOrder(
                hasProperty("id", equalTo(0L)),
                hasProperty("id", equalTo(1L)),
                hasProperty("id", equalTo(2L))
        ));
    }

    @Test
    @DbUnitDataSet(before = "shopOutletsForOutletsFilter.csv")
    void listShopOwnOutletInfos() {
        List<OutletInfo> outlets = manageOutletInfoService.listShopOwnOutletInfos(
                Collections.singletonList(11L));

        assertThat(outlets, notNullValue());
        assertThat(outlets, containsInAnyOrder(
                hasProperty("id", equalTo(0L)),
                hasProperty("id", equalTo(1L))
        ));
    }

    @Test
    @DisplayName("проверка создания оутлета и данных в outlet modification")
    @DbUnitDataSet(after = "ManageOutletInfoService.createOutlet.after.csv")
    void createOutletInfo() {
        final int outletId = 1;
        final OutletInfo outlet = getOutletInfo(outletId, 2, "test", "8009009988", "moscow");
        final long outletCreatedId = manageOutletInfoService.createOutletInfo(ACTION_ID, outlet);
        Assertions.assertEquals(outletId, outletCreatedId);
        Assertions.assertNotNull(getModerationModificationTime(outletId));
    }

    @Test
    @DisplayName("проверка обновления оутлета - нет изменений, но стоит флаг need_moderation")
    @DbUnitDataSet(
            before = "ManageOutletInfoService.updateOutlet.before.csv",
            after = "ManageOutletInfoService.updateOutlet.after.csv")
    void updateOutletInfoNoChanges() {
        final int outletId = 1;
        final OutletInfo outletNew = getOutletInfo(outletId, 2, "test", "8009009988", "moscow");
        final OutletInfo outletOld = getOutletInfo(outletId, 2, "test", "8009009988", "moscow");
        outletOld.setModerationInfo(geModerationInfo());
        manageOutletInfoService.updateOutletInfo(ACTION_ID, outletNew, outletOld);
    }

    @Test
    @DisplayName("проверка обновления оутлета - изменена цена")
    @DbUnitDataSet(
            before = "ManageOutletInfoService.changePrice.before.csv",
            after = "ManageOutletInfoService.changePrice.after.csv")
    void updateOutletInfoPriceChanges() {
        final int outletId = 1;
        final OutletInfo outletNew = getOutletInfo(outletId, 2, "test", "8009009988", "moscow");
        final OutletInfo outletOld = getOutletInfo(outletId, 2, "test", "8009009988", "moscow");
        outletNew.addDeliveryRule(getDeliveryRule(5, 10, 10));
        manageOutletInfoService.updateOutletInfo(ACTION_ID, outletNew, outletOld);
    }

    @Test
    @DisplayName("проверка обновления оутлета - есть изменения и есть непройденная модерация")
    @DbUnitDataSet(
            before = "ManageOutletInfoService.updateOutlet.change.address.moderation.before.csv",
            after = "ManageOutletInfoService.updateOutlet.change.address.after.csv")
    void updateOutletInfoWithChangesModerationExist() {
        final int outletId = 1;
        final OutletInfo outletNew = getOutletInfo(outletId, 2, "test", "8009009988", "moscow");
        final OutletInfo outletOld = getOutletInfo(outletId, 2, "test", "8009009988", "spb");
        outletOld.setModerationInfo(geModerationInfo());
        Instant moderationModificationTimeOld = getModerationModificationTime(outletId);
        manageOutletInfoService.updateOutletInfo(ACTION_ID, outletNew, outletOld);
        Instant moderationModificationTimeNew = getModerationModificationTime(outletId);
        Assertions.assertNotEquals(moderationModificationTimeOld, moderationModificationTimeNew);
    }

    @Test
    @DisplayName("проверка обновления оутлета - есть изменения")
    @DbUnitDataSet(
            before = "ManageOutletInfoService.updateOutlet.change.address.before.csv",
            after = "ManageOutletInfoService.updateOutlet.change.address.after.csv")
    void updateOutletInfoWithChangesNoModeration() {
        final int outletId = 1;
        final OutletInfo outletNew = getOutletInfo(outletId, 2, "test", "8009009988", "moscow");
        final OutletInfo outletOld = getOutletInfo(outletId, 2, "test", "8009009977", "moscow");
        manageOutletInfoService.updateOutletInfo(ACTION_ID, outletNew, outletOld);
        Assertions.assertNotNull(getModerationModificationTime(outletId));
    }

    @Nonnull
    private OutletInfo getOutletInfo(long id, long ds, String name, String phoneNumber, String city) {
        final OutletType type = OutletType.MIXED;
        final OutletInfo outlet = new OutletInfo(id, ds, type, name, true, type.toString());
        final Address address = new Address.Builder().setCity(city).build();
        outlet.setAddress(address);

        final PhoneNumber phone = PhoneNumber.builder()
                .setCountry("7")
                .setCity("495")
                .setNumber(phoneNumber)
                .setExtension("7")
                .setPhoneType(PhoneType.PHONE).build();
        outlet.addPhone(phone);

        final ScheduleLine scheduleLine = new ScheduleLine(
                ScheduleLine.DayOfWeek.MONDAY,
                5, 0,
                (int) Duration.ofHours(8).toMinutes()
        );
        final Schedule schedule = new Schedule(1, List.of(scheduleLine));
        outlet.setSchedule(schedule);
        outlet.setEmails(List.of("test@test.ru"));
        final Coordinates coordinates = new Coordinates(50, 50);
        outlet.setGeoInfo(new GeoInfo(coordinates, 1L));
        outlet.setHidden(OutletVisibility.VISIBLE);
        outlet.setActualizationDate(new Date());
        outlet.setStatus(OutletStatus.AT_MODERATION);
        return outlet;
    }

    private DeliveryRule getDeliveryRule(int priceFrom, int priceTo, int cost) {
        return new DeliveryRule(BigDecimal.valueOf(priceFrom), BigDecimal.valueOf(priceTo), BigDecimal.valueOf(cost));
    }

    private ModerationInfo geModerationInfo() {
        return new ModerationInfo("test", ModerationLevel.MODERATION_LEVEL_1);
    }

    private Instant getModerationModificationTime(long outletId) {
        return jdbcTemplate.queryForObject("" +
                        "select moderation_modification_time " +
                        "from shops_web.outlet_modification where outlet_id = ?",
                Timestamp.class,
                outletId
        ).toInstant();
    }
}

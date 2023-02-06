package ru.yandex.direct.jobs.bannersystem.dataimport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.entity.ContentType;
import org.assertj.core.api.Assertions;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.bannersystem.BannerSystemClient;
import ru.yandex.direct.bannersystem.BsHostType;
import ru.yandex.direct.bannersystem.BsUriFactory;
import ru.yandex.direct.bannersystem.container.exporttable.HolidayInfoRecord;
import ru.yandex.direct.core.entity.timetarget.model.HolidayItem;
import ru.yandex.direct.core.entity.timetarget.service.ProductionCalendarProviderService;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.MockedHttpWebServerExtention;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static ru.yandex.direct.bannersystem.BsUriFactory.EXPORT_TABLE;


@JobsTest
@ExtendWith(SpringExtension.class)
class HolidaySynchronizerJobTest {

    private static class TestHolidayInfo {

        private Integer virtualWeekday;
        private String updateTime;
        private Long regionId;

        TestHolidayInfo(Integer virtualWeekday, String updateTime, Long regionId) {
            this.virtualWeekday = virtualWeekday;
            this.updateTime = updateTime;
            this.regionId = regionId;
        }

        @JsonValue
        public JsonNode toJson() {
            return JsonUtils.getObjectMapper().createObjectNode()
                    .put("VirtualWeekday", virtualWeekday.toString())
                    .put("UpdateTime", updateTime)
                    .put("RegionID", regionId.toString());
        }

    }

    private static TestHolidayInfo getValidTestHolidayInfo() {
        return new TestHolidayInfo(
                HolidayInfoRecord.VirtualWeekdayType.HOLIDAY.getCode(),
                LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern(HolidayInfoRecord.DATE_FORMAT_PATTERN)),
                Region.RUSSIA_REGION_ID);
    }

    private HolidayItem getValidTestHolidayItem() {
        return new HolidayItem(
                Region.RUSSIA_REGION_ID,
                LocalDate.now(),
                job.toHolidayItemType(HolidayInfoRecord.VirtualWeekdayType.HOLIDAY)
        );
    }

    private static TestHolidayInfo getInvalidTestHolidayInfo() {
        return new TestHolidayInfo(
                HolidayInfoRecord.VirtualWeekdayType.HOLIDAY.getCode(),
                LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern(HolidayInfoRecord.DATE_FORMAT_PATTERN)),
                Region.GLOBAL_REGION_ID);
    }

    private static final HolidayItem futureHoliday =
            new HolidayItem(
                    Region.RUSSIA_REGION_ID,
                    LocalDate.now().plusDays(1),
                    HolidayItem.Type.HOLIDAY);

    @Autowired
    private ProductionCalendarProviderService productionCalendarProviderService;

    @Autowired
    private AsyncHttpClient asyncHttpClient;

    @RegisterExtension
    static MockedHttpWebServerExtention server = new MockedHttpWebServerExtention(ContentType.APPLICATION_JSON);

    private HolidaySynchronizerJob job;

    @BeforeEach
    void before() {
        server.addResponse(EXPORT_TABLE.getUrlPath() + "?table_name=Holiday",
                JsonUtils.toJson(
                        Arrays.asList(
                                getValidTestHolidayInfo(),
                                getInvalidTestHolidayInfo())));
        EnumMap<BsHostType, String> urlsMap = new EnumMap<>(BsHostType.class);
        urlsMap.put(BsHostType.EXPORT, server.getServerURL());
        BsUriFactory bsUriFactory = new BsUriFactory(urlsMap);
        BannerSystemClient bannerSystemClient = new BannerSystemClient(bsUriFactory, asyncHttpClient);
        job = new HolidaySynchronizerJob(bannerSystemClient, productionCalendarProviderService);

        final HolidayItem zeroDateItem = new HolidayItem(Region.RUSSIA_REGION_ID, null, HolidayItem.Type.HOLIDAY);
        final List<HolidayItem> initialHolidayItems = singletonList(zeroDateItem);
        productionCalendarProviderService.updateHolidays(initialHolidayItems);
    }

    @Test
    void shouldAddOnlyValidHolidays() {
        job.execute();
        final List<HolidayItem> allHolidays = productionCalendarProviderService.getAllHolidays();
        Assertions.assertThat(allHolidays).as("Добавили запись")
                .contains(getValidTestHolidayItem());
    }

    @Test
    void unsynchronizedFutureHolidaysShouldBeDeleted() {
        productionCalendarProviderService.updateHolidays(singletonList(futureHoliday));
        assertThat("Добавили в базу Директа праздник в будущем",
                productionCalendarProviderService.getAllHolidays(),
                hasItem(futureHoliday));
        job.execute();
        final List<HolidayItem> allHolidays = productionCalendarProviderService.getAllHolidays();
        assertSoftly(softly -> {
            softly.assertThat(allHolidays).as("Добавили запись")
                    .contains(getValidTestHolidayItem());
            softly.assertThat(allHolidays).as("Несихронизированная дата из будущего удалена")
                    .doesNotContain(futureHoliday);
        });
    }

    @Test
    void synchronizedHolidaysShouldNotBeDeleted() {
        // 1st synchronization
        job.execute();
        final List<HolidayItem> allHolidays = productionCalendarProviderService.getAllHolidays();
        Assertions.assertThat(allHolidays).as("Запись добавлена")
                        .contains(getValidTestHolidayItem());

        // 2nd synchronization
        job.execute();
        assertThat("Синхронизированная дата не удалена",
                productionCalendarProviderService.getAllHolidays(),
                is(allHolidays));
    }

    @Test
    void holidayTypeShouldBeUpdated() {
        // 1st synchronization
        job.execute();
        final HolidayItem.Type[] types = HolidayItem.Type.values();
        // change holiday type in DB
        final List<HolidayItem> updatedHolidayItems = Stream.of(getValidTestHolidayItem())
                .map(h -> new HolidayItem(
                        h.getRegionId(),
                        h.getDate(),
                        types[(h.getType().ordinal() + 1) % types.length]))
                .collect(toList());
        productionCalendarProviderService.updateHolidays(updatedHolidayItems);
        assertThat("Данные в БД изменены", productionCalendarProviderService.getAllHolidays(),
                not(hasItem(getValidTestHolidayItem())));
        // 2nd synchronization
        job.execute();
        assertThat("Данные синхронизированы",
                productionCalendarProviderService.getAllHolidays(),
                hasItem(getValidTestHolidayItem()));
    }

    @ParameterizedTest
    @EnumSource(HolidayInfoRecord.VirtualWeekdayType.class)
    void allVirtualWeekdayTypeEnumValuesShouldConvertToHolidayItemType(HolidayInfoRecord.VirtualWeekdayType type) {
        Assertions.assertThat(job.toHolidayItemType(type)).isNotNull();
    }

    @Test
    void emptyBannerSystemResponseShouldNotCauseDeletingFutureHolidays() {
        // 1st synchronization
        job.execute();
        final List<HolidayItem> initialHolidays = productionCalendarProviderService.getAllHolidays();
        // emulate empty BannerSystem response
        server.clear();
        server.addResponse(EXPORT_TABLE.getUrlPath() + "?table_name=Holiday",
                JsonUtils.toJson(emptyList()));
        // 2nd synchronization
        job.execute();
        assertThat("Данные не удалены",
                productionCalendarProviderService.getAllHolidays(),
                hasItems(initialHolidays.toArray(new HolidayItem[0])));
    }

}

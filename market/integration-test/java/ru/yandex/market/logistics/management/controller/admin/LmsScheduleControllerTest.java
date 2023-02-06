package ru.yandex.market.logistics.management.controller.admin;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.entity.request.schedule.ScheduleDayFilter;
import ru.yandex.market.logistics.management.service.client.ScheduleService;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_SCHEDULE;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_SCHEDULE_EDIT;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;

@CleanDatabase
@DatabaseSetup("/data/controller/admin/schedule/prepare_data.xml")
class LmsScheduleControllerTest extends AbstractContextualTest {

    private static final String SCHEDULE_URL = "/admin/lms/schedule";
    private static final String SCHEDULE_DATA_PATH = "data/controller/admin/schedule/";
    private static final String PARENT_DATA = "?parentSlug=partner-relation&idFieldName=intakeSchedule&parentId=1";

    private static final long SCHEDULE_ID_FOR_UPDATE = 2L;
    private static final long SCHEDULE_DAY_ID_FOR_UPDATE = 6L;
    private static final long SCHEDULE_DAY_WITH_MAIN_ATTRIBUTE_ID_FOR_UPDATE = 3L;

    private static final ScheduleDay SCHEDULE_DAY_UPDATED = getUpdatedScheduleDay();
    private static final ScheduleDay SCHEDULE_DAY_FOR_UPDATE = getScheduleDayForUpdate();

    @Autowired
    private ScheduleService scheduleService;

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE})
    void scheduleGridIsOk() throws Exception {
        getScheduleGrid("1")
            .andExpect(status().isOk())
            .andExpect(testJson("schedule_grid_is_ok.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE})
    void scheduleGridWithoutMainAttributeIsOk() throws Exception {
        getScheduleGrid("2")
            .andExpect(status().isOk())
            .andExpect(testJson("schedule_grid_without_main_attribute_is_ok.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE})
    void scheduleGridIsEmptyWithoutScheduleId() throws Exception {
        getScheduleGrid("")
            .andExpect(status().isOk())
            .andExpect(testJson("empty_schedule_grid_is_ok.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE})
    void scheduleGridIsForbiddenWithIncorrectScheduleId() throws Exception {
        getScheduleGrid("incorrectScheduleId")
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void getNewScheduleViewIsOkWithParentData() throws Exception {
        getNewSchedule(PARENT_DATA)
            .andExpect(status().isOk())
            .andExpect(testJson("new_schedule_view_is_ok.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void getNewScheduleViewIsOkWithoutParentData() throws Exception {
        getNewSchedule(null)
            .andExpect(status().isOk())
            .andExpect(testJson("new_schedule_view_is_ok.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void createScheduleIsOkWithParentData() throws Exception {
        createSchedule("create_schedule_request_valid.json", PARENT_DATA)
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/admin/lms/partner-relation/1"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void createScheduleIsForbiddenWithoutParentData() throws Exception {
        createSchedule("create_schedule_request_valid.json", null)
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void createScheduleIsForbiddenWithUnknownParentData() throws Exception {
        createSchedule("create_schedule_request_valid.json", "?parentSlug=UNKNOWN&idFieldName=UNKNOWN&parentId=100500")
            .andExpect(status().isBadRequest());
    }

    /**
     * Расписание с id=1 - это расписание отгрузки для связки партнеров с id=1
     * для которого мы пытаемся создать расписание.
     */
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void createScheduleIsForbiddenWithInvalidTimeInterval() throws Exception {
        Set<ScheduleDay> scheduleDaysBefore =
            new TreeSet<>(scheduleService.getScheduleDaysByScheduleId(1L));

        createSchedule("create_schedule_request_invalid.json", PARENT_DATA)
            .andExpect(status().isBadRequest());

        Set<ScheduleDay> scheduleDaysAfter =
            new TreeSet<>(scheduleService.getScheduleDaysByScheduleId(1L));

        Assert.assertEquals(scheduleDaysBefore, scheduleDaysAfter);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE})
    void scheduleDetailsIsOkWithoutParentData() throws Exception {
        getScheduleDayDetails(SCHEDULE_DAY_ID_FOR_UPDATE, null)
            .andExpect(status().isOk())
            .andExpect(testJson("schedule_day_details_is_ok.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE})
    void scheduleDetailsIsOkWithParentData() throws Exception {
        getScheduleDayDetails(SCHEDULE_DAY_ID_FOR_UPDATE, PARENT_DATA)
            .andExpect(status().isOk())
            .andExpect(testJson("schedule_day_details_is_ok.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE})
    void scheduleDetailsWithMainAttributeIsOk() throws Exception {
        getScheduleDayDetails(SCHEDULE_DAY_WITH_MAIN_ATTRIBUTE_ID_FOR_UPDATE, null)
            .andExpect(status().isOk())
            .andExpect(testJson("schedule_day_details_with_main_attributes_is_ok.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void updateScheduleIsOkWithParentData() throws Exception {
        ScheduleDay scheduleDayBefore = scheduleService.getScheduleDayOrThrow(SCHEDULE_DAY_ID_FOR_UPDATE);

        updateSchedule(SCHEDULE_DAY_ID_FOR_UPDATE, "update_schedule_day_request_valid.json", PARENT_DATA)
            .andExpect(status().isOk())
            .andExpect(testJson("update_schedule_day_is_ok.json"));

        ScheduleDay scheduleDayAfter = scheduleService.getScheduleDayOrThrow(SCHEDULE_DAY_ID_FOR_UPDATE);

        Assert.assertEquals(scheduleDayBefore, SCHEDULE_DAY_FOR_UPDATE);
        Assert.assertEquals(scheduleDayAfter, SCHEDULE_DAY_UPDATED);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void updateScheduleIsOkWithoutParentData() throws Exception {
        ScheduleDay scheduleDayBefore = scheduleService.getScheduleDayOrThrow(SCHEDULE_DAY_ID_FOR_UPDATE);

        updateSchedule(SCHEDULE_DAY_ID_FOR_UPDATE, "update_schedule_day_request_valid.json", null)
            .andExpect(status().isOk())
            .andExpect(testJson("update_schedule_day_is_ok.json"));

        ScheduleDay scheduleDayAfter = scheduleService.getScheduleDayOrThrow(SCHEDULE_DAY_ID_FOR_UPDATE);

        Assert.assertEquals(scheduleDayBefore, SCHEDULE_DAY_FOR_UPDATE);
        Assert.assertEquals(scheduleDayAfter, SCHEDULE_DAY_UPDATED);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void updateScheduleWithMainAttributeIsOk() throws Exception {
        ScheduleDay scheduleDayBefore = scheduleService
            .getScheduleDayOrThrow(SCHEDULE_DAY_WITH_MAIN_ATTRIBUTE_ID_FOR_UPDATE);

        updateSchedule(
            SCHEDULE_DAY_WITH_MAIN_ATTRIBUTE_ID_FOR_UPDATE,
            "update_schedule_day_with_main_attribute_request_valid.json",
            null
        )
            .andExpect(status().isOk())
            .andExpect(testJson("update_schedule_day_with_main_attribute_is_ok.json"));

        ScheduleDay scheduleDayAfter = scheduleService
            .getScheduleDayOrThrow(SCHEDULE_DAY_WITH_MAIN_ATTRIBUTE_ID_FOR_UPDATE);

        Assert.assertEquals(
            scheduleDayBefore,
            new ScheduleDay()
                .setId(SCHEDULE_DAY_WITH_MAIN_ATTRIBUTE_ID_FOR_UPDATE)
                .setDay(4)
                .setFrom(LocalTime.of(13, 0, 0))
                .setTo(LocalTime.of(14, 0, 0))
                .setIsMain(true)
        );
        Assert.assertEquals(
            scheduleDayAfter,
            new ScheduleDay()
                .setId(SCHEDULE_DAY_WITH_MAIN_ATTRIBUTE_ID_FOR_UPDATE)
                .setDay(4)
                .setFrom(LocalTime.of(14, 0, 0))
                .setTo(LocalTime.of(15, 0, 0))
                .setIsMain(true)
        );
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void updateScheduleDayIsForbiddenWithInvalidTimeInterval() throws Exception {
        ScheduleDay scheduleDayBefore = scheduleService.getScheduleDayOrThrow(SCHEDULE_DAY_ID_FOR_UPDATE);

        updateSchedule(SCHEDULE_DAY_ID_FOR_UPDATE, "update_schedule_day_request_invalid.json", PARENT_DATA)
            .andExpect(status().isBadRequest());

        ScheduleDay scheduleDayAfter = scheduleService.getScheduleDayOrThrow(SCHEDULE_DAY_ID_FOR_UPDATE);

        Assert.assertEquals(scheduleDayBefore, SCHEDULE_DAY_FOR_UPDATE);
        Assert.assertEquals(scheduleDayBefore, scheduleDayAfter);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void deleteScheduleIsOkWithParentData() throws Exception {
        ScheduleDay scheduleDayBefore = scheduleService.getScheduleDayOrThrow(SCHEDULE_DAY_ID_FOR_UPDATE);
        Set<ScheduleDay> scheduleDaysBefore =
            new TreeSet<>(scheduleService.getScheduleDaysByScheduleId(SCHEDULE_ID_FOR_UPDATE));

        deleteSchedule(PARENT_DATA)
            .andExpect(status().isOk())
            .andExpect(header().string("location", "http://localhost/admin/lms/partner-relation/1"));

        checkScheduleAfterDelete(scheduleDayBefore, scheduleDaysBefore);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void deleteScheduleIsOkWithoutParentData() throws Exception {
        ScheduleDay scheduleDayBefore = scheduleService.getScheduleDayOrThrow(SCHEDULE_DAY_ID_FOR_UPDATE);
        Set<ScheduleDay> scheduleDaysBefore =
            new TreeSet<>(scheduleService.getScheduleDaysByScheduleId(SCHEDULE_ID_FOR_UPDATE));

        deleteSchedule(null)
            .andExpect(status().isOk());

        checkScheduleAfterDelete(scheduleDayBefore, scheduleDaysBefore);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void deleteScheduleDaysActionIsOk() throws Exception {
        List<ScheduleDay> scheduleDaysForDelete = scheduleService.getScheduleDays(new ScheduleDayFilter(
            Set.of(5L, SCHEDULE_DAY_ID_FOR_UPDATE)));
        Set<ScheduleDay> scheduleDaysBefore =
            new TreeSet<>(scheduleService.getScheduleDaysByScheduleId(SCHEDULE_ID_FOR_UPDATE));

        deleteScheduleDays("delete_schedule_days_action.json")
            .andExpect(status().isOk());

        Set<ScheduleDay> scheduleDaysAfter =
            new TreeSet<>(scheduleService.getScheduleDaysByScheduleId(SCHEDULE_ID_FOR_UPDATE));

        Assert.assertEquals(2, scheduleDaysForDelete.size());
        Assert.assertTrue(scheduleDaysBefore.removeAll(scheduleDaysForDelete));
        Assert.assertEquals(scheduleDaysBefore, scheduleDaysAfter);
    }

    @DisplayName("Успешная работа с основным расписанием")
    @MethodSource("getMainScheduleOkArguments")
    @ParameterizedTest(name = AbstractContextualTest.TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DatabaseSetup("/data/controller/admin/schedule/main/main_schedule_prepare_data.xml")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void mainScheduleOk(
        @SuppressWarnings("unused") String caseName,
        Consumer<LmsScheduleControllerTest> action,
        String expectedJsonFile
    ) throws Exception {
        action.accept(this);

        getScheduleGrid("1001").andExpect(status().isOk()).andExpect(testJson(expectedJsonFile));
    }

    @Nonnull
    private static Stream<Arguments> getMainScheduleOkArguments() throws Exception {
        return Stream.<Triple<String, Consumer<LmsScheduleControllerTest>, String>>of(
            Triple.of(
                "Добавление интервала для дня, в котором нет основного интервала",
                c -> c.createSchedule("main/create_schedule_friday_13-14.json", PARENT_DATA),
                "main/schedule_day_for_day_without_main_schedule.json"
            ),
            Triple.of(
                "Добавление интервалов для двух дней, в которых нет основных интервалов",
                c -> {
                    c.createSchedule("main/create_schedule_thursday_13-14.json", PARENT_DATA);
                    c.createSchedule("main/create_schedule_friday_13-14.json", PARENT_DATA);
                },
                "main/two_schedule_days_for_two_days_without_main_schedule.json"
            ),
            Triple.of(
                "Добавление двух интервалов для дня, в котором нет основного интервала",
                c -> {
                    c.createSchedule("main/create_schedule_friday_13-14.json", PARENT_DATA);
                    c.createSchedule("main/create_schedule_friday_15-16.json", PARENT_DATA);
                },
                "main/two_schedule_days_for_day_without_main_schedule.json"
            ),
            Triple.of(
                "Добавление интервала для дня, в котором уже есть основной интервал",
                c -> c.createSchedule("main/create_schedule_monday_9-10.json", PARENT_DATA),
                "main/schedule_day_for_day_with_main_schedule.json"
            ),
            Triple.of(
                "Делаем основными другие интервалы",
                c -> c.makeMainScheduleDays("main/make_main_schedule_monday_13-14_and_tuesday_13-14.json", PARENT_DATA),
                "main/made_main_schedule_monday_13-14_and_tuesday_13-14.json"
            ),
            Triple.of(
                "Удаляем единственный интервал для дня, в котором больше нет интервалов",
                c -> c.deleteScheduleDays("main/delete_only_one_schedule_day_wednesday_13-14.json"),
                "main/deleted_only_one_schedule_day_wednesday_13-14.json"
            ),
            Triple.of(
                "Удаляем основной интервал для дня, в котором есть другие интервалы. Основным становится другой",
                c -> c.deleteScheduleDays("main/delete_main_schedule_day_tuesday_17-18.json"),
                "main/deleted_main_schedule_day_tuesday_17-18.json"
            ),
            Triple.of(
                "Удаляем НЕосновной интервал для дня, в котором есть другие интервалы. Основной остается прежним",
                c -> c.deleteScheduleDays("main/delete_main_schedule_day_tuesday_15-16.json"),
                "main/deleted_main_schedule_day_tuesday_15-16.json"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @Test
    @DisplayName("Ошибка при попытке сделать основными два интервала для одного дня")
    @DatabaseSetup("/data/controller/admin/schedule/main/main_schedule_prepare_data.xml")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_SCHEDULE_EDIT})
    void mainScheduleErrorOnMakeMainTwoScheduleDaysForOneDay() throws Exception {
        makeMainScheduleDays("main/make_main_schedule_monday_13-14_and_15-16.json", PARENT_DATA)
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Только один интервал может быть основным для дня: понедельник"));
    }

    private void checkScheduleAfterDelete(ScheduleDay scheduleDayBefore, Set<ScheduleDay> scheduleDaysBefore) {
        ScheduleDay scheduleDayAfter = scheduleService.getScheduleDay(SCHEDULE_DAY_ID_FOR_UPDATE).orElse(null);
        Set<ScheduleDay> scheduleDaysAfter =
            new TreeSet<>(scheduleService.getScheduleDaysByScheduleId(SCHEDULE_ID_FOR_UPDATE));

        Assert.assertEquals(scheduleDayBefore, SCHEDULE_DAY_FOR_UPDATE);
        Assert.assertNull(scheduleDayAfter);

        Assert.assertTrue(scheduleDaysBefore.remove(scheduleDayBefore));
        Assert.assertEquals(scheduleDaysBefore, scheduleDaysAfter);
    }

    private ResultActions getScheduleGrid(String scheduleId) throws Exception {
        return mockMvc.perform(
            get(SCHEDULE_URL + "?scheduleId=" + scheduleId)
        );
    }

    private ResultActions getScheduleDayDetails(long scheduleDayId, String parentData) throws Exception {
        return mockMvc.perform(
            get(appendParentData(SCHEDULE_URL + "/" + scheduleDayId, parentData))
        );
    }

    private ResultActions getNewSchedule(String parentData) throws Exception {
        return mockMvc.perform(
            get(appendParentData(SCHEDULE_URL + "/new", parentData))
        );
    }

    private ResultActions updateSchedule(long scheduleDayId, String filename, String parentData) throws Exception {
        return mockMvc.perform(
            put(appendParentData(SCHEDULE_URL + "/" + scheduleDayId, parentData))
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(SCHEDULE_DATA_PATH + filename))
        );
    }

    private ResultActions deleteSchedule(String parentData) throws Exception {
        return mockMvc.perform(
            delete(appendParentData(SCHEDULE_URL + "/" + SCHEDULE_DAY_ID_FOR_UPDATE, parentData))
        );
    }

    @SneakyThrows
    private ResultActions deleteScheduleDays(String filename) {
        return mockMvc.perform(
            post(SCHEDULE_URL + LMSPlugin.SLUG_SCHEDULE_DELETE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(SCHEDULE_DATA_PATH + filename))
        );
    }

    @SneakyThrows
    private ResultActions createSchedule(String filename, String parentData) {
        return mockMvc.perform(
            post(appendParentData(SCHEDULE_URL, parentData))
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(SCHEDULE_DATA_PATH + filename))
        );
    }

    @SneakyThrows
    private ResultActions makeMainScheduleDays(String filename, String parentData) {
        return mockMvc.perform(
            post(appendParentData(SCHEDULE_URL + LMSPlugin.SLUG_SCHEDULE_MAKE_MAIN, parentData))
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(SCHEDULE_DATA_PATH + filename))
        );
    }

    private String appendParentData(String urlTemplate, String parentData) {
        if (StringUtils.isEmpty(parentData)) {
            return urlTemplate;
        }
        return urlTemplate + parentData;
    }

    private ResultMatcher testJson(String filename) {
        return TestUtil.testJson(SCHEDULE_DATA_PATH + filename, true);
    }

    private static ScheduleDay getUpdatedScheduleDay() {
        return new ScheduleDay()
            .setId(SCHEDULE_DAY_ID_FOR_UPDATE)
            .setDay(7)
            .setFrom(LocalTime.of(13, 0, 0))
            .setTo(LocalTime.of(13, 0, 0))
            .setIsMain(true);
    }

    private static ScheduleDay getScheduleDayForUpdate() {
        return new ScheduleDay()
            .setId(SCHEDULE_DAY_ID_FOR_UPDATE)
            .setDay(7)
            .setFrom(LocalTime.of(17, 0, 0))
            .setTo(LocalTime.of(17, 0, 0))
            .setIsMain(true);
    }
}

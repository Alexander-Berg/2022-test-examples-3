package ru.yandex.market.logistics.management.controller.admin;

import java.io.InputStream;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerRoute;
import ru.yandex.market.logistics.management.domain.entity.Schedule;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("/data/controller/admin/partner/prepare_data.xml")
@SuppressWarnings({"checkstyle:MagicNumber"})
class SupportControllerPartnerTest extends AbstractContextualTest {

    @Autowired
    private PartnerRepository partnerRepository;

    @TestFactory
    Collection<DynamicTest> dynamicTests() {
        return Arrays.asList(
            DynamicTest.dynamicTest("Routes exist and properly loaded",
                () -> testTemplate("2/routes", "csv/existing_routes.json")),

            DynamicTest.dynamicTest("Routes not exist and empty json properly loaded",
                () -> testTemplate("1/routes", "csv/empty_list.json"))
        );
    }

    void testTemplate(String path, String filename) throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(String.format("/support/lms/partner/%s", path))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson(String.format("data/controller/admin/partner/%s", filename)));
    }

    @Test
    void testRoutesProperlyUpdated() throws Exception {
        InputStream is = new ClassPathResource("data/controller/admin/partner/csv/new_routes_ok.csv")
            .getInputStream();
        Partner partner = performRequestToPartnerWithRoutes(is);

        var scheduleDays = getScheduleDays(partner);
        softly.assertThat(scheduleDays).as("All days should be added").hasSize(29);
        assertAllDaysHaveTimeFromMidnightToMidnight(scheduleDays);

        softly.assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "schedule_day"))
            .as("Only new records should exist in table schedule_days").isEqualTo(29);
        softly.assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "partner_route"))
            .as("Only new records should exist in table partner_route").isEqualTo(6);
    }

    @Test
    void testRoutesProperlyUpdatedDuplicatesIgnored() throws Exception {
        InputStream is = new ClassPathResource("data/controller/admin/partner/csv/new_routes_duplicates.csv")
            .getInputStream();
        Partner partner = performRequestToPartnerWithRoutes(is);

        var scheduleDays = getScheduleDays(partner);
        softly.assertThat(scheduleDays).as("Only unique days remain").hasSize(7);
        assertAllDaysHaveTimeFromMidnightToMidnight(scheduleDays);

        softly.assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "schedule_day"))
            .as("Only unique records should exist in table schedule_days").isEqualTo(7);
        softly.assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "partner_route"))
            .as("Only unique records should exist in table partner_route").isEqualTo(1);
    }

    @Test
    void testRoutesNotUpdatedWhenIncorrectCsv() throws Exception {
        InputStream is = new ClassPathResource("data/controller/admin/partner/csv/new_routes_fail.csv")
            .getInputStream();
        MockMultipartFile upload = new MockMultipartFile("file", is);
        mockMvc.perform(MockMvcRequestBuilders.multipart("/support/lms/partner/2/routes")
            .file(upload))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Cant parse csv lines: 1, 2, 3, 4"));

        Partner partner = partnerRepository.findWithRoutesById(2L).orElse(null);
        softly.assertThat(partner)
            .as("Partner should not be null").isNotNull();

        softly.assertThat(partner.getPartnerRoutes()).extracting(PartnerRoute::getLocationTo)
            .as("Initial routes should be replaced").containsOnly(225);

        softly.assertThat(getScheduleDays(partner)).as("All days should be added").hasSize(9);

        softly.assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "schedule_day"))
            .as("Initial amount should remain in table schedule_days").isEqualTo(12);
        softly.assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "partner_route"))
            .as("Initial amount should remain in table partner_route").isEqualTo(2);
    }

    @Test
    void testNewRoutesCreated() throws Exception {
        InputStream is = new ClassPathResource("data/controller/admin/partner/csv/new_routes_ok.csv")
            .getInputStream();
        MockMultipartFile upload = new MockMultipartFile("file", is);
        mockMvc.perform(MockMvcRequestBuilders.multipart("/support/lms/partner/4/routes")
            .file(upload))
            .andExpect(status().isOk());

        Partner partner = partnerRepository.findWithRoutesById(4L).orElse(null);
        softly.assertThat(partner)
            .as("Partner should not be null").isNotNull();

        var scheduleDays = getScheduleDays(partner);
        softly.assertThat(scheduleDays).as("All days should be added").hasSize(29);
        assertAllDaysHaveTimeFromMidnightToMidnight(scheduleDays);
        softly.assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "schedule_day"))
            .as("Both Initial amount and new should exist in table schedule_days").isEqualTo(41);
        softly.assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "partner_route"))
            .as("Both Initial amount and new should exist in table partner_route").isEqualTo(8);
    }

    private Partner performRequestToPartnerWithRoutes(InputStream is) throws Exception {
        MockMultipartFile upload = new MockMultipartFile("file", is);
        mockMvc.perform(MockMvcRequestBuilders.multipart("/support/lms/partner/2/routes")
            .file(upload))
            .andExpect(status().isOk());

        Partner partner = partnerRepository.findWithRoutesById(2L).orElse(null);
        softly.assertThat(partner)
            .as("Partner should not be null").isNotNull();

        softly.assertThat(partner.getPartnerRoutes()).extracting(PartnerRoute::getLocationTo)
            .as("Initial routes should be replaced").doesNotContain(225);
        return partner;
    }

    private static List<ScheduleDay> getScheduleDays(Partner partner) {
        return partner.getPartnerRoutes().stream()
            .map(PartnerRoute::getSchedule)
            .map(Schedule::getScheduleDays)
            .flatMap(Set::stream)
            .collect(Collectors.toList());
    }

    private void assertAllDaysHaveTimeFromMidnightToMidnight(List<ScheduleDay> scheduleDays) {
        var totalCount = scheduleDays.size();
        var withCorrectTimeCount = (int) scheduleDays.stream()
            .filter(day -> day.getFrom().equals(LocalTime.MIDNIGHT) && day.getTo().equals(LocalTime.of(23, 59, 59)))
            .count();

        softly
            .assertThat(withCorrectTimeCount)
            .as("All days should have time from 00:00:00 to 23:59:59")
            .isEqualTo(totalCount);
    }
}

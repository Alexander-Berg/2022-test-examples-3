package ru.yandex.market.logistics.management.controller.admin;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import au.com.bytecode.opencsv.CSVReader;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("/data/controller/admin/deliveryIntervalSnapshots/prepare_delivery_interval_snapshots.xml")
class AdminPartnerDeliveryIntervalSnapshotsControllerTest extends AbstractContextualTest {

    @Autowired
    private PechkinHttpClient pechkinHttpClient;

    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_SNAPSHOTS)
    @Test
    void testGetSnapshots() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/lms/delivery-interval-snapshot"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/deliveryIntervalSnapshots/get_result.json"));
    }

    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_SNAPSHOTS)
    @Test
    void testGetSnapshot() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/lms/delivery-interval-snapshot/1"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/deliveryIntervalSnapshots/get_single_result.json"));
    }

    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_SNAPSHOTS)
    @Test
    void testDownloadScheduleSnapshotZip() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/admin/lms/delivery-interval-snapshot/download-schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("ids", "1")
            )
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename=partner-delivery-interval-snapshots-schedule.zip"
            ))
            .andExpect(content().contentType("application/zip"))
            .andExpect(result ->
                extractCsvAndCompare(
                    result,
                    "snapshot-schedule-1-new.csv",
                    "data/controller/admin/deliveryIntervalSnapshots/download_schedule_result.csv"
                )
            );
    }

    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_SNAPSHOTS)
    @Test
    void testDownloadCalendarSnapshotZip() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/admin/lms/delivery-interval-snapshot/download-calendar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .param("ids", "2"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename=partner-delivery-interval-snapshots-calendar.zip"
            ))
            .andExpect(content().contentType("application/zip"))
            .andExpect(result ->
                extractCsvAndCompare(
                    result,
                    "snapshot-calendar-2-outdated.csv",
                    "data/controller/admin/deliveryIntervalSnapshots/download_calendar_result.csv"
                )
            );
    }

    @DatabaseSetup("/data/controller/admin/deliveryIntervalSnapshots/prepare_snapshots_for_activation.xml")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_SNAPSHOTS_EDIT
    )
    @Test
    void testActivateSeveralPartnerSnapshotsError() throws Exception {
        mockMvc.perform(
                post("/admin/lms/delivery-interval-snapshot/activate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ids\": [2, 3, 4]}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Can't activate several snapshots for one partner")
            );
    }

    @DatabaseSetup("/data/controller/admin/deliveryIntervalSnapshots/prepare_snapshots_for_activation.xml")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_DELIVERY_INTERVAL_SNAPSHOTS_EDIT
    )
    @Test
    void testActivateActiveAndNotActivePartnerSnapshotsNoError() throws Exception {
        mockMvc.perform(
                post("/admin/lms/delivery-interval-snapshot/activate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"ids\": [3, 4]}")
            )
            .andExpect(status().isOk()
            );
    }

    private void extractCsvAndCompare(
        MvcResult result,
        String fileName,
        String pathToCheckFile
    ) throws Exception {
        try (ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(
            result.getResponse().getContentAsByteArray()))) {

            ZipEntry file = stream.getNextEntry();
            assertEquals(
                "Response content",
                fileName,
                file.getName()
            );

            byte[] res = new byte[1024];
            stream.read(res);
            stream.closeEntry();

            try (InputStream is = new ByteArrayInputStream(res);
                 InputStreamReader inputStreamReader = new InputStreamReader(is);
                 CSVReader reader = new CSVReader(inputStreamReader);
                 InputStream checkIs = new ClassPathResource(pathToCheckFile).getInputStream();
                 ru.yandex.common.util.csv.CSVReader checkReader = new ru.yandex.common.util.csv.CSVReader(checkIs)) {

                String[] expected;
                String[] parsed;
                while (checkReader.readRecord()) {
                    expected = checkReader.getFields().toArray(String[]::new);
                    parsed = reader.readNext();
                    assertEquals(
                        String.format(
                            "Csv files comparison string: \nparsed: %s\nexpected:\n%s",
                            Arrays.toString(parsed),
                            Arrays.toString(expected)
                        ),
                        parsed,
                        expected
                    );
                }
            }
        }
    }
}

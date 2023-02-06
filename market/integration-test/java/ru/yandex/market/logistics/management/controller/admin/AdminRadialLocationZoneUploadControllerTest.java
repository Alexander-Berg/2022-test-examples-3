package ru.yandex.market.logistics.management.controller.admin;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestRegions.buildRegionTree;

@DisplayName("Загрузка радиальных зон через админку")
public class AdminRadialLocationZoneUploadControllerTest extends AbstractContextualTest {
    private static final String METHOD_URL = "/admin/lms/radial-location-zone";
    @Autowired
    private RegionService regionService;
    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        when(regionService.get()).thenReturn(buildRegionTree());
        featureProperties.setDefaultRadiusValues(Set.of(5000L, 10000L, 15000L, 20000L, 25000L));
    }

    @Test
    @DatabaseSetup("/data/controller/radialZone/before/before_linking_new_zones.xml")
    @ExpectedDatabase(
        value = "/data/controller/radialZone/after/after_link_from_file.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE_EDIT
    )
    @DisplayName("Успешная загрузка радиальных зон")
    void uploadLocationZoneSuccess() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart(METHOD_URL + "/upload/add")
                .file(
                    new MockMultipartFile(
                        "request",
                        "zones.xlsx",
                        "application/x-xls",
                        getSystemResourceAsStream("data/controller/radialZone/zones.xlsx")
                    )
                )
            )
            .andExpect(status().isOk())
            .andExpect(TestUtil.noContent());
    }
}

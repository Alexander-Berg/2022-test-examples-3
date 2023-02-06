package ru.yandex.market.sc.internal.controller.partner;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterFeatures;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PartnerFeaturesControllerTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    @SpyBean
    SortingCenterPropertySource sortingCenterPropertySource;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    @SneakyThrows
    void sortingCenterSupportSeparateRequestForRoutesMainInfoInPI() {
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/features")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{" +
                        "\"sortingCenterId\":" + sortingCenter.getId() + "," +
                        "\"features\": {\""
                        + SortingCenterFeatures.USE_SEPARATE_REQUEST_FOR_ROUTES_MAIN_INFO_IN_PI.name()
                        + "\":true}" +
                        "}", false)
                );
    }

    @Test
    @SneakyThrows
    void sortingCenterDoesNotSupportAnything() {
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/features")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{" +
                        "\"sortingCenterId\":" + sortingCenter.getId() + "," +
                        "\"features\": {}" +
                        "}", false)
                );
    }

    @Test
    @SneakyThrows
    void sortingCenterSupportsXDoc() {
        when(sortingCenterPropertySource.canProcessXdoc(sortingCenter.getId())).thenReturn(true);
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/features")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{" +
                        "\"sortingCenterId\":" + sortingCenter.getId() + "," +
                        "\"features\": {\"" + SortingCenterFeatures.XDOC_ENABLED + "\":true}" +
                        "}", false)
                );
    }

    @Test
    @SneakyThrows
    void sortingCenterCanPrintDocuments() {
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.CANT_PRINT_DOCUMENTS_FROM_WEB, "true");
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/features")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{" +
                        "\"sortingCenterId\":" + sortingCenter.getId() + "," +
                        "\"features\": {\"" + SortingCenterFeatures.CANT_PRINT_DOCUMENTS + "\":true}" +
                        "}", false)
                );
    }

    @Test
    @SneakyThrows
    void sortingCenterCantPrintDocuments() {
        mockMvc.perform(MockMvcRequestBuilders.get(
                        "/internal/partners/" + sortingCenter.getPartnerId() + "/features")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.features",
                        not(hasKey(SortingCenterFeatures.CANT_PRINT_DOCUMENTS.name()))));
    }

}

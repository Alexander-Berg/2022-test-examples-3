package ru.yandex.market.wms.api.controller.monitoring;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.wms.api.repository.TableColumnNamesRepository;
import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {
        InterSchemaDataComparisonMonitoringControllerTestSku.TestConfig.class,
})
@ExtendWith(MockitoExtension.class)
class InterSchemaDataComparisonMonitoringControllerTestSku extends IntegrationTest {

    @Configuration
    public static class TestConfig {
        @Bean
        public TableColumnNamesRepository tableColumnNamesRepository() {
            TableColumnNamesRepository mock = Mockito.mock(TableColumnNamesRepository.class);
            when(mock.findColumnNames(any(), any())).thenReturn(Arrays.asList("SKU", "SUSR5"));
            return mock;
        }
    }

    @Test
    @DatabaseSetups(
            {
                    @DatabaseSetup(value = "/monitoring/interSchemaComparisonSku/data-1.xml",
                            connection = "wmwhseConnection"),
                    @DatabaseSetup(value = "/monitoring/interSchemaComparisonSku/data-1.xml",
                            connection = "enterpriseConnection")
            }
    )
    public void interSchemaComparisonSkuSame() throws Exception {
        mockMvc.perform(get("/monitoring/interSchemaComparisonSku"))
                .andExpect(status().isOk())
                .andExpect(content().string("0;OK"));
    }

    @Test
    @DatabaseSetups(
            {
                    @DatabaseSetup(value = "/monitoring/interSchemaComparisonSku/data-1.xml",
                            connection = "wmwhseConnection"),
                    @DatabaseSetup(value = "/monitoring/interSchemaComparisonSku/data-2.xml",
                            connection = "enterpriseConnection")
            }
    )
    public void interSchemaComparisonSkuDifferent() throws Exception {
        mockMvc.perform(get("/monitoring/interSchemaComparisonSku"))
                .andExpect(status().isOk())
                .andExpect(content().string("2;Mismatched rows: 3; by column: {SUSR5=1}"));
    }
}

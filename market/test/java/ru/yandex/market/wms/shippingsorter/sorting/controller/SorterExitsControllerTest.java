package ru.yandex.market.wms.shippingsorter.sorting.controller;


import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class SorterExitsControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/sorting/controller/sorter-exits-management/successful-get-all/before.xml")
    @ExpectedDatabase(value = "/sorting/controller/sorter-exits-management/successful-get-all/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void shouldSuccessGetSorterExitsAndSettings() throws Exception {
        mockMvc.perform(get("/sorting/sorter-exits")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("sorting/controller/sorter-exits-management/successful-get" +
                        "-all/response.json")));
    }
}

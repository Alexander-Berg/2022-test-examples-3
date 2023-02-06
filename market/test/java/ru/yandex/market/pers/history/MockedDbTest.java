package ru.yandex.market.pers.history;

import java.util.Map;

import javax.sql.DataSource;

import com.google.common.cache.Cache;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.pers.test.common.AbstractPersWebTest;
import ru.yandex.market.pers.test.common.PersTestMocksHolder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
    PersHistoryTestConfiguration.class,
    TestDbConfig.class,
})
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({"junit"})
@TestPropertySource("classpath:/test-application.properties")
public abstract class MockedDbTest extends AbstractPersWebTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    @Qualifier("memCacheMock")
    protected Cache<String, Object> cache;

    @Autowired
    private DataSource persViewsDataSource;

    @Before
    public void cleanDatabase() {
        applySqlScript(persViewsDataSource, "truncate_tables_views.sql");
        resetMocks();
    }

    public void resetMocks() {
        cache.invalidateAll();
        PersTestMocksHolder.resetMocks();
    }

    protected String invokeAndCheckResponse(String path, Map<String, String> parameters) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(path);
        if (parameters != null && !parameters.isEmpty()) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                requestBuilder = requestBuilder.param(entry.getKey(), entry.getValue());
            }
        }

        return mockMvc.perform(requestBuilder
            .accept(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andReturn().getResponse().getContentAsString();
    }

}

package ru.yandex.market.crm.platform.api.test;

import javax.inject.Inject;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;
import ru.yandex.market.mcrm.http.HttpEnvironment;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author apershukov
 */
@WebAppConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = ControllerTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractControllerTest {

    @Inject
    protected HttpEnvironment httpEnvironment;

    @Inject
    private MockMvc mockMvc;

    @Inject
    private YtSchemaTestUtils ytSchemaTestUtils;

    @After
    public void commonTearDown() {
        httpEnvironment.tearDown();
        ytSchemaTestUtils.removeCreated();
    }

    protected ResultActions request(MockHttpServletRequestBuilder builder) throws Exception {
        ResultActions actions = mockMvc.perform(builder);
        MvcResult result = actions.andReturn();
        if (!result.getRequest().isAsyncStarted()) {
            return actions;
        }

        return mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(result));
    }

    protected <T> T request(MockHttpServletRequestBuilder builder, ProtobufParser<T> parser) throws Exception {
        byte[] response = request(builder)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        return parser.parse(response);
    }

    @FunctionalInterface
    public interface ProtobufParser<T> {
        T parse(byte[] content) throws InvalidProtocolBufferException;
    }
}

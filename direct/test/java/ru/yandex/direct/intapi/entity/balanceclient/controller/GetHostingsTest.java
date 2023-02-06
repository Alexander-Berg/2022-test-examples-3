package ru.yandex.direct.intapi.entity.balanceclient.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants;
import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientGetHostingsResponse;
import ru.yandex.direct.intapi.entity.balanceclient.model.GetHostingsResult;
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler;

import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetHostingsTest {

    @Autowired
    private BalanceClientController controller;
    @Autowired
    private HostingsHandler hostingsHandler;

    private MockMvc mockMvc;

    @Before
    public void before() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void checkGetHostingsController() throws Exception {
        mockMvc
                .perform(get(BalanceClientServiceConstants.GET_HOSTINGS_PREFIX))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    public void checkGetHostings() throws Exception {
        BalanceClientGetHostingsResponse response = controller.getHostings();
        assertThat(response.getBody(), beanDiffer(new GetHostingsResult(hostingsHandler.getHostings(),
                hostingsHandler.getPublicSecondLevelDomains())));
    }

}

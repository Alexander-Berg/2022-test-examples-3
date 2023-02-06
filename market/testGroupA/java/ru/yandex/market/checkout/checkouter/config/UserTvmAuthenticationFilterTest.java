package ru.yandex.market.checkout.checkouter.config;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.common.web.CheckoutHttpParameters;
import ru.yandex.market.sdk.userinfo.service.UidConstants;
import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.TicketStatus;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USER_TVM_REQUIRED;

public class UserTvmAuthenticationFilterTest extends AbstractWebTestBase {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserTvmAuthenticationFilter userTvmAuthenticationFilter;

    @Autowired
    private Tvm2 tvm2;

    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;

    @BeforeEach
    public void init() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(userTvmAuthenticationFilter)
                .build();
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"OK"})
    public void notOkStatusTvmTicketForbidden() throws Exception {
        checkouterFeatureWriter.writeValue(USER_TVM_REQUIRED, true);

        String userTicket = String.valueOf(1234567890);
        long userId = 1;
        Assertions.assertFalse(UidConstants.MUID_RANGE.contains(userId));
        mockTvm2(userId, TicketStatus.EXPIRED, userTicket);

        performOrdersQueryWithUserTicket(userTicket, userId, status().isForbidden());
    }

    @Test
    public void tvmTicketOk() throws Exception {
        checkouterFeatureWriter.writeValue(USER_TVM_REQUIRED, true);

        String userTicket = String.valueOf(1234567890);
        long userId = 1;
        Assertions.assertFalse(UidConstants.MUID_RANGE.contains(userId));
        mockTvm2(userId, TicketStatus.OK, userTicket);

        performOrdersQueryWithUserTicket(userTicket, userId, status().isOk());
    }

    @Test
    public void tvmTicketWrongUidForbidden() throws Exception {
        checkouterFeatureWriter.writeValue(USER_TVM_REQUIRED, true);

        String userTicket = String.valueOf(1234567890);
        long userId = 1;
        long nonUserId = 2;
        Assertions.assertFalse(UidConstants.MUID_RANGE.contains(userId));
        Assertions.assertFalse(UidConstants.MUID_RANGE.contains(nonUserId));
        mockTvm2(userId, TicketStatus.OK, userTicket);

        performOrdersQueryWithUserTicket(userTicket, nonUserId, status().isForbidden());
    }

    @Test
    public void tvmTicketWithMuid() throws Exception {
        checkouterFeatureWriter.writeValue(USER_TVM_REQUIRED, true);

        long userId = UidConstants.MUID_RANGE.lowerEndpoint();
        Assertions.assertTrue(UidConstants.MUID_RANGE.contains(userId));

        performOrdersQueryWithoutUserTicket(userId, status().isOk());
    }

    @Test
    public void endpointWithoutUidPAramDoesNotRequireTicketTest() throws Exception {
        checkouterFeatureWriter.writeValue(USER_TVM_REQUIRED, true);

        mockMvc.perform(get("/check-multicart-min-cost", RandomUtils.nextLong())
                        .param(CheckouterClientParams.REGION_ID, "77")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void wrongUidTest() throws Exception {
        checkouterFeatureWriter.writeValue(USER_TVM_REQUIRED, true);

        mockMvc.perform(get("/orders", RandomUtils.nextLong())
                        .param(CheckouterClientParams.UID, "This is non valid uid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
        // при передаче невалидного uid надо отдавать пользовательскую ошибку, а не ошибку сервера
    }

    private void performOrdersQueryWithUserTicket(
            String userTicket,
            long uidParam,
            ResultMatcher expectedStatus) throws Exception {
        mockMvc.perform(get("/orders", RandomUtils.nextLong())
                        .param(CheckouterClientParams.UID, String.valueOf(uidParam))
                        .header(CheckoutHttpParameters.USER_TICKET_HEADER, userTicket)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(expectedStatus);
    }

    private void performOrdersQueryWithoutUserTicket(long uidParam, ResultMatcher expectedStatus) throws Exception {
        mockMvc.perform(get("/orders", RandomUtils.nextLong())
                        .param(CheckouterClientParams.UID, String.valueOf(uidParam))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(expectedStatus);
    }


    private void mockTvm2(Long userUid, TicketStatus status, String ticket) {
        CheckedUserTicket checkedUserTicket = new CheckedUserTicket(status, "mock ticket", new String[]{}, userUid,
                new long[]{userUid});
        when(tvm2.checkUserTicket(ticket)).thenReturn(checkedUserTicket);
    }
}

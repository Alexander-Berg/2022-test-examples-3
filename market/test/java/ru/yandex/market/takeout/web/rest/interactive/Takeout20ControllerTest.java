package ru.yandex.market.takeout.web.rest.interactive;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.takeout.common.CommonConstants;
import ru.yandex.market.takeout.common.TimeoutProvider;
import ru.yandex.market.takeout.config.Takeout2TvmIds;
import ru.yandex.market.takeout.service.AsyncTvmClient;
import ru.yandex.market.takeout.service.DeleteRequest;
import ru.yandex.market.takeout.service.Takeout20Service;
import ru.yandex.market.takeout.util.JsonUtils;
import ru.yandex.market.takeout.web.rest.interactive.models.DeleteResponse;
import ru.yandex.market.takeout.web.rest.interactive.models.ManualDeleteRequest;
import ru.yandex.market.takeout.web.rest.interactive.models.Status;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(secure = false)
@ContextConfiguration(classes = {Takeout20Controller.class, Takeout20ControllerTest.TestConf.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class Takeout20ControllerTest {

    private static final long ADMIN_TVM_ID = 2016441L;

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private Takeout20Service takeout20Service;
    @MockBean
    private AsyncTvmClient asyncTvmClient;
    @MockBean
    private TimeoutProvider timeoutProvider;

    @Test
    public void shouldDeleteManual() throws Exception {
        String serviceTicket = "ticket";
        long uid = 1L;
        when(asyncTvmClient.checkServiceTicketForTvmIds(eq(serviceTicket), eq(singleton(ADMIN_TVM_ID)), any(RequestContext.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(takeout20Service.delete(any(DeleteRequest.class))).thenReturn(CompletableFuture.completedFuture(null));
        MvcResult mvcResult = mockMvc.perform(post("/1/takeout/delete/manual")
                        .header(CommonConstants.X_YA_SERVICE_TICKET, serviceTicket)
                        .content(JsonUtils.marshall(new ManualDeleteRequest(uid, 2L, "login", "some reason")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        verify(takeout20Service).delete(
                argThat(request -> request.getUid() == uid && request.getTypes().contains("all")));
        DeleteResponse deleteResponse = (DeleteResponse) mvcResult.getAsyncResult();
        assertEquals(deleteResponse.getStatus(), Status.Ok);
    }

    @Test
    public void shouldNotDeleteManualWrongTvmId() throws Exception {
        String serviceTicket = "ticket";
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.completeExceptionally(new Exception());
        when(asyncTvmClient.checkServiceTicketForTvmIds(eq(serviceTicket), eq(singleton(ADMIN_TVM_ID)), any(RequestContext.class)))
                .thenReturn(future);
        mockMvc.perform(post("/1/takeout/delete/manual")
                        .header(CommonConstants.X_YA_SERVICE_TICKET, serviceTicket)
                        .content(JsonUtils.marshall(new ManualDeleteRequest(1L, 2L, "login", "some reason")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        verify(takeout20Service, never()).delete(any(DeleteRequest.class));
    }

    @Test
    public void shouldNotDeleteError() throws Exception {
        String serviceTicket = "ticket";
        long uid = 1L;
        CompletableFuture future = new CompletableFuture<>();
        future.completeExceptionally(new Exception());
        when(asyncTvmClient.checkServiceTicketForTvmIds(eq(serviceTicket), eq(singleton(ADMIN_TVM_ID)), any(RequestContext.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(takeout20Service.delete(any(DeleteRequest.class))).thenReturn(future);
        MvcResult mvcResult = mockMvc.perform(post("/1/takeout/delete/manual")
                        .header(CommonConstants.X_YA_SERVICE_TICKET, serviceTicket)
                        .content(JsonUtils.marshall(new ManualDeleteRequest(uid, 2L, "login", "some reason")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        verify(takeout20Service).delete(
                argThat(request -> request.getUid() == uid && request.getTypes().contains("all")));
        DeleteResponse deleteResponse = (DeleteResponse) mvcResult.getAsyncResult();
        assertEquals(deleteResponse.getStatus(), Status.Error);
    }

    @Configuration
    public static class TestConf {

        @Bean
        public Takeout2TvmIds takeout2TvmIds() {
            return new Takeout2TvmIds(emptySet(), ADMIN_TVM_ID);
        }
    }
}

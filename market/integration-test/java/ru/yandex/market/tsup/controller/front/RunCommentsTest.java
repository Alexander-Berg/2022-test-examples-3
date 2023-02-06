package ru.yandex.market.tsup.controller.front;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tpl.common.data_provider.meta.FrontHttpRequestMeta;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.service.data_provider.entity.run.enums.RunCommentScope;
import ru.yandex.mj.generated.client.carrier.api.RunApiClient;
import ru.yandex.mj.generated.client.carrier.model.RunCommentDto;
import ru.yandex.mj.generated.client.carrier.model.RunCommentScopeDto;
import ru.yandex.mj.generated.client.carrier.model.RunCommentSeverityDto;
import ru.yandex.mj.generated.client.carrier.model.RunCommentTypeDto;
import ru.yandex.mj.generated.client.carrier.model.RunCommentsDto;
import ru.yandex.mj.generated.client.carrier.model.TimestampDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class RunCommentsTest extends AbstractContextualTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RunApiClient runClient;

    @Test
    void getRunMessages() throws Exception {
        ExecuteCall<RunCommentsDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        OffsetDateTime offsetDateTime = OffsetDateTime.of(LocalDateTime.of(2021, 1, 2, 11, 0, 0), ZoneOffset.of("+3"));
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(
                new RunCommentsDto().comments(List.of(new RunCommentDto()
                        .createdAt(offsetDateTime)
                        .defaultTimestamp(new TimestampDto().timestamp(offsetDateTime).timezoneName("Europe/Moscow"))
                        .localTimestamp(new TimestampDto().timestamp(offsetDateTime).timezoneName("Europe/Moscow"))
                        .scope(RunCommentScopeDto.DELAY_REPORT)
                        .type(RunCommentTypeDto.DISRUPTION_TRANSPORT_COMPANY)
                        .severity(RunCommentSeverityDto.CRITICAL)
                        .text("Водила пьян!!!")
                        .author("coordinator")
                        .attachmentPath("price_comment_from_ds/run_12_asdfatasd")
                ))));


        Mockito.when(runClient.internalRunsRunIdCommentsGet(
                        Mockito.anyLong(), Mockito.anyList()
                ))
                .thenReturn(call);

        mockMvc.perform(get("/runs/12/comments")
                        .param("scopes",
                                RunCommentScope.DELAY_REPORT.toString(),
                                RunCommentScope.FROM_DRIVER.toString()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "coordinator"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/run/gerRunComments.json"));
    }

    @Test
    void putRunComment() throws Exception {
        ExecuteCall<RunCommentDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        OffsetDateTime offsetDateTime = OffsetDateTime.of(LocalDateTime.of(2021, 1, 2, 11, 0, 0), ZoneOffset.of("+3"));
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(
                new RunCommentDto()
                        .createdAt(offsetDateTime)
                        .defaultTimestamp(new TimestampDto().timestamp(offsetDateTime).timezoneName("Europe/Moscow"))
                        .localTimestamp(new TimestampDto().timestamp(offsetDateTime).timezoneName("Europe/Moscow"))
                        .scope(RunCommentScopeDto.DELAY_REPORT)
                        .type(RunCommentTypeDto.DISRUPTION_TRANSPORT_COMPANY)
                        .severity(RunCommentSeverityDto.CRITICAL)
                        .text("Водила пьян!!!")
                        .author("coordinator")
        ));


        Mockito.when(runClient.internalRunsIdCommentPut(
                        Mockito.any(), Mockito.any()
                ))
                .thenReturn(call);


        mockMvc.perform(post("/runs/12/delayComment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "coordinator")
                        .content(extractFileContent("fixture/run/createRunCommentRequest.json")))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/run/createRunCommentResponse.json"));
    }

}

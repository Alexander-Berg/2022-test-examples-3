package ru.yandex.market.pers.author.takeout;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.takeout.model.TakeoutParam;
import ru.yandex.market.pers.author.takeout.model.TakeoutRequest;
import ru.yandex.market.pers.author.takeout.model.TakeoutState;
import ru.yandex.market.pers.author.takeout.model.TakeoutType;
import ru.yandex.market.pers.author.yt.YtHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 17.11.2021
 */
public class TakeoutS3JobTest extends PersAuthorTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private TakeoutService takeoutService;

    @Autowired
    private TakeoutS3Job takeoutS3Job;

    @Autowired
    private YtHelper ytHelper;

    @Autowired
    private MdsS3Client mdsS3TakeoutClient;

    @Test
    public void testS3Job() throws Exception {
        takeoutService.saveTakeoutRequest(TakeoutRequest.generate("ref", TakeoutType.UID).withData(Map.of(
            TakeoutParam.USER_ID.getCode(), "123"
        )));

        takeoutService.changeStateSafe("ref", TakeoutState.YT_READY);

        List<JsonNode> nodes = List.of(
            MAPPER.readTree("{\"key\":123, \"value\":\"134dd\"}"),
            MAPPER.readTree("{\"key\":52, \"value\":\"5sfdf\"}")
        );

        String expectedJson = "[{\"key\":123, \"value\":\"134dd\"},{\"key\":52, \"value\":\"5sfdf\"}]";

        // mock yt and s3 clients
        doAnswer(invocation -> {
            // do not mock content
            final Consumer<List<JsonNode>> argument = invocation.getArgument(3);
            argument.accept(nodes);
            return null;
        })
            .when(ytHelper.getYtClient()).consumeTableBatched(any(), anyInt(), any(), any());

        when(mdsS3TakeoutClient.getUrl(any())).thenReturn(new URL("http://not.found/path"));
        when(mdsS3TakeoutClient.getPresignedUrl(any(), any())).thenReturn(new URL("http://not.found.other/path2"));

        // run job
        takeoutS3Job.takeoutToS3();

        // check result
        TakeoutRequest request = takeoutService.getStatus("ref");

        assertEquals(TakeoutState.READY, request.getState());
        assertEquals("http://not.found.other/path2", request.getUrl());

        // verify calls
        verify(ytHelper.getYtClient(), times(1)).consumeTableBatched(
            eq(YPath.simple(takeoutS3Job.getTakeoutPath()).child(String.valueOf(request.getId()))),
            anyInt(),
            any(),
            any()
        );

        ArgumentCaptor<ResourceLocation> resourceCaptor =
            ArgumentCaptor.forClass(ResourceLocation.class);
        verify(mdsS3TakeoutClient, times(1)).getUrl(resourceCaptor.capture());

        assertEquals(takeoutS3Job.getMdsBucketName(), resourceCaptor.getValue().getBucketName());
        assertEquals(request.getId() + "-UID-123.json", resourceCaptor.getValue().getKey());

        final ArgumentCaptor<ContentProvider> uploadCaptor = ArgumentCaptor.forClass(ContentProvider.class);
        verify(mdsS3TakeoutClient, times(1)).upload(any(), uploadCaptor.capture());

        assertTrue(uploadCaptor.getValue() instanceof StreamContentProvider);
        JSONAssert.assertEquals(expectedJson, IOUtils.toString(uploadCaptor.getValue().getInputStream()), false);
    }
}

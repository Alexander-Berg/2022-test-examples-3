package ru.yandex.market.tsum.clients.sandbox;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.Test;

import ru.yandex.market.request.netty.JsonNettyHttpClient;
import ru.yandex.market.request.netty.WrongStatusCodeException;
import ru.yandex.market.tsum.clients.sandbox.exceptions.ReleaseAlreadyInProgressException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 27.06.17
 */
public class SandboxClientMockedTest {
    private final Gson gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setDateFormat(SandboxClient.DATE_TIME_FORMAT)
        .create();
    private final ReleaseCreate releaseCreate = new ReleaseCreate(1, SandboxReleaseType.UNSTABLE, "Test");
    private final JsonNettyHttpClient mockedSandboxApiClient = mock(JsonNettyHttpClient.class);

    @Test(expected = ReleaseAlreadyInProgressException.class)
    public void testReleaseAlreadyInProgress() {
        when(mockedSandboxApiClient.executeRequest(
            HttpMethod.POST, "/release", releaseCreate, Object.class
        )).thenThrow(
            new WrongStatusCodeException(
                400,
                "{\"reason\": \"Releasing of task #1 is already in progress\"}",
                "",
                ""
            )
        );
        new SandboxClient(mockedSandboxApiClient, null, null).release(releaseCreate);
    }

    @Test(expected = RuntimeException.class)
    public void testReleaseOtherExceptions() {
        when(mockedSandboxApiClient.executeRequest(
            HttpMethod.POST, "/release", releaseCreate, Object.class
        )).thenThrow(
            new RuntimeException(
                new WrongStatusCodeException(
                    401,
                    "{\"reason\": \"Releasing of task #1 is already in progress\"}",
                    "",
                    ""
                )
            )
        );

        new SandboxClient(mockedSandboxApiClient, null, null).release(releaseCreate);
    }
}

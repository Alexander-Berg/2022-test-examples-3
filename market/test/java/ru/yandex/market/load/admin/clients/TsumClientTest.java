package ru.yandex.market.load.admin.clients;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import retrofit2.Response;

import ru.yandex.market.load.admin.AbstractFunctionalTest;
import ru.yandex.mj.generated.client.tsum.api.TsumApiClient;
import ru.yandex.mj.generated.client.tsum.model.ReleaseLaunchInfo;

@Disabled
@Log4j2
public class TsumClientTest extends AbstractFunctionalTest {
    @Autowired
    private TsumApiClient client;

    @Value("${tsum.oauth.token}")
    private String token;

    @Test
    public void getLaunchInfoOk() throws ExecutionException, InterruptedException {
        Response<ReleaseLaunchInfo> response = client.getLaunchInfoReleaseIdGet(
                "62399bbf2c8c5057074cdae7",
                "OAuth " + token)
                .scheduleResponse()
                .get();
       log.info(response.body());
    }

    @Test
    public void launchProjectOk() throws ExecutionException, InterruptedException {
        client.launchProjectIdPipeIdPost("content-api", "load-production-dev-cancel-orders",
                "OAuth " + token, Collections.emptyList())
            .scheduleResponse()
            .get();
    }
}

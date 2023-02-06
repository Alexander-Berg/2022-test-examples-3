package ru.yandex.market.logistics.utilizer.config;

import java.net.URL;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.logbroker.consumer.LogbrokerReader;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.utilizer.client.datacamp.DataCampClient;
import ru.yandex.market.logistics.utilizer.domain.AppInfo;
import ru.yandex.market.logistics.utilizer.service.StartrekService;
import ru.yandex.market.logistics.utilizer.service.mds.MdsS3Service;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mboc.http.DeliveryParams;
import ru.yandex.startrek.client.Session;

import static org.mockito.ArgumentMatchers.any;

@Configuration
public class MockConfig {

    @Bean
    public MbiApiClient mbiApiClient() {
        return Mockito.mock(MbiApiClient.class);
    }

    @Bean
    public FulfillmentWorkflowClientApi fulfillmentWorkflowClientApi() {
        return Mockito.mock(FulfillmentWorkflowClientApi.class);
    }

    @Bean
    public StartrekService startrekService() {
        return Mockito.mock(StartrekService.class);
    }

    @Bean
    public DeliveryParams deliveryParamsService() {
        return Mockito.mock(DeliveryParams.class);
    }

    @Bean
    public MdsS3Service mdsS3Client() {
        MdsS3Service mdsS3Service = Mockito.mock(MdsS3Service.class);
        Mockito.when(mdsS3Service.uploadFile(any(), any())).thenAnswer(invocation -> {
            String fileName = invocation.getArgument(0);
            return new URL("https://" + fileName);
        });
        return mdsS3Service;
    }

    @Bean
    public LogbrokerReader ssLogbrokerReader() {
        return Mockito.mock(LogbrokerReader.class);
    }

    @Bean
    public LMSClient lmsClient() {
        return Mockito.mock(LMSClient.class);
    }

    @Bean
    public DataCampClient dataCampClient() {
        return Mockito.mock(DataCampClient.class);
    }

    @Bean
    public AppInfo appInfo(){
        return new AppInfo("mockHost");
    }

    @Bean
    public Yt yt() {
        return Mockito.mock(Yt.class);
    }
}

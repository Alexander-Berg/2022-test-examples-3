package ru.yandex.canvas.service;

import java.math.BigInteger;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.canvas.model.video.vh.VhUploadResponse;
import ru.yandex.canvas.model.video.vh.VhVideo;
import ru.yandex.canvas.service.video.VhService;
import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты ходят в реальные сервисы, для тестирования нужно подложить TVM тикет в переменную окружения tvm
 * Который "3:serv:*"
 * добыть токен https://st.yandex-team.ru/DIRECTKNOL-12
 */
public class VhServiceTest {
    private VhService client;

    @Before
    public void before() {
        TvmIntegration tvmIntegration = mock(TvmIntegration.class);
        when(tvmIntegration.isEnabled()).thenReturn(true);
        when(tvmIntegration.getTicket(any())).thenReturn(System.getenv("tvm"));
        client = new VhService("https://vh.test.yandex.ru/v1/",
                tvmIntegration, TvmService.CMS_TEST,
                new ParallelFetcherFactory(new DefaultAsyncHttpClient(), new FetcherSettings()));
        System.out.println(System.getenv("tvm"));
    }

    @Test
    @Ignore("Ходит в реальную систему")
    public void getVideo() {
        VhVideo video = client.getVideo(BigInteger.valueOf(8813608391117004900L));
        assertThat(video.getService(), is("canvas-testing"));
        System.out.println(video);
    }

    @Test
    @Ignore("Ходит в реальную систему")
    public void startEncoding() {
        VhUploadResponse response = client.startEncoding(
                "https://storage.mds.yandex.net/get-bstor/2267090/3e2f55d0-8de2-4200-9fe2-cddd0742b7d5.mp4");
        System.out.println(response);
        assertNotNull(response);
    }
}

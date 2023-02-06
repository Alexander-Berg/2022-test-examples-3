package ru.yandex.market.mboc.common.services.idxapi.pics;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import NMarket.NPicrobot.MdsInfo;
import NMarket.NPicrobot.State;
import io.micrometer.core.instrument.Metrics;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.mboc.common.services.idxapi.pics.dto.ImageSignatureCollection;
import ru.yandex.market.mboc.common.services.idxapi.pics.dto.ImageSignatureLayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PicrobotApiServiceTest {
    private static final String host = "http://market.ir.test";
    private static final String namespace = "pic_test";

    private PicrobotApiService picrobotApiService;


    @Before
    public void setUp() {
//        picrobotApiService= new DefaultPicrobotApiService(
//            restTemplate(),
//            "http://datacamp-picrobot.vs.market.yandex.net",
//            "marketpic"
//            );
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(anyString(), any(), eq(State.TPicrobotStateBatch.class)))
            .then(invocation -> {
                List<String> urls = invocation.getArgument(1);
                State.TPicrobotStateBatch.Builder builder = State.TPicrobotStateBatch.newBuilder();

                for (String url : urls) {
                    State.TPicrobotState picrobotState = State.TPicrobotState.newBuilder()
                        .addMdsInfo(MdsInfo.TMdsInfo.newBuilder()
                            .setFactors("{\"image2toloka_v6\":\"1 2 3\"}")
                            .setMdsId(MdsInfo.TMdsId.newBuilder().setNamespace(namespace))
                        )
                        .build();
                    builder.putStates(url, picrobotState);

                }
                return new ResponseEntity<>(builder.build(), HttpStatus.OK);
            });

        picrobotApiService = new DefaultPicrobotApiService(Metrics.globalRegistry, restTemplate, host, namespace);
    }

    @Test
    public void getStates() {
        List<String> urls = IntStream.range(0, 1000).mapToObj(i -> "image" + i).collect(Collectors.toList());
        ImageSignatureCollection imageSignatureCollection = picrobotApiService.getImageEmbeddings(urls);
        assertThat(imageSignatureCollection.getSize()).isEqualTo(urls.size());

        Map<ImageSignatureLayer, String> signatures = imageSignatureCollection.getSignaturesByPicId(urls.get(0));
        assertThat(signatures).containsOnlyKeys(ImageSignatureLayer.SIGNATURE_V6);
    }
}

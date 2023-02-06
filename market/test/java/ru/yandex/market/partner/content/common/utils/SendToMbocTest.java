package ru.yandex.market.partner.content.common.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.collect.Iterables;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.http.ServiceClient;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsServiceStub;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor;
import ru.yandex.market.request.httpclient.trace.TraceHttpResponseInterceptor;
import ru.yandex.market.request.trace.Module;

/**
 * Массовая переотправка офферов
 */
public class SendToMbocTest {

    /**
     * На вход - csv файл со списком офферов в формате businessId, shopSku.
     */
    @Ignore
    @Test
    public void bulkSending() throws IOException {
        String pathToFile = "/home/eventyr/Downloads/offers.csv";
        send(pathToFile);
    }

    private void send(String pathToFile) throws IOException {
        MboMappingsServiceStub stub = new MboMappingsServiceStub();
        stub.setHost("http://cm-api.vs.market.yandex.net/proto/mboMappingsService/");
        initServiceClient(stub, Module.MBOC_UI);

        List<String> allIds = Files.readAllLines(Paths.get(pathToFile));

        int sentIds = 0;
        for (List<String> ids : Iterables.partition(allIds, 100)) {
            MboMappings.AddToContentProcessingRequest.Builder reqBuilder =
                    MboMappings.AddToContentProcessingRequest.newBuilder();
            reqBuilder.setIsForce(false);
            reqBuilder.setIsDeduplicated(false);
            for (String id : ids) {
                String[] split = id.split(",");
                int businessId = Integer.parseInt(split[0]);
                String shopSku = split[1].replaceAll("\"", "");
                reqBuilder.addBusinessSkuKey(MbocCommon.BusinessSkuKey.newBuilder()
                        .setBusinessId(businessId)
                        .setOfferId(shopSku)
                        .build());
            }
            MboMappings.AddToContentProcessingResponse addToContentProcessingResponse =
                    stub.addOfferToContentProcessing(reqBuilder.build());
            System.out.println(addToContentProcessingResponse);
            sentIds = sentIds + addToContentProcessingResponse.getResultCount();
        }
    }

    private void initServiceClient(ServiceClient serviceClient, Module traceModule) {
        serviceClient.setUserAgent("defaultUserAgent");
        if (traceModule != null) {
            serviceClient.setHttpRequestInterceptor(new TraceHttpRequestInterceptor(traceModule));
            serviceClient.setHttpResponseInterceptor(new TraceHttpResponseInterceptor());
        }
    }
}

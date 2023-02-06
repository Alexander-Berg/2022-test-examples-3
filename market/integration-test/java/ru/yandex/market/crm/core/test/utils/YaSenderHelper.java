package ru.yandex.market.crm.core.test.utils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.yandex.bolts.function.Function;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.core.domain.yasender.Recipient;
import ru.yandex.market.crm.core.domain.yasender.YaSenderCampaign;
import ru.yandex.market.crm.core.domain.yasender.YaSenderSendingState;
import ru.yandex.market.crm.core.domain.yasender.YaSenderTransactionalCampaign;
import ru.yandex.market.crm.core.services.external.yandexsender.CampaignResult;
import ru.yandex.market.crm.core.services.external.yandexsender.UnsubscribeGroup;
import ru.yandex.market.crm.core.services.external.yandexsender.YaSenderResponse;
import ru.yandex.market.crm.core.services.external.yandexsender.YaSenderResponse.Result;
import ru.yandex.market.crm.core.services.external.yandexsender.YaSenderResponse.Status;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderDataRow;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.http.Http.NamedValue;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.ResponseBuilder;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mcrm.http.HttpRequest.get;
import static ru.yandex.market.mcrm.http.HttpRequest.post;

/**
 * @author apershukov
 */
@Component
public class YaSenderHelper implements StatefulHelper {

    private static class SenderResponse {

        @JsonProperty("result")
        private CampaignResult result;

        SenderResponse(CampaignResult result) {
            this.result = result;
        }

        public CampaignResult getResult() {
            return result;
        }

        public void setResult(CampaignResult result) {
            this.result = result;
        }
    }

    public static class RenderRequest {

        @JsonProperty("params")
        private YaSenderDataRow data;

        public YaSenderDataRow getData() {
            return data;
        }

        public void setData(YaSenderDataRow data) {
            this.data = data;
        }
    }

    public static class PromoInfo {

        private String template;
        private YPath dataTable;

        PromoInfo(String template, YPath dataTable) {
            this.template = template;
            this.dataTable = dataTable;
        }

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

        public YPath getDataTable() {
            return dataTable;
        }

        public void setDataTable(YPath dataTable) {
            this.dataTable = dataTable;
        }
    }

    @FunctionalInterface
    public interface CampaignCallback {

        void onReceive(YaSenderCampaign campaign) throws Exception;
    }

    public static class SendTxRequest {
        private final String email;
        private final Map<String, Object> args;

        SendTxRequest(String email, Map<String, Object> args) {
            this.email = email;
            this.args = args;
        }

        public String getEmail() {
            return email;
        }

        public Map<String, Object> getArgs() {
            return args;
        }
    }

    public static Result sendTxResult(SendTxRequest request) {
        return sendTxResult("message_to-" + request.getEmail());
    }

    public static Result sendTxResult(String messageId) {
        Result result = new Result();
        result.setStatus(Status.OK);
        result.setMessageId(messageId);
        return result;
    }

    private final JsonSerializer jsonSerializer;
    private final JsonDeserializer jsonDeserializer;

    private final HttpEnvironment httpEnvironment;
    private final String baseUrl;

    private final String senderAccount;

    private final Map<String, Boolean> receivedTransactional = new ConcurrentHashMap<>();
    private final List<PromoInfo> createdPromos = Collections.synchronizedList(new ArrayList<>());

    public YaSenderHelper(JsonSerializer jsonSerializer,
                          JsonDeserializer jsonDeserializer,
                          HttpEnvironment httpEnvironment,
                          @Value("${external.yasender.url}") String baseUrl,
                          @Value("${external.yasender.account.slug}") String senderAccount) {
        this.jsonSerializer = jsonSerializer;
        this.jsonDeserializer = jsonDeserializer;
        this.httpEnvironment = httpEnvironment;
        this.baseUrl = baseUrl;
        this.senderAccount = senderAccount;
    }

    @Override
    public void setUp() {
    }

    public void onCreateTxCampaign(Function<YaSenderTransactionalCampaign, CampaignResult> callback) {
        httpEnvironment.when(
            post(baseUrl() + "/automation/newtransact")
        ).then(request -> {
            YaSenderTransactionalCampaign body = jsonDeserializer.readObject(
                    YaSenderTransactionalCampaign.class,
                    request.getBody()
            );

            SenderResponse response = new SenderResponse(callback.apply(body));
            return ResponseBuilder.newBuilder()
                    .body(jsonSerializer.writeObjectAsBytes(response))
                    .build();
        });
    }

    public void expectTransactionals(String campaignSlug) {
        onSendTransactional(campaignSlug, request -> {
            receivedTransactional.put(request.getEmail(), Boolean.TRUE);
            return sendTxResult(request);
        });
    }

    public void onSendTransactional(String campaignSlug, Function<SendTxRequest, Result> callback) {
        httpEnvironment.when(
                post(baseUrl() + "/transactional/" + campaignSlug + "/send")
        )
        .then(request -> {
            String email = request.getQueryParameters().stream()
                    .filter(param -> "to_email".equals(param.getName()))
                    .findFirst()
                    .map(NamedValue::getValue)
                    .orElseThrow(() -> new IllegalStateException("No email query parameter"));

            String[] parts = new String(request.getBody(), StandardCharsets.UTF_8).split("&");

            Map<String, Object> args = Stream.of(parts)
                    .map(part -> {
                        String[] pair = part.split("=");
                        return Pair.of(pair[0], pair[1]);
                    })
                    .filter(x -> "args".equals(x.getLeft()))
                    .findFirst()
                    .map(Pair::getRight)
                    .map(x -> URLDecoder.decode(x, StandardCharsets.UTF_8))
                    .map(value -> jsonDeserializer.readObject(new TypeReference<Map<String, Object>>() {}, value))
                    .orElseThrow(() -> new IllegalStateException("Cannot deserialize request args"));

            Result result = callback.apply(new SendTxRequest(email, args));

            YaSenderResponse<Result> response = new YaSenderResponse<>();
            response.setResult(result);

            return ResponseBuilder.newBuilder()
                    .body(jsonSerializer.writeObjectAsBytes(response))
                    .build();
        });
    }

    public void expectPromos(Set<YPath> paths) {
        var strPaths = paths.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());

        onSendOrCreatePromo(campaign -> {
            var requestedPath = (String) campaign.getSegment().getParams().get("path");
            assertTrue(strPaths.contains(requestedPath));
            prepareCampaignState(campaign.getSlug(), YaSenderSendingState.SENT);
            createdPromos.add(new PromoInfo(campaign.getLetterBody(), YPath.simple(requestedPath)));
        });
    }

    public void expectPromo(String campaignSlug, YPath path) {
        onSendOrCreatePromo(campaignSlug, campaign -> {
            String requestedPath = (String) campaign.getSegment().getParams().get("path");
            assertEquals(path.toString(), requestedPath);

            createdPromos.add(new PromoInfo(campaign.getLetterBody(), YPath.simple(requestedPath)));
        });
    }

    public void onSendOrCreatePromo(CampaignCallback callback) {
        onSendOrCreatePromo(UUID.randomUUID().toString(), callback);
    }

    public void onSendOrCreatePromo(String campaignSlug, CampaignCallback callback) {
        httpEnvironment.when(
                post(baseUrl() + "/automation/promoletter")
        ).then(request -> {
            YaSenderCampaign campaign = jsonDeserializer.readObject(YaSenderCampaign.class, request.getBody());

            var campaignId = RandomUtils.nextLong(0, 100_000);
            campaign.setId(campaignId);
            campaign.setSlug(campaignSlug);

            callback.onReceive(campaign);

            CampaignResult result = new CampaignResult();
            result.setId(campaignId);
            result.setSlug(campaignSlug);

            SenderResponse response = new SenderResponse(result);

            return ResponseBuilder.newBuilder()
                    .body(jsonSerializer.writeObjectAsBytes(response))
                    .build();
        });
    }

    public void onTestSendPromo(String campaignSlug, Consumer<List<Recipient>> callback) {
        httpEnvironment.when(
                post(baseUrl() + "/automation/promoletter/" + campaignSlug + "/test")
        ).then(request -> {
            List<Recipient> recipients = jsonDeserializer.readObject(
                    new TypeReference<Map<String, List<Recipient>>>() {}, request.getBody()
            ).get("recipients");

            callback.accept(recipients);

            return ResponseBuilder.newBuilder().build();
        });
    }

    public void expectPreview(String resultHtml) {
        expectPreview(resultHtml, request -> {});
    }

    public void expectPreview(String resultHtml, Consumer<RenderRequest> callback) {
        httpEnvironment.when(
                post("https://sender.yandex-team.ru/api/0/market/render")
        ).then(request -> {
            RenderRequest renderRequest = jsonDeserializer.readObject(RenderRequest.class, request.getBody());
            callback.accept(renderRequest);
            return ResponseBuilder.newBuilder()
                    .body("{\"result\":\"" + resultHtml + "\"}")
                    .build();
        });
    }

    public void onRenderRequest(long campaignId, long letterId, Function<RenderRequest, String> callback) {
        var url = String.format(
                "https://sender.yandex-team.ru/api/0/market/render/campaign/%d/letter/%d",
                campaignId,
                letterId
        );

        httpEnvironment.when(post(url))
                .then(request -> {
                    var renderRequest = jsonDeserializer.readObject(RenderRequest.class, request.getBody());
                    var html = callback.apply(renderRequest);
                    return ResponseBuilder.newBuilder()
                            .body("{\"result\":\"" + html + "\"}")
                            .build();
                });
    }

    public void setGlobalUnsubscribeStatus(String email) {
        UnsubscribeGroup group = new UnsubscribeGroup();
        group.setUnsubscribed(false);

        YaSenderResponse<UnsubscribeGroup> response = new YaSenderResponse<>();
        response.setResultObject(group);

        httpEnvironment.when(
                get(baseUrl() + "/unsubscribe/global")
                        .param("email", email)
        ).then(
                ResponseBuilder.newBuilder()
                        .body(jsonSerializer.writeObjectAsBytes(response))
                        .build()
        );
    }

    public void prepareCampaign(YaSenderCampaign campaign) {
        httpEnvironment.when(get(baseUrl() + "/campaign/" + campaign.getSlug() + "/."))
                .then(
                        ResponseBuilder.newBuilder()
                                .body(jsonSerializer.writeObjectAsBytes(campaign))
                                .build()
                );
    }

    public void prepareCampaignState(String slug, YaSenderSendingState state) {
        var campaign = new YaSenderCampaign();
        campaign.setSlug(slug);
        campaign.setState(state);
        prepareCampaign(campaign);
    }

    @Override
    public void tearDown() {
        receivedTransactional.clear();
        createdPromos.clear();
    }

    public void verifySent(String email) {
        assertTrue(receivedTransactional.containsKey(email));
    }

    public List<PromoInfo> getCreatedPromos() {
        return createdPromos;
    }

    private String baseUrl() {
        return baseUrl + "/api/0/" + senderAccount;
    }
}

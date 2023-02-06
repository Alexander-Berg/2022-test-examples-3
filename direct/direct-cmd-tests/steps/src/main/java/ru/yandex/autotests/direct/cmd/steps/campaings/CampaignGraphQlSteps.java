package ru.yandex.autotests.direct.cmd.steps.campaings;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndLogger;
import ru.yandex.autotests.direct.cmd.steps.base.DirectStepsContext;
import ru.yandex.autotests.direct.utils.BaseSteps;
import ru.yandex.autotests.httpclientlite.HttpClientLite;
import ru.yandex.autotests.httpclientlite.context.ConnectionContext;
import ru.yandex.autotests.httpclientlite.context.ContextRequestExecutor;
import ru.yandex.autotests.httpclientlite.core.RequestBuilder;
import ru.yandex.autotests.httpclientlite.core.Response;
import ru.yandex.autotests.httpclientlite.core.ResponseParser;
import ru.yandex.autotests.httpclientlite.core.request.AbstractRequestBuilder;
import ru.yandex.autotests.httpclientlite.core.response.AbstractResponseParser;
import ru.yandex.qatools.allure.annotations.Step;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static ru.yandex.autotests.direct.cmd.data.Headers.ACCEPT_JSON_HEADER;
import static ru.yandex.autotests.direct.cmd.data.Headers.X_REQUESTED_WITH_HEADER;

@ParametersAreNonnullByDefault
public class CampaignGraphQlSteps extends BaseSteps<DirectStepsContext> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String ADD_CAMPAIGNS_QUERY =
            "mutation AddCampaigns($input: GdAddCampaignsInput!, $login: String!) {\n" +
            "  reqId: getReqId\n" +
            "  addCampaigns(input: $input) {\n" +
            "    addedCampaigns {\n" +
            "      id\n" +
            "    }\n" +
            "    validationResult {\n" +
            "      errors {\n" +
            "        code\n" +
            "        params\n" +
            "        path\n" +
            "      }\n" +
            "      warnings {\n" +
            "        code\n" +
            "        params\n" +
            "        path\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "  getClientMutationId(input: {login: $login}) {\n" +
            "    mutationId\n" +
            "  }\n" +
            "}\n";

    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignGraphQlSteps.class);

    private ContextRequestExecutor requestExecutor;
    private ConnectionContext connectionContext;

    protected void init(DirectStepsContext context) {
        super.init(context);

        HttpClientLite clientLite = new HttpClientLite.Builder()
                .withClient(context.getHttpClient())
                .withRequestBuilder(buildRequestBuilder())
                .withResponseParser(buildResponseParser())
                .withLogger(new DirectBackEndLogger(context))
                .build();

        connectionContext = context.getConnectionContext();
        requestExecutor = new ContextRequestExecutor(clientLite, connectionContext);
    }

    @Step("Создаём кампанию через graphql")
    public Long addCampaign(String ulogin, Map<String, Object> request) {
        Map<String, Object> postPayload = ImmutableMap.<String, Object>builder()
                .put("operationName", "AddCampaigns")
                .put("variables", ImmutableMap.<String, Object>builder()
                        .put("input", ImmutableMap.<String, Object>builder()
                                .put("campaignAddItems", Collections.singletonList(request))
                                .build())
                        .put("login", ulogin)
                        .build())
                .put("query", ADD_CAMPAIGNS_QUERY)
                .build();

        LOGGER.warn("request: {}", postPayload);

        AddCampaignsGraphQlResponse result = runRequest(ulogin, postPayload, AddCampaignsGraphQlResponse.class);

        checkNotNull(result.data);
        checkNotNull(result.data.addCampaigns);
        checkNotNull(result.data.addCampaigns.addedCampaigns);
        checkState(result.data.addCampaigns.addedCampaigns.size() == 1);

        GdAddCampaignPayloadItem addedCampaignItem = result.data.addCampaigns.addedCampaigns.get(0);
        checkNotNull(addedCampaignItem, "added campaigns item must not be null from response");
        checkNotNull(addedCampaignItem.id, "added campaigns item ID must not be null from response");

        return addedCampaignItem.id;
    }

    private <T> T runRequest(String ulogin, Map<String, Object> postPayload,
                             @SuppressWarnings("SameParameterValue") Class<T> responseType) {
        URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme(connectionContext.getScheme())
                    .setHost(connectionContext.getHost())
                    .setPort(connectionContext.getPort())
                    .setPath("/web-api/grid/api")
                    .addParameter("ulogin", ulogin)
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        Object executionResult = requestExecutor.getHttpClientLite().post(uri, postPayload, Map.class);
        LOGGER.warn("result: {}", executionResult);

        return OBJECT_MAPPER.convertValue(executionResult, responseType);
    }

    protected RequestBuilder buildRequestBuilder() {
        return new AbstractRequestBuilder() {
            @Override
            protected void setEntity(HttpEntityEnclosingRequestBase request, Object o) {
                String marshalledRequest;
                try {
                    marshalledRequest = OBJECT_MAPPER.writeValueAsString(o);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                request.setEntity(new StringEntity(marshalledRequest, ContentType.APPLICATION_JSON));
            }

            @Override
            protected void setHeaders(HttpUriRequest request) {
                super.setHeaders(request);

                request.setHeaders(new Header[] {
                        ACCEPT_JSON_HEADER,
                        X_REQUESTED_WITH_HEADER,
                        new BasicHeader("X-CSRF-TOKEN", getContext().getAuthConfig().getCsrfToken().getValue())
                });
            }
        };
    }

    protected ResponseParser buildResponseParser() {
        return new AbstractResponseParser() {
            @Override
            protected <T> T actualParse(Response response, Class<T> resultClass) {
                try {
                    return OBJECT_MAPPER.readValue(response.getResponseContent().asString(), resultClass);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private static class AddCampaignsGraphQlResponse {
        private AddCampaignsGraphQlData data;
    }

    private static class AddCampaignsGraphQlData {
        private Long reqId;
        private AddCampaignsGdApiResponse addCampaigns;
    }

    private static class AddCampaignsGdApiResponse {
        private GdValidationResult validationResult;
        private List<GdAddCampaignPayloadItem> addedCampaigns;
    }

    private static class GdValidationResult {
        private List<GdDefect> errors;
        private List<GdDefect> warnings;
    }

    private static class GdDefect {
        private String code;
        private String path;
        private Object params;
    }

    private static class GdAddCampaignPayloadItem {
        private Long id;
    }
}

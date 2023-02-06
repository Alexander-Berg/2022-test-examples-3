package ru.yandex.market.crm.core.test.utils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.crm.core.services.external.appmetrica.domain.GroupListResponse;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.GroupReqResp;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushBatchResponse;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushMessages;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushSendGroup;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.SendPushBatchResponse;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.util.LiluStreams;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.ResponseBuilder;
import ru.yandex.market.mcrm.utils.test.StatefulHelper;

import static ru.yandex.market.mcrm.http.HttpRequest.get;
import static ru.yandex.market.mcrm.http.HttpRequest.post;

/**
 * @author apershukov
 */
@Configuration
public class AppMetricaHelper implements StatefulHelper {

    private final HttpEnvironment httpEnvironment;
    private final JsonSerializer jsonSerializer;
    private final JsonDeserializer jsonDeserializer;
    private final String baseUrl;

    private final Set<String> expected = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final BlockingQueue<SendPushesRequest> receivedSendRequests = new LinkedBlockingQueue<>();
    private final BlockingQueue<PushSendGroup> createdGroups = new LinkedBlockingQueue<>();

    public AppMetricaHelper(HttpEnvironment httpEnvironment,
                            JsonSerializer jsonSerializer,
                            JsonDeserializer jsonDeserializer,
                            @Value("${external.appMetricaPush.url}") String baseUrl) {
        this.httpEnvironment = httpEnvironment;
        this.jsonSerializer = jsonSerializer;
        this.jsonDeserializer = jsonDeserializer;
        this.baseUrl = baseUrl;
    }

    public void expectDevice(String deviceId) {
        expected.add(deviceId);
    }

    public void expectDevices(String... deviceIds) {
        for (String id : deviceIds) {
            expectDevice(id);
        }
    }

    public void verify() {
        if (!expected.isEmpty()) {
            Assert.fail("Not all expected devices got notification");
        }
    }

    @Override
    public void setUp() {
        GroupListResponse emptyGroupsResponse = new GroupListResponse();
        emptyGroupsResponse.setGroups(Collections.emptyList());

        httpEnvironment.when(get(baseUrl + "/push/v1/management/groups"))
                .then(
                        ResponseBuilder.newBuilder()
                                .body(jsonSerializer.writeObjectAsBytes(emptyGroupsResponse))
                                .build()
                );

        httpEnvironment.when(post(baseUrl + "/push/v1/management/groups"))
                .then(request -> {
                    GroupReqResp resp = jsonDeserializer.readObject(GroupReqResp.class, request.getBody());
                    var group = resp.getGroup();
                    group.setId(123);

                    createdGroups.put(group);

                    return ResponseBuilder.newBuilder()
                            .body(
                                    jsonSerializer.writeObjectAsBytes(resp)
                            )
                            .build();
                });

        httpEnvironment.when(post(baseUrl + "/push/v1/send-batch"))
                .then(request -> {
                    SendPushesRequest body = jsonDeserializer.readObject(SendPushesRequest.class, request.getBody());
                    receivedSendRequests.put(body);

                    var sendBatchRequest = body.getSendBatchRequest();

                    var groupId = sendBatchRequest.getGroupId();
                    var groupExists = createdGroups.stream()
                            .anyMatch(group -> group.getId() == groupId);

                    if (!groupExists) {
                        throw new IllegalStateException("No group with id " + groupId);
                    }

                    LiluStreams.of(sendBatchRequest.getBatches())
                            .flatMap(batch -> LiluStreams.of(batch.getDevices()))
                            .flatMap(x -> LiluStreams.of(x.getIdValues()))
                            .forEach(deviceId -> {
                                if (!expected.remove(deviceId)) {
                                    throw new IllegalArgumentException(
                                            "Got push notification to unexpected device '" + deviceId + "'"
                                    );
                                }
                            });

                    PushBatchResponse pushBatchResponse = new PushBatchResponse();
                    pushBatchResponse.setTransferId(RandomUtils.nextInt(1, 100_000));

                    SendPushBatchResponse response = new SendPushBatchResponse();
                    response.setPushBatchResponse(pushBatchResponse);

                    return ResponseBuilder.newBuilder()
                            .body(
                                    jsonSerializer.writeObjectAsBytes(response)
                            )
                            .build();
                });
    }

    public SendPushesRequest pollForSendRequest(int timeoutSeconds) throws InterruptedException {
        return receivedSendRequests.poll(timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void tearDown() {
        expected.clear();
        receivedSendRequests.clear();
        createdGroups.clear();
    }

    public BlockingQueue<PushSendGroup> getCreatedGroups() {
        return createdGroups;
    }

    public static class DeviceSet {

        @JsonProperty("id_values")
        private List<String> idValues;

        public List<String> getIdValues() {
            return idValues;
        }

        public void setIdValues(List<String> idValues) {
            this.idValues = idValues;
        }
    }

    public static class Batch {

        @JsonProperty("messages")
        private PushMessages pushMessages;

        @JsonProperty("devices")
        private List<DeviceSet> devices;

        public List<DeviceSet> getDevices() {
            return devices;
        }

        public void setDevices(List<DeviceSet> devices) {
            this.devices = devices;
        }

        public PushMessages getPushMessages() {
            return pushMessages;
        }

        public void setPushMessages(PushMessages pushMessages) {
            this.pushMessages = pushMessages;
        }
    }

    public static class SendBatchRequest {

        @JsonProperty("group_id")
        private int groupId;

        @JsonProperty("batch")
        private List<Batch> batches;

        public int getGroupId() {
            return groupId;
        }

        public void setGroupId(int groupId) {
            this.groupId = groupId;
        }

        public List<Batch> getBatches() {
            return batches;
        }

        public void setBatches(List<Batch> batches) {
            this.batches = batches;
        }
    }

    public static class SendPushesRequest {

        @JsonProperty("push_batch_request")
        private SendBatchRequest sendBatchRequest;

        public SendBatchRequest getSendBatchRequest() {
            return sendBatchRequest;
        }

        public void setSendBatchRequest(SendBatchRequest sendBatchRequest) {
            this.sendBatchRequest = sendBatchRequest;
        }
    }
}

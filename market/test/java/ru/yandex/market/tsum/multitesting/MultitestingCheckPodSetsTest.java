package ru.yandex.market.tsum.multitesting;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.inside.yt.kosher.impl.ytree.serialization.YTreeTextSerializer;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.tsum.clients.nanny.NannyClient;
import ru.yandex.market.tsum.clients.nanny.service.NannyService;
import ru.yandex.market.tsum.clients.yp.YandexDeployClientTest;
import ru.yandex.yp.YpInstance;
import ru.yandex.yp.YpRawClient;
import ru.yandex.yp.YpRawClientBuilder;
import ru.yandex.yp.client.api.Autogen;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.model.YpObjectType;
import ru.yandex.yp.model.YpPayload;
import ru.yandex.yp.model.YpSelectStatement;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@RunWith(Parameterized.class)
@Ignore("integration test")
public class MultitestingCheckPodSetsTest {
    private static final int ABC_SERVICE_ID = 2629;
    private static final Duration YP_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration NANNY_TIMEOUT = Duration.ofMinutes(5);
    private static final int NANNY_THREADS = 20;

    private static final String YP_TOKEN;
    private static final Map<Location, YpRawClient> YP_RAW_CLIENTS;
    private static final String NANNY_TOKEN;
    private static final NannyClient NANNY_CLIENT;

    static {
        // https://oauth.yandex-team.ru/authorize?response_type=token&client_id=f8446f826a6f4fd581bf0636849fdcd7
        YP_TOKEN = YandexDeployClientTest.getToken(".yp/token");
        YP_RAW_CLIENTS = Stream.of(Location.values())
            .collect(Collectors.toMap(
                Function.identity(),
                location -> new YpRawClientBuilder(location.getYpInstance(), () -> YP_TOKEN).build()));

        // https://nanny.yandex-team.ru/ui/#/oauth/
        NANNY_TOKEN = YandexDeployClientTest.getToken(".nanny/token");
        NettyHttpClientContext nettyContext = new NettyHttpClientContext(new HttpClientConfig());
        NANNY_CLIENT = new NannyClient("https://nanny.yandex-team.ru", NANNY_TOKEN, nettyContext);
    }

    private final TestCase testCase;

    public MultitestingCheckPodSetsTest(TestCase testCase) {
        this.testCase = testCase;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        // если хочется узнать, какие данные вообще можно вытащить с помощью ya tool yp select-objects,
        // можно убрать все --selector, указать --selector '' --limit 1 --no-tabular

        /*
        ya tool yp select-objects pod_set \
            --filter '[/spec/account_id] = "abc:service:2629"' \
            --address SAS \
            --selector '/meta/id' \
            --selector '/labels/nanny_service_id'
         */
        List<PodSetData> podSets = getPodSets();

        /*
        ya tool yp select-objects pod --address SAS \
            --filter '[/meta/pod_set_id] = "mt-front-market--marketfront-7332-3f85a0d6-sas"' \
            --selector '/meta/id' \
            --selector '/meta/pod_set_id' \
            --selector '/spec/resource_requests' \
            --no-tabular
         */
        Collection<PodSetIdWithLocation> podSetIds = podSets.stream()
            .map(podSet -> new PodSetIdWithLocation(podSet.getId(), podSet.getLocation()))
            .collect(toSet());

        Map<String, List<PodData>> pods = getPods(podSetIds).stream()
            .collect(Collectors.groupingBy(pod -> pod.podSetId, toList()));

        Collection<String> nannyServiceIds = podSets.stream()
            .map(PodSetData::getNannyServiceId)
            .filter(Objects::nonNull)
            .collect(toSet());
        Map<String, Optional<NannyService>> nannyServices = getNannyServices(nannyServiceIds);

        return podSets.stream()
            .map(podSet -> new Object[]{new TestCase(podSet, pods.get(podSet.getId()),
                nannyServices.get(podSet.getNannyServiceId()))})
            .collect(toList());
    }

    @Test
    public void hasPods() {
        assertThat(testCase.pods)
            .isNotNull()
            .isNotEmpty();
    }

    @Test
    public void reasonableCpuGuarantee() {
        List<PodData> pods = testCase.pods;
        Long totalCpus = pods.stream().map(pod -> pod.cpuGuarantee).reduce(Long::sum).orElse(0L);
        assertThat(totalCpus).isLessThanOrEqualTo(2000L);
    }

    @Test
    public void diskBandwidthLimitAndGuarantee() {
        assertSoftly(softly -> {
            for (PodData pod : testCase.pods) {
                for (Map.Entry<String, BandwidthSpec> entry : pod.bandwidthSpecsByMountPoint.entrySet()) {
                    softly.assertThat(entry.getValue().guarantee)
                        .as("disk guarantee for " + pod.id + " mountpoint " + entry.getKey())
                        .isGreaterThan(0L);
                    softly.assertThat(entry.getValue().limit)
                        .as("disk limit for " + pod.id + " mountpoint " + entry.getKey())
                        .isGreaterThan(0L);
                }
            }
        });
    }

    @Test
    public void networkBandwidthLimitAndGuarantee() {
        assertSoftly(softly -> {
            for (PodData pod : testCase.pods) {
                softly.assertThat(pod.networkBandwidthSpec.guarantee)
                    .as("network guarantee for " + pod.id)
                    .isGreaterThan(0L);
                softly.assertThat(pod.networkBandwidthSpec.limit)
                    .as("network limit for " + pod.id)
                    .isGreaterThan(0L);
            }
        });
    }

    @Test
    public void nannyServiceExists() {
        assertThat(testCase.nannyServiceOptional)
            .as("https://nanny.yandex-team.ru/ui/#/services/catalog/" + testCase.podSet.getNannyServiceId())
            .isPresent();
    }

    private static List<PodSetData> getPodSets() {
        return Stream.of(Location.values())
            .flatMap(location -> getPodSets(location).stream())
            .collect(Collectors.toList());
    }

    private static List<PodSetData> getPodSets(Location location) {
        YpSelectStatement statement = YpSelectStatement.ysonBuilder(YpObjectType.POD_SET)
            .addSelector("/meta/id")
            .addSelector("/labels/nanny_service_id")
            .setFilter("[/spec/account_id] = \"abc:service:" + ABC_SERVICE_ID + "\"")
            .build();

        try {
            return YP_RAW_CLIENTS.get(location).objectService()
                .selectObjects(statement, attributes -> new PodSetData(
                    location,
                    stringFromYsonAttribute(attributes.get(0)),
                    stringFromYsonAttribute(attributes.get(1))))
                .get(YP_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                .getResults();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<PodData> getPods(Collection<PodSetIdWithLocation> podSetIds) {
        Map<Location, List<String>> podsByLocation = podSetIds.stream()
            .collect(Collectors.groupingBy(PodSetIdWithLocation::getLocation,
                Collectors.mapping(PodSetIdWithLocation::getPodSetId,
                    Collectors.toList())));

        return podsByLocation.entrySet().stream()
            .flatMap(entry -> getPods(entry.getKey(), entry.getValue()).stream())
            .collect(Collectors.toList());
    }

    private static List<PodData> getPods(Location location, Collection<String> podSetIds) {
        YpRawClient ypRawClient = YP_RAW_CLIENTS.get(location);
        List<PodData> result = new ArrayList<>();

        for (List<String> podSetIdsChunk : Lists.partition(new ArrayList<>(podSetIds), 200)) {
            try {
                String podSetIdListQueryPart = podSetIdsChunk.stream()
                    .map(podSetId -> "\"" + podSetId + "\"")
                    .collect(Collectors.joining(","));

                YpSelectStatement statement = YpSelectStatement.protobufBuilder(YpObjectType.POD)
                    .addSelector("/meta")
                    .addSelector("/spec")
                    .setFilter("[/meta/pod_set_id] IN (" + podSetIdListQueryPart + ")")
                    .build();

                result.addAll(ypRawClient.objectService()
                    .selectObjects(statement, attributes -> {
                        try {
                            Autogen.TPodMeta meta = Autogen.TPodMeta.parseFrom(
                                attributes.get(0).getProtobuf().orElseThrow());
                            DataModel.TPodSpec spec = DataModel.TPodSpec.parseFrom(
                                attributes.get(1).getProtobuf().orElseThrow());

                            return getPodData(meta, spec);
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .get(YP_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                    .getResults());
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }

        return unmodifiableList(result);
    }

    private static PodData getPodData(Autogen.TPodMeta meta, DataModel.TPodSpec spec) {
        Map<String, BandwidthSpec> bandwidthSpecsByMountPoint = spec.getDiskVolumeRequestsList().stream()
            .collect(Collectors.toMap(
                volume -> volume.getLabels().getAttributesList().stream()
                    .filter(attr -> attr.getKey().equals("mount_path"))
                    .map(attr -> attr.getValue().toString(StandardCharsets.UTF_8))
                    .findFirst()
                    .orElseThrow(),
                volume -> new BandwidthSpec(
                    volume.getQuotaPolicy().getBandwidthGuarantee(),
                    volume.getQuotaPolicy().getBandwidthLimit())));

        return new PodData(meta.getId(), meta.getPodSetId(),
            spec.getResourceRequests().getVcpuGuarantee(),
            bandwidthSpecsByMountPoint,
            new BandwidthSpec(spec.getResourceRequests().getNetworkBandwidthGuarantee(),
                spec.getResourceRequests().getNetworkBandwidthLimit()));
    }

    private static Map<String, Optional<NannyService>> getNannyServices(Collection<String> serviceIds) {
        Map<String, Optional<NannyService>> result = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(NANNY_THREADS);
        for (String serviceId : serviceIds) {
            executor.execute(() -> result.put(serviceId, NANNY_CLIENT.getOptionalService(serviceId)));
        }
        executor.shutdown();

        try {
            if (!executor.awaitTermination(NANNY_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                executor.shutdownNow();
                throw new RuntimeException("getNannyServices timed out");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return ImmutableMap.copyOf(result);
    }

    private static String stringFromYsonAttribute(YpPayload attribute) {
        ByteArrayInputStream input = new ByteArrayInputStream(attribute.getYson().orElseThrow().toByteArray());
        YTreeNode node = YTreeTextSerializer.deserialize(input);
        if (!node.isStringNode()) {
            return null;
        }

        return node.stringValue();
    }

    private static class PodSetData {
        final Location location;
        final String id;
        final String nannyServiceId;

        PodSetData(Location location, String id, String nannyServiceId) {
            this.location = location;
            this.id = id;
            this.nannyServiceId = nannyServiceId;
        }

        public Location getLocation() {
            return location;
        }

        public String getId() {
            return id;
        }

        public String getNannyServiceId() {
            return nannyServiceId;
        }

        @Override
        public String toString() {
            return id + " (nanny: " + nannyServiceId + ")";
        }
    }

    private static class PodData {
        final String id;
        final String podSetId;
        final Long cpuGuarantee;
        final Map<String, BandwidthSpec> bandwidthSpecsByMountPoint;
        final BandwidthSpec networkBandwidthSpec;

        PodData(String id, String podSetId, Long cpuGuarantee,
                Map<String, BandwidthSpec> bandwidthSpecsByMountPoint,
                BandwidthSpec networkBandwidthSpec) {
            this.id = id;
            this.podSetId = podSetId;
            this.cpuGuarantee = cpuGuarantee;
            this.bandwidthSpecsByMountPoint = bandwidthSpecsByMountPoint;
            this.networkBandwidthSpec = networkBandwidthSpec;
        }
    }

    private static class BandwidthSpec {
        final long guarantee;
        final long limit;

        private BandwidthSpec(long guarantee, long limit) {
            this.guarantee = guarantee;
            this.limit = limit;
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static class TestCase {
        final PodSetData podSet;
        final List<PodData> pods;
        final Optional<NannyService> nannyServiceOptional;

        TestCase(PodSetData podSet, List<PodData> pods, Optional<NannyService> nannyServiceOptional) {
            this.podSet = podSet;
            this.pods = pods;
            this.nannyServiceOptional = nannyServiceOptional;
        }

        @Override
        public String toString() {
            return podSet.toString();
        }
    }

    private enum Location {
        SAS("sas", YpInstance.SAS),
        IVA("iva", YpInstance.IVA);

        private final String name;
        private final YpInstance ypInstance;

        Location(String name, YpInstance ypInstance) {
            this.name = name;
            this.ypInstance = ypInstance;
        }

        public YpInstance getYpInstance() {
            return ypInstance;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class PodSetIdWithLocation {
        final String podSetId;
        final Location location;

        PodSetIdWithLocation(String podSetId, Location location) {
            this.podSetId = podSetId;
            this.location = location;
        }

        String getPodSetId() {
            return podSetId;
        }

        Location getLocation() {
            return location;
        }

        @Override
        public String toString() {
            return "PodSetIdWithLocation{" +
                "podSetId='" + podSetId + '\'' +
                ", location=" + location +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PodSetIdWithLocation that = (PodSetIdWithLocation) o;
            return podSetId.equals(that.podSetId) && location == that.location;
        }

        @Override
        public int hashCode() {
            return Objects.hash(podSetId, location);
        }
    }
}

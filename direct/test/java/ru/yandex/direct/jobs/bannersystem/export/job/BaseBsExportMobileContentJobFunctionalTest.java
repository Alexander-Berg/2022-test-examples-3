package ru.yandex.direct.jobs.bannersystem.export.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableMap;
import org.apache.http.entity.ContentType;
import org.asynchttpclient.AsyncHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.bannersystem.BannerSystemClient;
import ru.yandex.direct.bannersystem.BsHostType;
import ru.yandex.direct.bannersystem.BsUriFactory;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository;
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentService;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.steps.MobileContentSteps;
import ru.yandex.direct.jobs.bannersystem.export.service.BsExportMobileContentService;
import ru.yandex.direct.libs.curator.CuratorFrameworkProvider;
import ru.yandex.direct.solomon.SolomonPushClient;
import ru.yandex.direct.test.utils.MockedHttpWebServerExtention;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Базовый класс для функциональных тестов экспорта мобильного контента в БК.
 */
abstract class BaseBsExportMobileContentJobFunctionalTest {
    static final String REQUEST_PATH = BsUriFactory.IMPORT_APPLICATION_STORE_DATA.getUrlPath();

    @RegisterExtension
    static MockedHttpWebServerExtention server = new MockedHttpWebServerExtention(ContentType.APPLICATION_JSON);

    BsExportMobileContentJob job;

    @Autowired
    MobileContentSteps mobileContentSteps;

    @Autowired
    MobileContentRepository mobileContentRepository;

    @Autowired
    private MobileContentService mobileContentService;

    @Autowired
    private BsExportMobileContentService bsExportMobileContentService;

    @Autowired
    private AsyncHttpClient asyncHttpClient;

    @Autowired
    private CuratorFrameworkProvider curatorFrameworkProvider;

    @Autowired
    private SolomonPushClient solomonPushClient;

    @BeforeEach
    void before() {
        BsUriFactory bsUriFactory = new BsUriFactory(ImmutableMap.<BsHostType, String>builder()
                .put(BsHostType.IMPORT, server.getServerURL())
                .build());
        BannerSystemClient bannerSystemClient = new BannerSystemClient(bsUriFactory, asyncHttpClient);
        job = new BsExportMobileContentJob(bsExportMobileContentService,
                solomonPushClient, bannerSystemClient, mobileContentService, curatorFrameworkProvider);
    }

    List<List<Map<String, Object>>> performRequestsAndGetRequestBodies(int shard, int objectsPerIteration,
                                                                       List<MobileContentInfo> mobileContentInfoList,
                                                                       boolean iterateOnce) {
        performRequests(shard, objectsPerIteration, mobileContentInfoList, iterateOnce);
        return getRequestBodies();
    }

    void performRequests(MobileContentInfo mobileContentInfo) {
        performRequests(mobileContentInfo.getShard(), Integer.MAX_VALUE, Collections.singletonList(mobileContentInfo),
                false);
    }

    private void performRequests(int shard, int objectsPerIteration,
                                 List<MobileContentInfo> mobileContentInfoList, boolean iterateOnce) {
        List<String> responseList = new ArrayList<>();
        mobileContentInfoList
                .forEach(mci -> responseList.add(String.format("{\"mobile_app_id\":%s}", mci.getMobileContentId())));
        server.addResponse(REQUEST_PATH,
                String.format("{\"Result\":[%s]}", String.join(",", responseList)));

        try {
            job.runIterationsInLock(shard, objectsPerIteration,
                    mapList(mobileContentInfoList, MobileContentInfo::getMobileContentId), iterateOnce);
        } catch (Exception e) {
            throw new RuntimeException("Error in iteration", e);
        }
    }

    private List<List<Map<String, Object>>> getRequestBodies() {
        List<String> requestJsonList = server.getRequests(REQUEST_PATH);

        TypeFactory tf = JsonUtils.getTypeFactory();
        JavaType javaType = tf.constructCollectionType(
                List.class,
                tf.constructMapType(Map.class, String.class, Object.class));
        return mapList(requestJsonList, r -> JsonUtils.fromJson(r, javaType));
    }

    Map<String, Object> performRequestAndGetRequestItem(MobileContentInfo mobileContentInfo) {
        performRequests(mobileContentInfo);
        List<List<Map<String, Object>>> requests = getRequestBodies();
        assertThat(requests).size()
                .as("В запросе был только один объект")
                .isEqualTo(1);

        List<Map<String, Object>> request = requests.get(0);
        assertThat(request).size()
                .as("В запросе был только один объект")
                .isEqualTo(1);

        return request.get(0);
    }

    void checkBsSyncStatusIsCorrect(MobileContentInfo mobileContentInfo, StatusBsSynced expectedStatus) {
        MobileContent mobileContent = mobileContentRepository
                .getMobileContent(mobileContentInfo.getShard(), mobileContentInfo.getMobileContentId());

        assertThat(mobileContent.getStatusBsSynced())
                .as("Статус синхронизации установлен в %s", expectedStatus)
                .isEqualTo(expectedStatus);
    }
}

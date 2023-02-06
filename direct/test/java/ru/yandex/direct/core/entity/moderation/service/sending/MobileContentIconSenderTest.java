package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.mobilecontent.model.ContentType;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentWithModerationInfo;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.mobilecontent.model.StatusIconModerate;
import ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow;
import ru.yandex.direct.core.entity.moderation.model.mobilecontenticon.MobileContentIconModerationRequest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.MobileContentStatusiconmoderate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static ru.yandex.direct.core.entity.mobilecontent.model.StatusIconModerate.READY;
import static ru.yandex.direct.core.entity.mobilecontent.model.StatusIconModerate.SENDING;
import static ru.yandex.direct.core.entity.mobilecontent.model.StatusIconModerate.SENT;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.MOBILE_CONTENT_ICON;
import static ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames.DIRECT_SERVICE;
import static ru.yandex.direct.core.testing.data.TestMobileContents.androidMobileContent;
import static ru.yandex.direct.core.testing.data.TestMobileContents.iosMobileContent;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileContentIconSenderTest {
    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private MobileContentIconSender mobileContentIconSender;

    private int shard;

    private MobileContentInfo androidMobileContent;
    private MobileContentInfo iosMobileContent;

    @Before
    public void before() throws IOException {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        androidMobileContent = steps.mobileContentSteps().createMobileContent(
                new MobileContentInfo()
                        .withClientInfo(clientInfo)
                        .withMobileContent(androidMobileContent().withStatusIconModerate(READY)));
        iosMobileContent = steps.mobileContentSteps().createMobileContent(
                new MobileContentInfo()
                        .withClientInfo(clientInfo)
                        .withMobileContent(iosMobileContent().withStatusIconModerate(READY)));
    }

    @Test
    public void createNewVersionTest() {
        mobileContentIconSender.send(shard, List.of(androidMobileContent.getMobileContentId()),
                e -> System.currentTimeMillis(), e -> "", lst -> {});

        TestModerationRepository.ModerationVersion version = testModerationRepository.getMobileContentIconVersionObj(shard,
                androidMobileContent.getMobileContentId());
        assertThat(version.getVersion()).isEqualTo(MobileContentIconSender.INITIAL_VERSION);
        assertThat(version.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void incrementExistingVersionTest() {
        testModerationRepository.createMobileContentIconVersion(shard, androidMobileContent.getMobileContentId(),
                100L, LocalDateTime.now().minusDays(1));

        mobileContentIconSender.send(shard, List.of(androidMobileContent.getMobileContentId()),
                e -> System.currentTimeMillis(), e -> "", lst -> {});

        TestModerationRepository.ModerationVersion version =
                testModerationRepository.getMobileContentIconVersionObj(shard, androidMobileContent.getMobileContentId());
        assertThat(version.getVersion()).isEqualTo(101L);
        assertThat(version.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void versionChangeToInitialTest() {
        testModerationRepository.createMobileContentIconVersion(shard, androidMobileContent.getMobileContentId(),
                2L, LocalDateTime.now().minusDays(1));

        mobileContentIconSender.send(shard, List.of(androidMobileContent.getMobileContentId()),
                e -> System.currentTimeMillis(), e -> "", lst -> {});

        TestModerationRepository.ModerationVersion version =
                testModerationRepository.getMobileContentIconVersionObj(shard, androidMobileContent.getMobileContentId());
        assertThat(version.getVersion()).isEqualTo(MobileContentIconSender.INITIAL_VERSION);
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<MobileContentIconModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<MobileContentWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(READY, androidMobileContent.getMobileContentId());

        mobileContentIconSender.beforeSendTransaction(shard, List.of(androidMobileContent.getMobileContentId()),
                objects, requests, e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(SENDING, androidMobileContent.getMobileContentId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(MobileContentIconSender.INITIAL_VERSION);

        mobileContentIconSender.afterSendTransaction(shard, objects);

        checkStatusModerate(SENT, androidMobileContent.getMobileContentId());

        assertThat(testModerationRepository.getMobileContentIconVersionObj(
                shard, androidMobileContent.getMobileContentId()).getVersion())
                .isEqualTo(MobileContentIconSender.INITIAL_VERSION);
    }

    @Test
    public void versionChangeAfterRetry() {
        AtomicReference<List<MobileContentIconModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<MobileContentWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(READY, androidMobileContent.getMobileContentId());

        mobileContentIconSender.beforeSendTransaction(shard, List.of(androidMobileContent.getMobileContentId()),
                objects, requests, e -> System.currentTimeMillis(), el -> null);
        mobileContentIconSender.beforeSendTransaction(shard, List.of(androidMobileContent.getMobileContentId()),
                objects, requests, e -> System.currentTimeMillis(), el -> null);


        checkStatusModerate(SENDING, androidMobileContent.getMobileContentId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(11L);

        mobileContentIconSender.afterSendTransaction(shard, objects);

        checkStatusModerate(SENT, androidMobileContent.getMobileContentId());
        assertThat(testModerationRepository.getMobileContentIconVersionObj(
                shard, androidMobileContent.getMobileContentId()).getVersion())
                .isEqualTo(11L);
    }

    @Test
    public void checkDataIsCorrect() {
        AtomicReference<List<MobileContentIconModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<MobileContentWithModerationInfo>> objects = new AtomicReference<>();

        mobileContentIconSender.beforeSendTransaction(shard, List.of(androidMobileContent.getMobileContentId()),
                objects, requests, e -> System.currentTimeMillis(), el -> null);

        assertThat(requests.get()).hasSize(1);

        var request = requests.get().get(0);
        assertThat(request.getService()).isEqualTo(DIRECT_SERVICE);
        assertThat(request.getType()).isEqualTo(MOBILE_CONTENT_ICON);
        assertThat(request.getWorkflow()).isEqualTo(ModerationWorkflow.COMMON);

        var meta = request.getMeta();
        assertThat(meta.getVersionId()).isEqualTo(MobileContentIconSender.INITIAL_VERSION);
        assertThat(meta.getMobileContentId()).isEqualTo(androidMobileContent.getMobileContentId());
        assertThat(meta.getClientId()).isEqualTo(androidMobileContent.getClientId().asLong());
        assertThat(meta.getUid()).isEqualTo(androidMobileContent.getClientInfo().getUid());

        var data = request.getData();
        assertThat(data.getName()).isEqualTo(androidMobileContent.getMobileContent().getName());
        assertThat(data.getStoreCountry()).isEqualTo(androidMobileContent.getMobileContent().getStoreCountry());
        assertThat(data.getStoreAppId()).isEqualTo(androidMobileContent.getMobileContent().getStoreContentId());
        assertThat(data.getOsType()).isEqualTo(OsType.toSource(androidMobileContent.getMobileContent().getOsType()));
        assertThat(data.getContentType()).isEqualTo(ContentType.toSource(androidMobileContent.getMobileContent().getContentType()));
        assertThat(data.getStoreName()).isEqualTo("Google Play");
        assertThat(data.getIconUrl()).isNotNull();
    }

    @Test
    public void sendTwoObjects() {
        testModerationRepository.createMobileContentIconVersion(shard, androidMobileContent.getMobileContentId(),
                100L, LocalDateTime.now().minusDays(1));
        testModerationRepository.createMobileContentIconVersion(shard, iosMobileContent.getMobileContentId(),
                200L, LocalDateTime.now().minusDays(1));

        mobileContentIconSender.send(shard,
                List.of(androidMobileContent.getMobileContentId(), iosMobileContent.getMobileContentId()),
                e -> System.currentTimeMillis(), e -> "", lst -> {});

        TestModerationRepository.ModerationVersion version =
                testModerationRepository.getMobileContentIconVersionObj(shard, androidMobileContent.getMobileContentId());
        assertThat(version.getVersion()).isEqualTo(101L);
        assertThat(version.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));

        version = testModerationRepository.getMobileContentIconVersionObj(shard, iosMobileContent.getMobileContentId());
        assertThat(version.getVersion()).isEqualTo(201L);
        assertThat(version.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    private void checkStatusModerate(StatusIconModerate expectedStatusModerate, Long mobileContentId) {
        MobileContentStatusiconmoderate actualStatusModerate =
                testModerationRepository.getMobileContentIconStatusModerate(shard, mobileContentId);
        assertThat(actualStatusModerate).isEqualTo(StatusIconModerate.toSource(expectedStatusModerate));
    }
}

package ru.yandex.direct.core.entity.feed.service;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.mds.MdsHolder;
import ru.yandex.direct.core.entity.feed.UploadedToMdsFeedInformation;
import ru.yandex.direct.core.entity.mdsfile.model.MdsFileMetadata;
import ru.yandex.direct.core.entity.mdsfile.model.MdsStorageHost;
import ru.yandex.direct.core.entity.mdsfile.model.MdsStorageType;
import ru.yandex.direct.core.entity.mdsfile.repository.MdsFileRepository;
import ru.yandex.direct.core.entity.mdsfile.service.MdsFileService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestMdsConstants;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.MdsMetadataStorageHost;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.inside.mds.MdsFileKey;
import ru.yandex.inside.mds.MdsHosts;
import ru.yandex.inside.mds.MdsNamespace;
import ru.yandex.inside.mds.MdsPostResponse;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.ip.HostPort;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.HashingUtils.getMd5HashAsBase64YaStringWithoutPadding;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeedUploaderServiceTest {

    private static final HostPort MDS_HOST =
            new HostPort(MdsMetadataStorageHost.storage_int_mdst_yandex_net.getLiteral(), 80);

    @Autowired
    private Steps steps;

    @Mock
    private MdsHolder mdsHolder;

    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private MdsFileRepository mdsFileRepository;

    private FeedUploaderService feedUploaderService;
    private MdsFileService mdsFileService;
    private ClientInfo clientInfo;
    private ClientId clientId;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();

        MdsHosts mdsHosts = mock(MdsHosts.class);
        MdsNamespace mdsNamespace = mock(MdsNamespace.class);
        MdsPostResponse mdsPostResponse = mock(MdsPostResponse.class);
        mdsHolder = mock(MdsHolder.class);
        when(mdsHosts.getHostPortForRead()).thenReturn(MDS_HOST);
        when(mdsNamespace.getName()).thenReturn("direct-files");
        when(mdsHolder.upload(anyString(), any(InputStreamSource.class))).thenAnswer(
                invocation -> {
                    String path = invocation.getArgument(0);
                    when(mdsPostResponse.getKey()).thenReturn(new MdsFileKey(1234, path));
                    return mdsPostResponse;
                }
        );
        when(mdsHolder.getHosts()).thenReturn(mdsHosts);
        when(mdsHolder.getNamespace()).thenReturn(mdsNamespace);
        when(mdsHolder.downloadUrl(anyString(), any())).thenAnswer(
                invocation -> {
                    MdsFileKey fileKey = invocation.getArgument(1);
                    return String.join("/",
                            "http://" + MDS_HOST.getHost(),
                            String.valueOf(fileKey.getGroup()),
                            fileKey.getFilename()
                    );
                }
        );
        mdsFileService = new MdsFileService(shardHelper, mdsFileRepository, mdsHolder);
        feedUploaderService = new FeedUploaderService(mdsFileService);
    }

    @Test
    public void uploadFeedToMds_new() {
        byte[] feedData = "<xml></xml>".getBytes();
        String hash = getMd5HashAsBase64YaStringWithoutPadding(feedData);
        String suffix = getMdsSuffix(hash);
        String expectedUrl = TestMdsConstants.TEST_HOST_URL + suffix;
        UploadedToMdsFeedInformation expectedInfo = new UploadedToMdsFeedInformation()
                .withUrl(expectedUrl)
                .withFileHash(hash);
        UploadedToMdsFeedInformation actualUploadedInfo = feedUploaderService.uploadToMds(clientId, feedData);
        MdsFileMetadata actualMetadata =
                mdsFileRepository.getMetadata(clientInfo.getShard(), clientId, List.of(hash)).get(hash);
        MdsFileMetadata expectedMetadata = new MdsFileMetadata()
                .withClientId(clientId.asLong())
                .withStorageHost(MdsStorageHost.STORAGE_INT_MDST_YANDEX_NET)
                .withMdsKey(suffix)
                .withFileImprint(hash)
                .withFilename(hash)
                .withType(MdsStorageType.PERF_FEEDS);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualUploadedInfo)
                    .as("Информация о загруженном фиде должна совпадать")
                    .is(matchedBy(beanDiffer(expectedInfo)));
            soft.assertThat(actualMetadata)
                    .as("Информация о загруженном файле в MDS должна совпадать")
                    .is(matchedBy(beanDiffer(expectedMetadata).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void uploadFeedToMds_alreadyUploaded() {
        byte[] feedData = "<xml></xml>".getBytes();
        String hash = getMd5HashAsBase64YaStringWithoutPadding(feedData);
        String suffix = getMdsSuffix(hash);
        MdsFileMetadata metadata = new MdsFileMetadata()
                .withClientId(clientId.asLong())
                .withMdsKey(suffix)
                .withStorageHost(MdsStorageHost.STORAGE_INT_MDST_YANDEX_NET)
                .withFileImprint(hash)
                .withFilename(hash)
                .withSize((long) feedData.length)
                .withType(MdsStorageType.PERF_FEEDS);
        mdsFileRepository.addMetadata(clientInfo.getShard(), List.of(metadata));
        feedUploaderService.uploadToMds(clientId, feedData);
        verify(spy(mdsFileService), times(0)).saveMdsFiles(anyList(), any());
    }

    private String getMdsSuffix(String hash) {
        return 1234 + "/" + "perf_feeds/" + clientId + "/" + hash;
    }
}

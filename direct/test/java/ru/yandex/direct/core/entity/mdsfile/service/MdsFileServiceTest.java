package ru.yandex.direct.core.entity.mdsfile.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.mds.MdsHolder;
import ru.yandex.direct.core.entity.mdsfile.model.MdsFileCustomName;
import ru.yandex.direct.core.entity.mdsfile.model.MdsFileMetadata;
import ru.yandex.direct.core.entity.mdsfile.model.MdsFileSaveRequest;
import ru.yandex.direct.core.entity.mdsfile.model.MdsStorageHost;
import ru.yandex.direct.core.entity.mdsfile.model.MdsStorageType;
import ru.yandex.direct.core.entity.mdsfile.repository.MdsFileRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestMdsFile;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.MdsMetadataStorageHost;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.liveresource.LiveResourceFactory;
import ru.yandex.inside.mds.MdsHosts;
import ru.yandex.inside.mds.MdsInternalProxies;
import ru.yandex.misc.ip.HostPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MdsFileServiceTest {

    @Autowired
    private Steps steps;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private MdsFileRepository mdsFileRepository;
    @Autowired
    private MdsHolder directFilesMds;

    @Autowired
    private MdsFileService mdsFileService;

    private MdsFileRepository mdsFileRepositorySpy;
    private ClientInfo clientInfo;
    private int shard;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();


        mdsFileRepositorySpy = Mockito.spy(mdsFileRepository);
        mdsFileService = new MdsFileService(shardHelper, mdsFileRepositorySpy, directFilesMds,
                MdsStorageHost.STORAGE_INT_MDST_YANDEX_NET);
        var hostPort = new HostPort(MdsMetadataStorageHost.storage_int_mdst_yandex_net.getLiteral(), 80);
        var mdsHosts = new MdsHosts(hostPort, hostPort, mock(MdsInternalProxies.class));
        doReturn(mdsHosts).when(directFilesMds).getHosts();
    }

    /**
     * Если при вызове метода {@link MdsFileService#deleteOldMdsFiles} данные из MDS_Metadata таблицы будут разбиты на
     * несколько чанков -> функция удаления {@link MdsFileRepository#deleteMetadata} метаданных будет вызвана
     * 'суммарное количество элементов из всех чанков' раз
     */
    @Test
    public void deleteOldMdsFiles_whenManyChunksOfMetadata() {
        doReturn(List.of(getTestMdsMetadata(5L)),
                List.of(getTestMdsMetadata(6L)),
                Collections.emptyList())
                .when(mdsFileRepositorySpy).getMetadataLessThanCreateTimeWithSort(anyInt(),
                any(MdsStorageType.class), any(LocalDateTime.class), anyLong(), anyInt());
        mdsFileService.deleteOldMdsFiles(shard, LocalDateTime.now(), MdsStorageType.XLS_HISTORY);
        verify(mdsFileRepositorySpy).deleteMetadata(shard, 5L);
        verify(mdsFileRepositorySpy).deleteMetadata(shard, 6L);
        // проверка на количество вызовов метода (количество итераций)
        verify(mdsFileRepositorySpy, times(2)).deleteMetadata(anyInt(), anyLong());
    }

    /**
     * Тест: если вызвать метод {@link MdsFileService#deleteMdsFile} для удаления существующего файла с записями о
     * нем в таблицах mds_custom_names и mds_metadata -> записи о файле в таблицах будут удалены
     */
    @Test
    public void deleteMdsFile() {
        MdsFileSaveRequest saveRequest = new MdsFileSaveRequest(MdsStorageType.XLS_HISTORY, "ok".getBytes())
                .withCustomName("a6549eec5a5327432eae60f4f482c852_1550615227.xls");
        MdsFileSaveRequest mdsFileSaveRequest = mdsFileService.saveMdsFiles(List.of(saveRequest),
                clientInfo.getClientId().asLong()).get(0);

        mdsFileService.deleteMdsFile(shard, mdsFileSaveRequest.getMdsMetadata());

        MdsFileMetadata mdsFileMetadata = steps.mdsFileSteps().getMetadata(shard,
                mdsFileSaveRequest.getMdsMetadata().getId());
        List<MdsFileCustomName> mdsCustomNames = steps.mdsFileSteps().getCustomName(shard,
                mdsFileSaveRequest.getMdsMetadata().getId());

        assertThat(mdsFileMetadata).isNull();
        assertThat(mdsCustomNames).isEmpty();
    }

    @Test
    public void getStorageHostTest() {
        var mdsHolder = MdsHolder.instance(
                MdsMetadataStorageHost.storage_int_mds_yandex_net.getLiteral() + ":80",
                "writehost:12345",
                "namespace",
                10,
                LiveResourceFactory.emptyResource(), new DefaultManagedTaskScheduler(), 10, 10);
        var mdsStorageHost = MdsFileService.getStorageHost(mdsHolder);
        assertThat(mdsStorageHost).isEqualTo(MdsStorageHost.STORAGE_INT_MDS_YANDEX_NET);
    }

    private MdsFileMetadata getTestMdsMetadata(long id) {
        return TestMdsFile.testMdsFileMetadata(null)
                .withStorageHost(MdsStorageHost.STORAGE_INT_MDST_YANDEX_NET)
                .withId(id)
                .withCreateTime(LocalDateTime.now())
                .withMdsKey("1095/YNDsiTx1wjsI8jHqttLaYw");
    }
}

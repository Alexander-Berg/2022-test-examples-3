package ru.yandex.direct.core.entity.mdsfile.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.mdsfile.model.MdsFileCustomName;
import ru.yandex.direct.core.entity.mdsfile.model.MdsFileMetadata;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestMdsFile;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MdsFileRepositoryTest {
    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private int shard;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
    }

    @Autowired
    private MdsFileRepository mdsFileRepository;


    @Test
    public void addMetadata_SuccessfulSave() {
        MdsFileMetadata mdsFile = TestMdsFile.testMdsFileMetadata(clientInfo.getClientId());
        List<Long> addedIds = mdsFileRepository.addMetadata(shard, singletonList(mdsFile));

        assertThat(addedIds, hasSize(1));
        assertThat(addedIds.get(0), greaterThan(Long.valueOf(0)));
    }

    @Test
    public void addCustomName_SuccessfulSave() {
        MdsFileCustomName mdsCustomName = TestMdsFile.testMdsFileCustomName(1L);
        List<Long> modifiedIds = mdsFileRepository.addCustomName(shard, singletonList(mdsCustomName));

        assertThat(modifiedIds, hasSize(1));
        assertThat(modifiedIds.get(0), is(mdsCustomName.getMdsId()));
    }


    /**
     * Тест: если отправить запрос на удаление существующей в таблице mds_metadata записи по id -> функция удаленния
     * верет 1 (количество удаленных строк) и запись пропадет из таблицы
     */
    @Test
    public void deleteMetadata() {
        MdsFileMetadata mdsFile = TestMdsFile.testMdsFileMetadata(clientInfo.getClientId());
        long id = mdsFileRepository.addMetadata(shard, List.of(mdsFile)).get(0);

        int countDeleted = mdsFileRepository.deleteMetadata(shard, id);
        MdsFileMetadata mdsMetadataIdAfter = steps.mdsFileSteps().getMetadata(shard, id);

        assertThat(countDeleted, equalTo(1));
        assertThat(mdsMetadataIdAfter, nullValue());
    }

    /**
     * Тест: если отправить запрос на удаление существующих в таблице mds_custom_name записей по mds_id -> функция
     * удаленния верет количество этих записей ( = количеству удаленных строк) и они удалятся из таблицы
     */
    @Test
    public void deleteCustomNames() {
        MdsFileCustomName mdsCustomName1 = TestMdsFile.testMdsFileCustomName(1L);
        MdsFileCustomName mdsCustomName2 = TestMdsFile.testMdsFileCustomName(1L);
        long mdsId = mdsFileRepository.addCustomName(shard, List.of(mdsCustomName1, mdsCustomName2)).get(0);

        int countDeleted = mdsFileRepository.deleteCustomNames(shard, mdsId);
        List<MdsFileCustomName> mdsFileCustomNames = steps.mdsFileSteps().getCustomName(shard, mdsId);

        assertThat(countDeleted, equalTo(2));
        assertThat(mdsFileCustomNames, empty());
    }
}

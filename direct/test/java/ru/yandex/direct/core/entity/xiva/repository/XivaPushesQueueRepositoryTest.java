package ru.yandex.direct.core.entity.xiva.repository;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.xiva.model.XivaPushesQueueItem;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbschema.ppc.enums.XivaPushesQueuePushType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.XIVA_PUSHES_QUEUE;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class XivaPushesQueueRepositoryTest {

    private static final int SHARD = 1;
    private static final XivaPushesQueuePushType PUSH = XivaPushesQueuePushType.FAKE_PUSH;

    @Autowired
    private XivaPushesQueueRepository xivaPushesQueueRepository;
    @Autowired
    private DslContextProvider dslContextProvider;

    private void deletePreviousAndAddNewPushes() {
        deleteAllPushes();

        // TODO: использовать в репозитории какой-нибудь datetime provider и обойтись без Thread.sleep
        xivaPushesQueueRepository.addPushToQueue(SHARD, 0L, PUSH);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        xivaPushesQueueRepository.addPushToQueue(SHARD, 1L, PUSH);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        xivaPushesQueueRepository.addPushToQueue(SHARD, 2L, PUSH);
    }

    private void deleteAllPushes() {
        // удаляем из таблицы посторонние записи
        dslContextProvider.ppc(SHARD)
                .truncate(XIVA_PUSHES_QUEUE)
                .execute();
    }

    @Test
    public void testGet() {
        deletePreviousAndAddNewPushes();
        List<XivaPushesQueueItem> got = xivaPushesQueueRepository.getTopPushes(SHARD, 1);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(got.size()).isEqualTo(1);
        soft.assertThat(got.get(0).getClientId())
                .isEqualTo(0);
        soft.assertThat(got.get(0).getPushType().toString())
                .isEqualTo(PUSH.toString());
        soft.assertThat(got.get(0).getAddTime())
                .isBeforeOrEqualTo(LocalDateTime.now().plusSeconds(1));
        soft.assertAll();
    }

    @Test
    public void testLimit() {
        deletePreviousAndAddNewPushes();
        List<XivaPushesQueueItem> got = xivaPushesQueueRepository.getTopPushes(SHARD, 2);
        assertThat(got.size()).isEqualTo(2);
    }

    @Test
    public void testDeleteOne() {
        deletePreviousAndAddNewPushes();
        xivaPushesQueueRepository.deletePush(SHARD, 0L, PUSH);
        List<XivaPushesQueueItem> got = xivaPushesQueueRepository.getTopPushes(SHARD, 3);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(got.size()).isLessThan(3);
        soft.assertThat(got.get(0).getClientId())
                .isNotEqualTo(0L);
        soft.assertAll();
    }

    @Test
    public void testDeleteBatch() {
        deletePreviousAndAddNewPushes();
        List<XivaPushesQueueItem> toDelete = new LinkedList<XivaPushesQueueItem>() {
        };
        toDelete.add(new XivaPushesQueueItem()
                .withClientId(0L)
                .withPushType(ru.yandex.direct.core.entity.xiva.model.XivaPushesQueuePushType.FAKE_PUSH)
        );
        toDelete.add(new XivaPushesQueueItem()
                .withClientId(1L)
                .withPushType(ru.yandex.direct.core.entity.xiva.model.XivaPushesQueuePushType.FAKE_PUSH)
        );

        xivaPushesQueueRepository.deletePushes(SHARD, toDelete);

        List<XivaPushesQueueItem> got = xivaPushesQueueRepository.getTopPushes(SHARD, 3);
        assertThat(got.size()).isEqualTo(1);
    }
}

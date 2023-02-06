package ru.yandex.market.mbo.mdm.common.masterdata.repository.queue;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuQueueInfo;

/**
 * @author dmserebr
 * @date 04/03/2020
 */
public class MskuToRefreshRepositoryImplTest
    extends MdmPriorityQueueRepositoryTestBase<MdmMskuQueueInfo, Long> {

    @Autowired
    private MskuToRefreshRepositoryImpl repository;

    @Override
    protected MdmQueueBaseRepository<MdmMskuQueueInfo, Long> getRepository() {
        return repository;
    }

    @Override
    protected Long getRandomKey() {
        return random.nextLong();
    }

    @Override
    protected MdmMskuQueueInfo createEmptyInfo() {
        return new MdmMskuQueueInfo();
    }
}

package ru.yandex.market.mbo.mdm.common.masterdata.repository.queue;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuIdOrOrphanSskuKeyInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuIdOrShopSkuKeyContainer;

public class MskuAndSskuQueueImplOrphansTest
    extends MdmPriorityQueueRepositoryTestBase<MdmMskuIdOrOrphanSskuKeyInfo, MdmMskuIdOrShopSkuKeyContainer> {

    @Autowired
    private MskuAndSskuQueue repository;

    @Override
    protected MdmQueueBaseRepository<MdmMskuIdOrOrphanSskuKeyInfo, MdmMskuIdOrShopSkuKeyContainer>
    getRepository() {
        return repository;
    }

    @Override
    protected MdmMskuIdOrShopSkuKeyContainer getRandomKey() {
        if (random.nextInt(10) % 2 == 0) {
            return MdmMskuIdOrShopSkuKeyContainer.ofSsku(random.nextInt(), String.valueOf(random.nextInt()));
        }
        return MdmMskuIdOrShopSkuKeyContainer.ofMsku(random.nextLong());
    }

    @Override
    protected MdmMskuIdOrOrphanSskuKeyInfo createEmptyInfo() {
        return new MdmMskuIdOrOrphanSskuKeyInfo();
    }
}

package ru.yandex.market.mbo.mdm.common.masterdata.repository.queue;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author dmserebr
 * @date 04/03/2020
 */
public class SskuToRefreshRepositoryImplTest
    extends MdmPriorityQueueRepositoryTestBase<SskuToRefreshInfo, ShopSkuKey> {

    @Autowired
    private SskuToRefreshRepositoryImpl repository;

    @Override
    protected MdmQueueBaseRepository<SskuToRefreshInfo, ShopSkuKey> getRepository() {
        return repository;
    }

    @Override
    protected ShopSkuKey getRandomKey() {
        return new ShopSkuKey(random.nextInt(), String.valueOf(random.nextInt()));
    }

    @Override
    protected SskuToRefreshInfo createEmptyInfo() {
        return new SskuToRefreshInfo();
    }
}

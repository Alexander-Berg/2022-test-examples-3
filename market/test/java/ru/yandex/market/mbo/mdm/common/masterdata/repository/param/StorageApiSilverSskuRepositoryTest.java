package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import java.util.List;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku.BmdmEntityToSilverCommonSskuConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.proto.MdmEntityStorageService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class StorageApiSilverSskuRepositoryTest extends SilverSskuRepositoryTest {
    @Autowired
    private MdmEntityStorageService mdmEntityStorageService;
    @Autowired
    private BmdmEntityToSilverCommonSskuConverter bmdmEntityToSilverCommonSskuConverter;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;

    private StorageApiSilverSskuRepository storageApiSilverSskuRepository;

    @Before
    public void setUpStorageApiSilverSskuRepository() {
        this.storageApiSilverSskuRepository = new StorageApiSilverSskuRepository(
            mdmEntityStorageService,
            bmdmEntityToSilverCommonSskuConverter,
            mdmSskuGroupManager
        );
    }

    @Override
    protected SilverSskuRepository repository() {
        return storageApiSilverSskuRepository;
    }

    @Test
    public void whenSearchBySingleSskuKeyReturnAllGroupCommonSskus() {
        // given
        Random random = new Random("Тамм Игорь Евгеньевич".hashCode());

        String shopSku = "U-238";
        MasterDataSource source = new MasterDataSource(MasterDataSourceType.MEASUREMENT, "304");

        ShopSkuKey serviceKey = new ShopSkuKey(SERVICE1, shopSku);
        ShopSkuKey businessKey = new ShopSkuKey(BUSINESS, shopSku);
        sskuExistenceRepository.markExistence(List.of(serviceKey, businessKey), true);

        SilverSskuKey silverServiceKey = new SilverSskuKey(serviceKey, source);
        SilverSskuKey silverBusinessKey = new SilverSskuKey(businessKey, source);

        SilverCommonSsku serviceSilverCommonSsku = new SilverCommonSsku(silverServiceKey);
        KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(serviceSilverCommonSsku::addBaseValue);

        SilverCommonSsku businessSilverCommonSsku = new SilverCommonSsku(silverBusinessKey);
        KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(businessSilverCommonSsku::addBaseValue);

        repository().insertOrUpdateSskus(List.of(businessSilverCommonSsku, serviceSilverCommonSsku));

        // when
        List<SilverCommonSsku> searchResult = repository().findSsku(serviceKey);

        // then
        Assertions.assertThat(searchResult)
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .containsExactlyInAnyOrder(serviceSilverCommonSsku, businessSilverCommonSsku);
    }
}

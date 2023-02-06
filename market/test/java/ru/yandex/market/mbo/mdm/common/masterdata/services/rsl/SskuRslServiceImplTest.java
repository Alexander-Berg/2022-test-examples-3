package ru.yandex.market.mbo.mdm.common.masterdata.services.rsl;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;

public class SskuRslServiceImplTest extends MdmBaseDbTestClass {

    private SskuRslServiceImpl sskuRslService;

    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;

    @Autowired
    private BeruId beruId;


    @Before
    public void setup() {
        generateMappingsMock(mappingsCacheRepository);
        this.sskuRslService = new SskuRslServiceImpl(beruId, mappingsCacheRepository);
    }

    @Test
    public void getByFirstPartySupplierAndRealSupplier() {
        var mockRslSupplierAndRealSupplier = generateRslSupplierAndRealSupplier();
        var result = sskuRslService.findSskuRealatedToFirstPartySupplierAndRealSupplier(
            mockRslSupplierAndRealSupplier);
        Assert.assertEquals(2, result.size());
    }

    private void generateMappingsMock(MappingsCacheRepository mock) {
        mock.insert(generateDaoItem(465852, "000191.1"));
        mock.insert(generateDaoItem(465852, "000191.2"));
        mock.insert(generateDaoItem(2, "not"));
        mock.insert(generateDaoItem(2, "2.not"));
        mock.insert(generateDaoItem(7, "77.not.77"));
        mock.insert(generateDaoItem(300, "3.3"));
    }

    private MappingCacheDao generateDaoItem(int supplierId, String shopSku) {
        var dao = new MappingCacheDao();
        dao.setShopSku(shopSku);
        dao.setSupplierId(supplierId);
        return dao;
    }

    private Map<Integer, Set<String>> generateRslSupplierAndRealSupplier() {
        var realSupList = new HashSet<String>();
        realSupList.add("000191");//in mock, 2 cases
        realSupList.add("17");//not in mock
        Map<Integer, Set<String>> result = new LinkedHashMap<>();
        result.put(465852, realSupList); //in mock

        realSupList = new HashSet<>();
        realSupList.add("100");//not in mock
        realSupList.add("200");//not in mock
        result.put(2, realSupList);//not in mock

        realSupList = new HashSet<>();
        realSupList.add("3");//in mock
        result.put(300, realSupList);//not in mock, but not 1p

        return result;
    }
}

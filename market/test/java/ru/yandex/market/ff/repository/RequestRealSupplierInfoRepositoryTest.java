package ru.yandex.market.ff.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.RequestRealSupplierInfo;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class RequestRealSupplierInfoRepositoryTest extends IntegrationTest {
    @Autowired
    private RequestRealSupplierInfoRepository requestRealSupplierInfoRepository;

    @Test
    @DatabaseSetup("classpath:repository/request_real_supplier_info/before.xml")
    @ExpectedDatabase(value = "classpath:repository/request_real_supplier_info/after.xml", assertionMode = NON_STRICT)
    public void testSave() {
        requestRealSupplierInfoRepository.save(Arrays.asList(
                new RequestRealSupplierInfo(null, 1L, "00001", "abc"),
                new RequestRealSupplierInfo(null, 1L, "00002", "def")
        ));
    }

    @Test
    @DatabaseSetup("classpath:repository/request_real_supplier_info/before.xml")
    @ExpectedDatabase(value = "classpath:repository/request_real_supplier_info/after-save-empty.xml",
            assertionMode = NON_STRICT)
    public void testSaveEmpty() {
        List<RequestRealSupplierInfo> list = new ArrayList<>();
        requestRealSupplierInfoRepository.save(list);
    }
}

package ru.yandex.market.ff.service.implementation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.service.SupplierService;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.fulfillment.SupplierInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SupplierServiceTest extends IntegrationTest {

    @Autowired
    SupplierService supplierService;

    @Autowired
    MbiApiClient mbiApiClient;

    @Test
    @DatabaseSetup("classpath:service/supplier-service/before-find.xml")
    @ExpectedDatabase(value = "classpath:service/supplier-service/after-find.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testFindByIdsAndSave() {
        //given
        when(mbiApiClient.getSupplierInfoListByFilter(any()))
                .thenReturn(List.of(
                        new SupplierInfo.Builder()
                                .setId(3)
                                .setName("supplier3")
                                .build()
                ));

        //when
        Collection<Supplier> foundSuppliers = supplierService.findAllByIdsAndSaveAbsent(List.of(1L, 2L, 3L, 4L));
        List<Long> foundSupplierIds = foundSuppliers.stream().map(Supplier::getId).collect(Collectors.toList());

        //then
        assertThat(foundSupplierIds)
                .hasSize(3)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DatabaseSetup("classpath:service/supplier-service/before-find.xml")
    public void testFindByIdsAndSaveWithNothingInMbi() {
        //given
        when(mbiApiClient.getSupplierInfoListByFilter(any()))
                .thenReturn(List.of(
                        new SupplierInfo.Builder()
                                .setId(3)
                                .setName("supplier3")
                                .build()
                ));

        //when
        supplierService.findAllByIdsAndSaveAbsent(List.of(1L, 2L));

        //then
        //Если нашли всех поставщиков, то mbiApiClient вообще не дергаем
        verify(mbiApiClient, never()).getSupplierInfoListByFilter(any());
    }
}

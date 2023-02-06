package ru.yandex.market.mbi.api.controller.fulfillment;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.client.entity.fulfillment.FulfillmentSupplierFilter;
import ru.yandex.market.mbi.api.client.entity.fulfillment.SupplierInfo;
import ru.yandex.market.mbi.api.config.FunctionalTest;

public class FulfillmentControllerNameTest extends FunctionalTest {

    private static Stream<Arguments> suppliers() {
        return Stream.of(
                Arguments.of(1001L, "ООО supplier 1"),
                Arguments.of(1002L, "ЗАО supplier 2"),

                //если тип входит в имя, он не добавляется (тип ЗАО, имя ЗАО supplier 3)
                Arguments.of(1003L, "ЗАО supplier 3"),
                Arguments.of(1004L, "ИП supplier 4"),
                Arguments.of(1005L, "ЧП supplier 5"),
                Arguments.of(1006L, "supplier 6"),
                Arguments.of(1007L, "ОАО supplier 7"),
                Arguments.of(1008L, "АО supplier 8"),
                Arguments.of(1009L, "Физ. лицо supplier 9"),
                Arguments.of(1100L, null) //у REAL_SUPPLIER не заполняется organization name

        );
    }

    @ParameterizedTest
    @MethodSource("suppliers")
    @DbUnitDataSet(before = "FulfillmentControllerTest.allowAllSuppliers.before.csv")
    public void testAllowAllSuppliersEnv(long supplierId, String expectedOrganizationName) {
        FulfillmentSupplierFilter filter = new FulfillmentSupplierFilter.Builder()
                .setSupplierIds(List.of(supplierId))
                .build();

        List<SupplierInfo> result = mbiApiClient.getSupplierInfoListByFilter(filter);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(expectedOrganizationName, result.get(0).getOrganisationName());
    }

    private static Stream<Arguments> supplierIdData() {
        return Stream.of(
                Arguments.of(1001L, "supplier 1"), //для этого поставщика env-параметр не включён
                Arguments.of(1002L, "ООО supplier 2") //для этого поставщика env-параметр включён
        );
    }

    @ParameterizedTest
    @MethodSource("supplierIdData")
    @DbUnitDataSet(before = "FulfillmentControllerTest.supplierId.before.csv")
    public void testSupplierIdEnv(long supplierId, String expectedOrganizationName) {
        FulfillmentSupplierFilter filter = new FulfillmentSupplierFilter.Builder()
                .setSupplierIds(List.of(supplierId))
                .build();

        List<SupplierInfo> result = mbiApiClient.getSupplierInfoListByFilter(filter);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(expectedOrganizationName, result.get(0).getOrganisationName());
    }

    @Test
    @DbUnitDataSet(before = "FulfillmentControllerTest.supplierId.before.csv")
    public void testGetSupplierInfoList() {
        List<SupplierInfo> result = mbiApiClient.getSupplierInfoList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("supplier 1", result.get(0).getOrganisationName());
        Assertions.assertEquals("ООО supplier 2", result.get(1).getOrganisationName());
    }
}

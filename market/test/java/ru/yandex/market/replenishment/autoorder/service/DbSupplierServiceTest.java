package ru.yandex.market.replenishment.autoorder.service;

import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Supplier;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
@DbUnitDataSet(before = "DbSupplierServiceTest.before.csv")
public class DbSupplierServiceTest extends FunctionalTest {

    private static final boolean WITH_RS_ID_DEFAULT_VALUE = false;

    @Autowired
    private DbSupplierService dbSupplierService;

    @Test
    public void findSuppliersWithIgnoreCaseLatinFilterTest() {
        String[] expectedNames = {
                "abc left low case",
                "centre aBc mix case",
                "centre AbC mix case",
                "right upper case ABC"
        };

        List<Supplier> actualSuppliers1 =
                dbSupplierService.findSuppliersByName("abc", WITH_RS_ID_DEFAULT_VALUE);
        assertThat(actualSuppliers1, hasSize(4));
        assertStreamEquals(
                actualSuppliers1.stream().map(Supplier::getName),
                Stream.of(expectedNames)
        );

        List<Supplier> actualSuppliers2 =
                dbSupplierService.findSuppliersByName("ABC", WITH_RS_ID_DEFAULT_VALUE);
        assertThat(actualSuppliers2, hasSize(4));
        assertStreamEquals(
                actualSuppliers2.stream().map(Supplier::getName),
                Stream.of(expectedNames)
        );

        List<Supplier> actualSuppliers3 =
                dbSupplierService.findSuppliersByName("aBc", WITH_RS_ID_DEFAULT_VALUE);
        assertThat(actualSuppliers3, hasSize(4));
        assertStreamEquals(
                actualSuppliers3.stream().map(Supplier::getName),
                Stream.of(expectedNames)
        );
    }

    @Test
    public void findSuppliersWithIgnoreCaseCyrillicFilterTest() {
        String[] expectedNames = {
                "абв left low case",
                "centre аБв mix case",
                "centre АбВ mix case",
                "right upper case АБВ"
        };

        List<Supplier> actualSuppliers1 =
                dbSupplierService.findSuppliersByName("абв", WITH_RS_ID_DEFAULT_VALUE);
        assertThat(actualSuppliers1, hasSize(4));
        assertStreamEquals(
                actualSuppliers1.stream().map(Supplier::getName),
                Stream.of(expectedNames)
        );

        List<Supplier> actualSuppliers2 =
                dbSupplierService.findSuppliersByName("АБВ", WITH_RS_ID_DEFAULT_VALUE);
        assertThat(actualSuppliers2, hasSize(4));
        assertStreamEquals(
                actualSuppliers2.stream().map(Supplier::getName),
                Stream.of(expectedNames)
        );

        List<Supplier> actualSuppliers3 =
                dbSupplierService.findSuppliersByName("аБв", WITH_RS_ID_DEFAULT_VALUE);
        assertThat(actualSuppliers3, hasSize(4));
        assertStreamEquals(
                actualSuppliers3.stream().map(Supplier::getName),
                Stream.of(expectedNames)
        );
    }

    @Test
    public void findSuppliersNameFilterWithoutRsIdTest() {
        String[] expectedNames = {
                "supplierwrsi left low case",
                "supplierwrsi left low case",
                "supplierWRSI mix case",
                "supplierWRSI mix case",
                "SupplierWRSI mix case",
                "SupplierWRSI mix case",
                "SUPPLIERWRSI upper case",
                "SUPPLIERWRSI upper case"
        };

        List<Supplier> actualSuppliers1 =
                dbSupplierService.findSuppliersByName("SupplierWRSI", false);
        assertThat(actualSuppliers1, hasSize(8));
        assertStreamEquals(
                actualSuppliers1.stream().map(Supplier::getName),
                Stream.of(expectedNames)
        );
    }

    @Test
    public void findSuppliersNameFilterWithRsIdTest() {
        String[] expectedNames = {
                "supplierwrsi left low case",
                "supplierWRSI mix case",
                "SupplierWRSI mix case",
                "SUPPLIERWRSI upper case"
        };

        List<Supplier> actualSuppliers1 =
                dbSupplierService.findSuppliersByName("SupplierWRSI", true);
        assertThat(actualSuppliers1, hasSize(4));
        assertStreamEquals(
                actualSuppliers1.stream().map(Supplier::getName),
                Stream.of(expectedNames)
        );
    }

    @Test
    public void findSuppliersFirst50WithoutFilterTest() {
        List<Supplier> actualSuppliers =
                dbSupplierService.findSuppliersByName(null, WITH_RS_ID_DEFAULT_VALUE);
        assertThat(actualSuppliers, hasSize(50));
        assertStreamEquals(
                actualSuppliers.stream().map(Supplier::getName).sorted(),
                generateExpectedStream()
        );
    }

    @Test
    public void findSuppliersFirst50ByFilterTest() {
        List<Supplier> actualSuppliers =
                dbSupplierService.findSuppliersByName("TEST", WITH_RS_ID_DEFAULT_VALUE);
        assertThat(actualSuppliers, hasSize(50));
        assertStreamEquals(
                actualSuppliers.stream().map(Supplier::getName).sorted(),
                generateExpectedStream()
        );
    }

    private Stream<String> generateExpectedStream() {
        return IntStream.rangeClosed(1, 50).boxed().map(i -> "123TEST " + (i < 10 ? "0" : "") + i);
    }

    private static void assertStreamEquals(Stream<?> actual, Stream<?> expected) {
        Iterator<?> actualIt = actual.iterator();
        Iterator<?> expectedIt = expected.iterator();
        while (actualIt.hasNext() && expectedIt.hasNext()) {
            assertThat(actualIt.next(), equalTo(expectedIt.next()));
        }
    }
}

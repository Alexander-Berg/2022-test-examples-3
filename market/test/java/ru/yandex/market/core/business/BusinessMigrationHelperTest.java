package ru.yandex.market.core.business;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

@DbUnitDataSet(before = "BusinessMigrationHelperTest.before.csv")
public class BusinessMigrationHelperTest extends FunctionalTest {

    @Autowired
    private BusinessMigrationHelper businessMigrationHelper;


    private static Stream<Arguments> provideDataForTestGenerateNameManyToOne() {
        return Stream.of(
                //SUPPLIERS
                //Нет старого бизнеса. Есть домен
                Arguments.of(100L, MarketServiceType.SUPPLIER, "www.100.ru"),
                Arguments.of(200L, MarketServiceType.SUPPLIER, "www.200.ru"),
                //Есть старый бизнес
                Arguments.of(300L, MarketServiceType.SUPPLIER, "oldBusiness"),
                //SHOPS
                //Нет старого бизнеса. Есть домен
                Arguments.of(400L, MarketServiceType.SHOP, "www.400.ru"),
                Arguments.of(500L, MarketServiceType.SHOP, "www.500.ru"),
                //Есть старый бизнес
                Arguments.of(600L, MarketServiceType.SHOP, "oldBusiness")
        );
    }

    @ParameterizedTest
    @CsvSource({
            "ozon.ru,ozon",
            "f-f.ru,f-f",
            "msk.holodilnik.ru,holodilnik"
    })
    void testGetDomain2Level(String input, String expected) {
        Assertions.assertEquals(expected, businessMigrationHelper.getDomain2Level(input).get());
    }

    @ParameterizedTest
    @MethodSource("provideDataForTestGenerateNameManyToOne")
    void testGenerateNameManyToOne(long serviceId, MarketServiceType serviceType, String expectedName) {
        Assertions.assertEquals(expectedName,
                businessMigrationHelper.generateNameByBusinessOrDomain(serviceId, serviceType));
    }

    @Test
    void testGenerateNameWithoutName() {
        Assertions.assertEquals("Бизнес-аккаунт",
                businessMigrationHelper.generateNameByBusinessOrDomain(700, MarketServiceType.SHOP));
    }
}

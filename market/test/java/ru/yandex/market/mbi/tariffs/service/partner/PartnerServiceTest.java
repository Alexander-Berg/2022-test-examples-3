package ru.yandex.market.mbi.tariffs.service.partner;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.Constants;
import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.mbi.tariffs.model.Partner;
import ru.yandex.market.mbi.tariffs.model.PartnerType;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Тесты для {@link ru.yandex.market.mbi.tariffs.service.partner.PartnerService}
 */
@ParametersAreNonnullByDefault
class PartnerServiceTest extends FunctionalTest {

    @Autowired
    private PartnerService partnerService;

    @BeforeEach
    void setUp() {
        //todo workaround, пока не придумал, как можно сделать лучше
        ((CachedPartnerService) partnerService).invalidateCache();
    }

    @Test
    @DisplayName("Тест на получение всех партнеров")
    void testGetAll() {
        List<Partner> actual = partnerService.getAll();
        List<Partner> expected = List.of(
                Constants.Partners.VALID_PARTNER_SHOP,
                Constants.Partners.VALID_PARTNER_SUPPLIER,
                Constants.Partners.VALID_PARTNER_BUSINESS
        );

        assertThat(actual, containsInAnyOrder(expected.toArray(Partner[]::new)));
    }

    @Test
    @DisplayName("Тест на сохранение партнеров")
    @DbUnitDataSet(
            before = "save.before.csv",
            after = "save.after.csv"
    )
    void testSavePartners() {
        List<Partner> newPartners = List.of(
                new Partner().id(1L).type(PartnerType.SUPPLIER),     //дубликат
                new Partner().id(10L).type(PartnerType.SUPPLIER),
                new Partner().id(20L).type(PartnerType.BUSINESS),
                new Partner().id(30L).type(PartnerType.BUSINESS),
                new Partner().id(40L).type(PartnerType.SUPPLIER)
        );
        partnerService.savePartners(newPartners);
    }

    @ParameterizedTest
    @DisplayName("Тест на проверку партнеров по id и типу")
    @DbUnitDataSet(before = "partnerExists.before.csv")
    @MethodSource("partnerExists")
    void testPartnerExists(long partnerId, PartnerType type, boolean expected) {
        assertEquals(expected, partnerService.checkExists(partnerId, type));
    }

    private static Stream<Arguments> partnerExists() {
        return Stream.of(
                Arguments.of(100L, PartnerType.BUSINESS, true),
                Arguments.of(100L, PartnerType.SHOP,false),
                Arguments.of(200L, PartnerType.BUSINESS,false)
        );
    }
}

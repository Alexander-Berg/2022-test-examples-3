package ru.yandex.market.mbi.api.controller.abo.cutoff.list;

import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.abo.AboCutoff;
import ru.yandex.market.mbi.api.client.entity.abo.AboCutoffBulkResponse;
import ru.yandex.market.mbi.api.client.entity.abo.AboCutoffInfo;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;
import static ru.yandex.market.mbi.util.MbiAsserts.assertXmlEquals;

/**
 * Функциональные тесты на ручку {@link ru.yandex.market.mbi.api.controller.AboCutoffController#getCutoffs(long)}.
 *
 * @author fbokovikov
 */
class ShopCutoffsFunctionalTest extends FunctionalTest {

    private static final long SHOP_ID = 774L;

    /**
     * Проверяем, что для магазина без отключений не возвращается ничего лишнего
     */
    @Test
    @DbUnitDataSet
    void testShopWithoutCutoffs() {
        AboCutoffBulkResponse aboCutoffs = mbiApiClient.getAboCutoffs(SHOP_ID);
        assertTrue(aboCutoffs.getAboCutoffs().stream()
                .noneMatch(AboCutoffInfo::isActive));
    }

    /**
     * Проверяем, что:
     * <ul>
     * <li>возвращаются как CPA, так и CPC отключения</li>
     * <li>правильный маппинг {@link ru.yandex.market.core.cutoff.model.CutoffType} в
     * {@link ru.yandex.market.core.abo.AboCutoff}</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(before = "testShopWithExistentCutoffs.before.csv")
    void testShopWithExistentCutoffs() {
        Set<AboCutoff> aboCutoffs = mbiApiClient.getAboCutoffs(SHOP_ID).getAboCutoffs().stream()
                .filter(AboCutoffInfo::isActive)
                .map(AboCutoffInfo::getAboCutoff)
                .collect(Collectors.toSet());
        Set<AboCutoff> expectedAboCutoffs = ImmutableSet.of(
                AboCutoff.SHOP_FRAUD, AboCutoff.PINGER
        );
        assertEquals(expectedAboCutoffs, aboCutoffs);
    }

    /**
     * Проверяем, что для магазина без программы размещения всё равно возвращаются абокатофы,
     * которые не зависят от программы размещения
     */
    @Test
    @DbUnitDataSet(before = "testShopWithoutPlacementProgram.before.csv")
    void testShopWithoutPlacementProgram() {
        Set<AboCutoff> aboCutoffs = mbiApiClient.getAboCutoffs(SHOP_ID).getAboCutoffs().stream()
                .map(AboCutoffInfo::getAboCutoff)
                .collect(Collectors.toSet());

        assertEquals(aboCutoffs, AboCutoff.programPlacementIndifferentValues());
    }

    @Test
    @DisplayName("Проверка xml на выходе ручки")
    @DbUnitDataSet(before = "testShopWithExistentCutoffs.before.csv")
    void rawXmlSerializationTest() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                "http://localhost:" + port + "/shop-abo-cutoffs/{datasourceId}",
                SHOP_ID
        );
        assertXmlEquals(getString(this.getClass(), "data/response.xml"), response.getBody());
    }
}

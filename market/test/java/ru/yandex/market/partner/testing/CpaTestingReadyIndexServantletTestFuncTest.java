package ru.yandex.market.partner.testing;

import java.util.stream.Stream;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@DbUnitDataSet(before = "testCpaTestingIndex.before.csv")
class CpaTestingReadyIndexServantletTestFuncTest extends FunctionalTest {

    private static Stream<Arguments> suppliersIndex() {
        long userId = 10;
        return Stream.of(
                // Фид поставщика в продовом индексе
                Arguments.of(userId, 10100L, "<index><details/><type>MAIN</type></index>"),
                // Фид поставщика в продовом индексе и зафейленый фид
                Arguments.of(userId, 10110L, "<index><details reason=\"broken-feed\"/><type>NONE</type></index>"),
                // Фид поставщика в тестовом индексе
                Arguments.of(userId, 10101L, "<index><details/><type>SANDBOX</type></index>"),
                // Фид поставщика в тестовом индексе,но с поломанным фидом
                Arguments.of(userId, 10111L, "<index><details reason=\"broken-feed\"/><type>NONE</type></index>"),
                // Фид поставщика едет в тестовый индекс, фича в статусе NEW
                Arguments.of(userId, 10102L, "<index><details reason=\"loading\"/><type>NONE</type></index>"),
                // Фид поставщика ни в тестовом, ни в продовом
                Arguments.of(userId, 10103L, "<index><details/><type>NONE</type></index>"),
                // Фид поставщика едет в продовый индекс, фича в статусе SUCCESS
                Arguments.of(userId, 10104L, "<index><details reason=\"loading\"/><type>NONE</type></index>")
        );
    }

    private static Stream<Arguments> dropshipBySellers() {
        long userId = 10;
        return Stream.of(
                // Фид в продовом индексе, но нет CPA офферов
                Arguments.of(userId, 10200L, "<index><details reason=\"no-cpa-offers\"/><type>NONE</type></index>"),
                // Фид в продовом индексе и зафейленый фид
                Arguments.of(userId, 10210L, "<index><details reason=\"broken-feed\"/><type>NONE</type></index>"),
                // Фид в тестовом индексе
                Arguments.of(userId, 10201L, "<index><details reason=\"no-cpa-offers\"/><type>NONE</type></index>"),
                // Фид в тестовом индексе,но с поломанным фидом
                Arguments.of(userId, 10211L, "<index><details reason=\"broken-feed\"/><type>NONE</type></index>"),
                // Фид едет в тестовый индекс, фича в статусе NEW
                Arguments.of(userId, 10202L, "<index><details reason=\"loading\"/><type>NONE</type></index>"),
                // Фид ни в тестовом, ни в продовом, фича в DONT_WANT, магазин сконфигуренный
                Arguments.of(userId, 10203L, "<index><details/><type>NONE</type></index>"),
                // Фид едет в продовый индекс
                Arguments.of(userId, 10204L, "<index><details reason=\"loading\"/><type>NONE</type></index>"),
                // Фид в продовом индексе
                Arguments.of(userId, 10212L, "<index><details/><type>MAIN</type></index>"),
                // Фид в тестовом индексе
                Arguments.of(userId, 10213L, "<index><details/><type>SANDBOX</type></index>"),
                // Фид в тестовом индексе, но с ошибками, поэтому идексатор не выбросил магаз из индекса
                Arguments.of(userId, 10214L, "<index><details reason=\"broken-feed\"/><type>NONE</type></index>"),
                // Магазин недонастроен (не подтвержден способ обработки заказов)
                Arguments.of(userId, 10215L, "<index><details reason=\"missed-params\"/><type>NONE</type></index>"),
                // Магазин недонастроен (нет доставки)
                Arguments.of(userId, 10216L, "<index><details reason=\"missed-params\"/><type>NONE</type></index>"),
                // Магазин отключен за качество, но загрузил индекс в ПШ через API_DEBUG
                Arguments.of(userId, 10217L, "<index><details/><type>SANDBOX</type></index>"),
                // Магазин отключен за качество, но загрузил индекс в ПШ через API_DEBUG, и ждет индекса
                Arguments.of(userId, 10218L, "<index><details reason=\"loading\"/><type>NONE</type></index>"),
                // Магазин отключен за качество
                Arguments.of(userId, 10219L, "<index><details/><type>NONE</type></index>"),
                // Магазин находится в индексе, но скрыт динамиком
                Arguments.of(userId, 10220L, "<index><details/><type>MAIN</type></index>"),
                // Магазин выброшен из индекса, так как давно вырублен динамиком
                Arguments.of(userId, 10221L, "<index><details/><type>NONE</type></index>"),
                // Многофидовый магазин в проде
                Arguments.of(userId, 10222L, "<index><details/><type>MAIN</type></index>")
        );
    }

    /**
     * Фид поставщика в тестовом индексе.
     */
    @ParameterizedTest
    @MethodSource({"dropshipBySellers", "suppliersIndex"})
    void testSupplierCpaTestingIndex(long userId, long campaignId, String expected) throws Exception {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(baseUrl + "/cpaTestingReadyIndex?_user_id={userId}&id={campaignId}",
                        userId, campaignId);

        String body = response.getBody();
        XMLAssert.assertXpathsEqual("/node()", expected, "/data/index", body);
    }

}

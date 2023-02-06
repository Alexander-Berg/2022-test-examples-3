package ru.yandex.market.wrap.infor.functional;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FulfillmentInteraction;
import ru.yandex.market.fulfillment.wrap.core.scenario.builder.FunctionalTestScenarioBuilder;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetInboundDetailsResponse;
import ru.yandex.market.wrap.infor.service.inbound.InboundDetailsService;

/**
 * Функциональные тесты для {@link InboundDetailsService}.
 *
 * @author avetokhin 12.10.18.
 */
@DatabaseSetup(
    connection = "wrapConnection",
    value = "classpath:fixtures/functional/get_inbound_details/common/mapping_state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
@DatabaseSetup(
    connection = "wmsConnection",
    value = "classpath:fixtures/functional/get_inbound_details/common/receiptdetail_state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
@DatabaseSetup(
    connection = "wmsConnection",
    value = "classpath:fixtures/functional/get_inbound_details/common/loc_state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
@DatabaseSetup(
    connection = "wmsConnection",
    value = "classpath:fixtures/functional/get_inbound_details/common/billofmaterial_state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
@DatabaseSetup(
    connection = "wmsConnection",
    value = "classpath:fixtures/functional/get_inbound_details/common/sku_state.xml",
    type = DatabaseOperation.CLEAN_INSERT
)
class GetInboundDetailsTest extends AbstractFunctionalTest {

    private static final String RECEIPT_ID = "123";

    /**
     * Сценарий #1:
     * <p>
     * Запрашиваются детали существующей оприходованной поставки.
     */
    @Test
    void getInboundDetailsPositive() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/1/wrap_request.xml",
            "fixtures/functional/get_inbound_details/common/wrap_response.xml"
        );
    }

    /**
     * Сценарий #2:
     * <p>
     * Запрашиваются детали не существующей поставки.
     */
    @Test
    void getInboundDetailsNotFound() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/2/wrap_request.xml",
            "fixtures/functional/get_inbound_details/2/wrap_response.xml"
        );
    }

    /**
     * Сценарий #3:
     * <p>
     * Запрашиваются детали существующей не оприходованной поставки.
     */
    @Test
    void getInboundDetailsNotAccepted() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/3/wrap_request.xml",
            "fixtures/functional/get_inbound_details/3/wrap_response.xml"
        );
    }

    /**
     * Сценарий #4:
     * <p>
     * Запрашиваются детали поставки, но некорректно указаны входные данные.
     */
    @Test
    void getInboundDetailsInvalidInputData() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/4/wrap_request.xml",
            "fixtures/functional/get_inbound_details/4/wrap_response.xml"
        );

    }

    /**
     * Сценарий #5:
     * <p>
     * Запрашиваются детали существующей оприходованной поставки, содержащая паллеты.
     * <p>Проверяем, что элементы с SKU=PL будут отфильтрованы</p>
     */
    @Test
    void getInboundDetailsPositiveWithPallets() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/5/wrap_request.xml",
            "fixtures/functional/get_inbound_details/5/wrap_response.xml"
        );
    }

    /**
     * Сценарий #6:
     * <p>
     * Запрашиваем детали существующей оприходованной поставки,
     * в деталях которой присутствуют строки с неизвестными значениями категорий.
     * <p>
     * В ответ ожидаем получить ошибку обработки.
     */
    @Test
    void getInboundDetailsWithUnrecognizedCategory() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/6/wrap_request.xml",
            "fixtures/functional/get_inbound_details/6/wrap_response.xml"
        );
    }

    /**
     * Сценарий #7:
     * <p>
     * Запрашиваем детали существующей оприходованной поставки,
     * в деталях которой присутствуют строки с излишками.
     * <p>
     */
    @Test
    void getInboundDetailsWithSurplus() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/7/wrap_request.xml",
            "fixtures/functional/get_inbound_details/7/wrap_response.xml"
        );
    }

    /**
     * Сценарий #8:
     * <p>
     * Запрашиваем детали существующей оприходованной поставки,
     * в деталях которой есть 1 многоместный товар (SKU15) в количестве 6 штук
     * и один обычный (SKU16) в количестве 1 штуки.
     * <p>
     * <p>
     * Метод должен отработать без ошибок и вернуть корректные числа.
     */
    @Test
    void getInboundDetailsWithCompoundItem() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/8/wrap_request.xml",
            "fixtures/functional/get_inbound_details/8/wrap_response.xml"
        );
    }

    /**
     * Сценарий #9:
     * <p>
     * Запрашиваем детали существующей оприходованной поставки c несколькими многоместными товарами и одним одноместным:
     * <p>
     * <br>
     * SKU17 - имеет две части (BOM0..3, BOM0..4), всего заявлено 6 мастеров,
     * <p>
     * 3 штуки BOM0..3 повреждено, остальные приняты;
     * <p>
     * выходит годных комплектов: 3, поврежденных: 3
     * <p><br>
     * SKU18 - одноместный, 1 заявлен, 1 принят.
     * <p><br>
     * SKU19 - имеет две части (BOM0..5, BOM0..6), всего заявлено 6 мастеров,
     * <p>
     * 3 штуки BOM0..5 повреждено, 2 принято, BOM0..6 - все шесть приняты;
     * <p>
     * выходит годных комплектов: 2, поврежденных: 4
     * <p><br>
     * SKU20 - имеет две части (BOM0..7, BOM0..8), - проверяем потерянные:
     * <p>
     * BOM0..7 - принято 1 штука, BOM0..8 - 2,
     * <p>
     * выходит годных комплектов: 1, поврежденных: 1
     * <p><br>
     * SKU21 - имеет три части (BOM0..9, BOM0..10, BOM0..11):
     * <p>
     * BOM0..9 - принято 2 штуки, повреждено 1 штука, BOM0..10 - принято 2 штуки, BOM0..11 - принято 2,
     * повреждено 8
     * <p>
     * выходит годных комплектов: 2, поврежденных: 8 (по BOM0..11)<p>
     * <p>
     * <p>
     * Метод должен отработать без ошибок и вернуть корректные числа.
     */
    @Test
    void getInboundDetailsDefectMultipartAndOrdinaryCombined() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/9/wrap_request.xml",
            "fixtures/functional/get_inbound_details/9/wrap_response.xml"
        );
    }

    /**
     * Сценарий #10 Проверяем работу падения, если toloc у какой-то из частей неизвестен в коде.
     * <br>
     * Ожидается, что метод завершится с ошибкой с корректным числом частей с неизвестным toloc
     */
    @Test
    void getInboundDetailsMultipartWithUnknownToloc() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/10/wrap_request.xml",
            "fixtures/functional/get_inbound_details/10/wrap_response.xml"
        );
    }

    @Test
    void getInboundDetailsWithSurplusAndMultipart() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/11/wrap_request.xml",
            "fixtures/functional/get_inbound_details/11/wrap_response.xml"
        );
    }

    /**
     * Сценарий #12 Проверят, что корректно обработается случай, если будут несколько деталей для одного sku,
     * но с разными lottable08, и все они будут означать, что это НЕ излишек.
     */
    @Test
    void getInboundDetailsWithCuriousGrouping() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/12/wrap_request.xml",
            "fixtures/functional/get_inbound_details/12/wrap_response.xml"
        );
    }

    /**
     * Сценарий #13 Проверяет, что если в системе заведен неполный комплект, то будем считать все damage.
     * При этом damage не является излишком.
     */
    @Test
    void getInboundDetailsWithWrongExpectedKitsSize() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/13/wrap_request.xml",
            "fixtures/functional/get_inbound_details/13/wrap_response.xml"
        );
    }

    /**
     * Сценарий #14 Проверяет, что для товара с различными заказами,
     * разбивающими группировку рассчет произойдет корректно.
     */
    @Test
    void getInboundDetailsForNonSurplusWithDifferentOrders() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/14/wrap_request.xml",
            "fixtures/functional/get_inbound_details/14/wrap_response.xml"
        );
    }

    /**
     * Сценарий #15 Проверяет корректность подсчета излишков в случае surplus
     */
    @Test
    void getInboundDetailsForSurplusWithDefect() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/15/wrap_request.xml",
            "fixtures/functional/get_inbound_details/15/wrap_response.xml"
        );
    }

    /**
     * Сценарий #16 Проверяет корректность подсчета излишков в случае когда принято дефекта больше, чем планировалось,
     * но lottable08 не проставлен. Получаем damage товар в количестве большем, чем заявляли и 0 излишков.
     */
    @Test
    void getInboundDetailsForDefectOnly() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/16/wrap_request.xml",
            "fixtures/functional/get_inbound_details/16/wrap_response.xml"
        );
    }

    /**
     * Сценарий #17 Проверяет что строки в которых кол-во ожидаемого и принятого == 0 не попадают в детали.
     */
    @Test
    void getInboundDetailsForNonZeroLines() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/17/wrap_request.xml",
            "fixtures/functional/get_inbound_details/17/wrap_response.xml"
        );
    }

    /**
     * Сценарий #18 Проверяет детали с TolocType 'DMG-ABO' рассматриваются
     * дефектными равно как и 'DAMAGE'.
     */
    @Test
    void getInboundDetailsWithAboDamageTolocType() throws Exception {
        executeScenario(
            "fixtures/functional/get_inbound_details/18/wrap_request.xml",
            "fixtures/functional/get_inbound_details/18/wrap_response.xml"
        );
    }

    private void executeScenario(String wrapRequest,
                                 String wrapResponse,
                                 FulfillmentInteraction... interactions) throws Exception {
        FunctionalTestScenarioBuilder.start(GetInboundDetailsResponse.class)
            .sendRequestToWrapQueryGateway(wrapRequest)
            .thenMockFulfillmentRequests(interactions)
            .andExpectWrapAnswerToBeEqualTo(wrapResponse)
            .build(mockMvc, restTemplate, fulfillmentMapper, clientProperties.getUrl())
            .start();
    }
}

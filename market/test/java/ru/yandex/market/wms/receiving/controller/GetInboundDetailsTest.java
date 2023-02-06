package ru.yandex.market.wms.receiving.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.wms.common.spring.service.inbound.InboundDetailsService;
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils;
import ru.yandex.market.wms.common.spring.utils.XmlAssertUtils;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

/**
 * Функциональные тесты для {@link InboundDetailsService}.
 *
 * @author avetokhin 12.10.18.
 */

@DatabaseSetup("classpath:get-inbound-details/common/loc_state.xml")
@DatabaseSetup("classpath:get-inbound-details/common/sku_state.xml")
@DatabaseSetup("classpath:get-inbound-details/common/billofmaterial_state.xml")
@DatabaseSetup("classpath:get-inbound-details/common/receipt_state.xml")
@DatabaseSetup("classpath:get-inbound-details/common/receiptdetail_state.xml")
@DatabaseSetup("classpath:get-inbound-details/common/codelkup_state.xml")
public class GetInboundDetailsTest extends ReceivingIntegrationTest {

    /**
     * Сценарий #1:
     * <p>
     * Запрашиваются детали существующей оприходованной поставки.
     */
    @Test
    void getInboundDetailsPositive() throws Exception {
        executeXMLScenario(
            "get-inbound-details/1/wrap_request.xml",
            "get-inbound-details/1/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/1/request.json",
            "get-inbound-details/1/response.json",
            status().isOk()
        );
    }

    /**
     * Сценарий #2:
     * <p>
     * Запрашиваются детали не существующей поставки.
     */
    @Test
    void getInboundDetailsNotFound() throws Exception {
        executeXMLScenario(
            "get-inbound-details/2/wrap_request.xml",
            "get-inbound-details/2/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/2/request.json",
            "get-inbound-details/2/response.json",
            status().isInternalServerError()
        );
    }

    /**
     * Сценарий #3:
     * <p>
     * Запрашиваются детали существующей не оприходованной поставки.
     */
    @Test
    void getInboundDetailsNotAccepted() throws Exception {
        executeXMLScenario(
            "get-inbound-details/3/wrap_request.xml",
            "get-inbound-details/3/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/3/request.json",
            "get-inbound-details/3/response.json",
            status().isInternalServerError()
        );
    }

    /**
     * Сценарий #4:
     * <p>
     * Запрашиваются детали поставки, но некорректно указаны входные данные.
     */
    @Test
    void getInboundDetailsInvalidInputData() throws Exception {
        executeXMLScenario(
            "get-inbound-details/4/wrap_request.xml",
            "get-inbound-details/4/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/4/request.json",
            "get-inbound-details/4/response.json",
            status().isInternalServerError()
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
        executeXMLScenario(
            "get-inbound-details/5/wrap_request.xml",
            "get-inbound-details/5/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/5/request.json",
            "get-inbound-details/5/response.json",
            status().isOk()
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
        executeXMLScenario(
            "get-inbound-details/6/wrap_request.xml",
            "get-inbound-details/6/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/6/request.json",
            "get-inbound-details/6/response.json",
            status().isInternalServerError()
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
        executeXMLScenario(
            "get-inbound-details/7/wrap_request.xml",
            "get-inbound-details/7/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/7/request.json",
            "get-inbound-details/7/response.json",
            status().isOk()
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
        executeXMLScenario(
            "get-inbound-details/8/wrap_request.xml",
            "get-inbound-details/8/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/8/request.json",
            "get-inbound-details/8/response.json",
            status().isOk()
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
        executeXMLScenario(
            "get-inbound-details/9/wrap_request.xml",
            "get-inbound-details/9/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/9/request.json",
            "get-inbound-details/9/response.json",
            status().isOk()
        );
    }

    /**
     * Сценарий #10 Проверяем работу падения, если toloc у какой-то из частей неизвестен в коде.
     * <br>
     * Ожидается, что метод завершится с ошибкой с корректным числом частей с неизвестным toloc
     */
    @Test
    void getInboundDetailsMultipartWithUnknownToloc() throws Exception {
        executeXMLScenario(
            "get-inbound-details/10/wrap_request.xml",
            "get-inbound-details/10/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/10/request.json",
            "get-inbound-details/10/response.json",
            status().isInternalServerError()
        );
    }

    @Test
    void getInboundDetailsWithSurplusAndMultipart() throws Exception {
        executeXMLScenario(
            "get-inbound-details/11/wrap_request.xml",
            "get-inbound-details/11/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/11/request.json",
            "get-inbound-details/11/response.json",
            status().isOk()
        );
    }

    /**
     * Сценарий #12 Проверят, что корректно обработается случай, если будут несколько деталей для одного sku,
     * но с разными lottable08, и все они будут означать, что это НЕ излишек.
     */
    @Test
    void getInboundDetailsWithCuriousGrouping() throws Exception {
        executeXMLScenario(
            "get-inbound-details/12/wrap_request.xml",
            "get-inbound-details/12/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/12/request.json",
            "get-inbound-details/12/response.json",
            status().isOk()
        );
    }

    /**
     * Сценарий #13 Проверяет, что если в системе заведен неполный комплект, то будем считать все damage.
     * При этом damage не является излишком.
     */
    @Test
    void getInboundDetailsWithWrongExpectedKitsSize() throws Exception {
        executeXMLScenario(
            "get-inbound-details/13/wrap_request.xml",
            "get-inbound-details/13/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/13/request.json",
            "get-inbound-details/13/response.json",
            status().isOk()
        );
    }

    /**
     * Сценарий #14 Проверяет, что для товара с различными заказами,
     * разбивающими группировку рассчет произойдет корректно.
     */
    @Test
    void getInboundDetailsForNonSurplusWithDifferentOrders() throws Exception {
        executeXMLScenario(
            "get-inbound-details/14/wrap_request.xml",
            "get-inbound-details/14/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/14/request.json",
            "get-inbound-details/14/response.json",
            status().isOk()
        );
    }

    /**
     * Сценарий #15 Проверяет корректность подсчета излишков в случае surplus
     */
    @Test
    void getInboundDetailsForSurplusWithDefect() throws Exception {
        executeXMLScenario(
            "get-inbound-details/15/wrap_request.xml",
            "get-inbound-details/15/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/15/request.json",
            "get-inbound-details/15/response.json",
            status().isOk()
        );
    }

    /**
     * Сценарий #16 Проверяет корректность подсчета излишков в случае когда принято дефекта больше, чем планировалось,
     * но lottable08 не проставлен. Получаем damage товар в количестве большем, чем заявляли и 0 излишков.
     */
    @Test
    void getInboundDetailsForDefectOnly() throws Exception {
        executeXMLScenario(
            "get-inbound-details/16/wrap_request.xml",
            "get-inbound-details/16/wrap_response.xml"
        );
        executeJSONScenario(
                "get-inbound-details/16/request.json",
                "get-inbound-details/16/response.json",
                status().isOk()
        );
    }

    /**
     * Сценарий #17 Проверяет что строки в которых кол-во ожидаемого и принятого == 0 не попадают в детали.
     */
    @Test
    void getInboundDetailsForNonZeroLines() throws Exception {
        executeXMLScenario(
            "get-inbound-details/17/wrap_request.xml",
            "get-inbound-details/17/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/17/request.json",
            "get-inbound-details/17/response.json",
            status().isOk()
        );
    }

    /**
     * Сценарий #18 Проверяет детали с TolocType 'DMG-ABO' рассматриваются
     * дефектными равно как и 'DAMAGE'.
     */
    @Test
    void getInboundDetailsWithAboDamageTolocType() throws Exception {
        executeXMLScenario(
            "get-inbound-details/18/wrap_request.xml",
            "get-inbound-details/18/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/18/request.json",
            "get-inbound-details/18/response.json",
            status().isOk()
        );
    }


    /**
     * Сценарий #19:
     * <p>
     * Запрашиваем детали существующей оприходованной поставки c одним многоместным товаром
     * У которого принят комплектный бракованный товар, есть два некомплекта и комплектный годный товар:
     * <p>
     * <br>
     * SKU24 - имеет две части (BOM0..16, BOM0..17), всего заявлено 10 мастеров,
     * <p>
     * 1 штука BOM0..16 повреждена, 3 принято;
     * <p>
     * <p>
     * 1 штука BOM0..17 повреждена, 1 принята;
     * <p>
     * выходит годных комплектов: 1, поврежденных: 3
     * <p>
     * Метод должен отработать без ошибок и вернуть корректные числа.
     */
    @Test
    void getInboundDetailsDefectMultipartAndIncomplete() throws Exception {
        executeXMLScenario(
            "get-inbound-details/19/wrap_request.xml",
            "get-inbound-details/19/wrap_response.xml"
        );
        executeJSONScenario(
            "get-inbound-details/19/request.json",
            "get-inbound-details/19/response.json",
            status().isOk()
        );
    }

    private void executeXMLScenario(String requestFilePath, String responseFilePath) throws Exception {
        executeScenario(
                requestFilePath,
                responseFilePath,
                status().isOk(),
                MediaType.TEXT_XML
        );
    }

    private void executeJSONScenario(String requestFilePath, String responseFilePath,
                                     ResultMatcher responseStatus) throws Exception {
        executeScenario(
                requestFilePath,
                responseFilePath,
                responseStatus,
                MediaType.APPLICATION_JSON
        );
    }

    private void executeScenario(String requestFilePath, String responseFilePath, ResultMatcher responseStatus,
                                 MediaType mediaType) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/ENTERPRISE/inbound/get-inbound-details")
                .contentType(mediaType)
                .content(getFileContent(requestFilePath)))
                .andDo(print())
                .andExpect(responseStatus)
                .andReturn();

        final String contentAsString = mvcResult.getResponse().getContentAsString();
        if (mediaType == MediaType.TEXT_XML) {
            XmlAssertUtils.assertXmlValuesAreEqual(contentAsString,
                    getFileContent(responseFilePath));
        } else {
            JsonAssertUtils.assertFileNonExtensibleEquals(responseFilePath, contentAsString);
        }
    }
}

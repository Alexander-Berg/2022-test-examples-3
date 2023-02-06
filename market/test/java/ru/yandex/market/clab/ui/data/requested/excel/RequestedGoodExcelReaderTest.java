package ru.yandex.market.clab.ui.data.requested.excel;

import org.apache.commons.compress.utils.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.clab.common.service.mapping.MbocService;
import ru.yandex.market.clab.common.service.requested.good.RequestedGoodService;
import ru.yandex.market.clab.common.test.RandomTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 26.06.2019
 */
@RunWith(Parameterized.class)
public class RequestedGoodExcelReaderTest {

    private static final int BERU_SUPPLIER_ID = 88140896;
    private static final int EXPECTED_SUCCESFUL_ROW_COUNT = 21;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private RequestedGoodService goodService;

    @Mock
    private MbocService mbocService;

    @SuppressWarnings("checkstyle:VisibilityModifierCheck")
    @Parameterized.Parameter
    public String extension;

    @Parameterized.Parameters(name = "extension={0}")
    public static List<String> extensions() {
        return Arrays.asList("xlsx", "xls");
    }

    @Test
    public void allSuccessful() {
        final byte[] fileBytes = read("/importExcel/sucessfulList." + extension);
        assert1POk(fileBytes);
    }

    @Test
    public void allSuccessfulDifferentColumn() {
        final byte[] fileBytes = read("/importExcel/sucessfulDifferentColumn." + extension);
        assert1POk(fileBytes);
    }

    private List<RequestedGoodExcelResult.FileRow> assertOk(byte[] fileBytes) {
        RequestedGoodExcelReader reader = new RequestedGoodExcelReader(BERU_SUPPLIER_ID, goodService, mbocService);

        RequestedGoodExcelResult result = reader.read(fileBytes);

        assertThat(result.getError()).isNull();
        List<RequestedGoodExcelResult.FileRow> rows = result.getRows();

        assertThat(rows).hasSize(EXPECTED_SUCCESFUL_ROW_COUNT);
        assertThat(rows).extracting(RequestedGoodExcelResult.FileRow::getError)
            .containsOnlyNulls();

        return rows;
    }

    private void assert1POk(byte[] fileBytes) {
        List<RequestedGoodExcelResult.FileRow> rows = assertOk(fileBytes);
        assertThat(rows).extracting(RequestedGoodExcelResult.FileRow::getSupplierId)
            .allSatisfy(e -> assertThat(e).isEqualTo(BERU_SUPPLIER_ID));
        assertThat(rows).extracting(RequestedGoodExcelResult.FileRow::getShopSkuId)
            .containsExactly(
                "000002.4600728000069",
                "000002.4600728000199",
                "000002.4600728000366",
                "000002.4600728000588",
                "000002.4600728002162",
                "000002.4600728002254",
                "000002.4600728002261",
                "000002.4600728002278",
                "000002.4600728002315",
                "000002.4600728002575",
                "000002.4600728003930",
                "000002.4600728003954",
                "000002.4600728003992",
                "000002.4600728004760",
                "000002.4600728004784",
                "000002.4600728006306",
                "000002.4600728006542",
                "000002.4600728006559",
                "000002.4600728006566",
                "000002.4600728008300",
                "000002.4600728008416"
            );
    }

    @Test
    public void thirdPartySuccessful() {
        final byte[] fileBytes = read("/importExcel/3pGoodsList." + extension);
        assert3pOk(fileBytes);
    }

    private void assert3pOk(byte[] fileBytes) {
        List<RequestedGoodExcelResult.FileRow> rows = assertOk(fileBytes);
        assertThat(rows).extracting(RequestedGoodExcelResult.FileRow::getSupplierId)
            .containsExactly(
                100500L,
                100500L,
                100500L,
                100500L,
                100500L,
                100500L,
                100500L,
                100500L,
                100500L,
                100500L,
                100600L,
                100600L,
                100600L,
                100600L,
                100600L,
                100600L,
                100600L,
                100600L,
                100600L,
                100600L,
                100600L
            );
        assertThat(rows).extracting(RequestedGoodExcelResult.FileRow::getShopSkuId)
            .containsExactly(
                "4600728000069",
                "4600728000199",
                "4600728000366",
                "4600728000588",
                "4600728002162",
                "4600728002254",
                "4600728002261",
                "4600728002278",
                "4600728002315",
                "4600728002575",
                "4600728003930",
                "4600728003954",
                "4600728003992",
                "4600728004760",
                "4600728004784",
                "4600728006306",
                "4600728006542",
                "4600728006559",
                "4600728006566",
                "4600728008300",
                "4600728008416"
            );
    }

    @Test
    public void noColumn() {
        final byte[] fileBytes = read("/importExcel/noColumnList." + extension);
        RequestedGoodExcelReader reader = new RequestedGoodExcelReader(BERU_SUPPLIER_ID, goodService, mbocService);

        RequestedGoodExcelResult result = reader.read(fileBytes);

        assertThat(result.getError()).isEqualTo("не найдена колонка SSKU");
    }

    @Test
    public void corruptedFile() {
        RequestedGoodExcelReader reader = new RequestedGoodExcelReader(BERU_SUPPLIER_ID, goodService, mbocService);

        RequestedGoodExcelResult result = reader.read(RandomTestUtils.randomBytes());

        assertThat(result.getError())
            .isEqualTo("ошибка чтения файла. Возможно файл битый или неподдерживаемого формата");
    }

    @Test
    public void emptyCells() {
        RequestedGoodExcelReader reader = new RequestedGoodExcelReader(BERU_SUPPLIER_ID, goodService, mbocService);

        RequestedGoodExcelResult result = reader.read(read("/importExcel/emptyCells." + extension));

        assertThat(result.getError()).isNull();
        assertThat(result.getRows()).extracting(RequestedGoodExcelResult.FileRow::getShopSkuId)
            .containsExactly("00097.5286133361048");
    }

    @Test
    public void duplicateCells() {
        RequestedGoodExcelReader reader = new RequestedGoodExcelReader(BERU_SUPPLIER_ID, goodService, mbocService);

        RequestedGoodExcelResult result = reader.read(read("/importExcel/duplicateList." + extension));

        assertThat(result.getError()).isNull();
        assertThat(result.getRows()).extracting(RequestedGoodExcelResult.FileRow::getError)
            .containsExactly(null, "Дубликат", null);
    }

    private static byte[] read(String name) {
        try (InputStream stream = RequestedGoodExcelReaderTest.class.getResourceAsStream(name)) {
            return IOUtils.toByteArray(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

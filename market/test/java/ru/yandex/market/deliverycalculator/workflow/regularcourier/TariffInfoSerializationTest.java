package ru.yandex.market.deliverycalculator.workflow.regularcourier;

import javax.xml.bind.JAXBContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.deliverycalculator.common.CurrencyUtils;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffStrategy;
import ru.yandex.market.deliverycalculator.workflow.util.XmlUtils;

/**
 * Тесты для проверки сериализации и десериализации {@link TariffInfo}.
 */
class TariffInfoSerializationTest {

    private static final JAXBContext JAXB_CONTEXT = RegularCourierTariffWorkflow.JAXB_CONTEXT;
    private static final String XML = "<tariff campaign-type=\"SHOP\">\n" +
            "   <matrix strategy=\"CATEGORY\" currency=\"RUR\">\n" +
            "      <default-category-index>0</default-category-index>\n" +
            "      <bucket-ids>1 2</bucket-ids>\n" +
            "   </matrix>\n" +
            "</tariff>";

    @Test
    void testSerialization() {
        final TariffInfo tariffInfo = createTariffInfo();
        final String xml = XmlUtils.serialize(JAXB_CONTEXT, tariffInfo);

        Assertions.assertEquals(XML, xml);
    }

    @Test
    void testDeserialization() {
        final TariffInfo data = XmlUtils.deserialize(JAXB_CONTEXT, XML);
        final MatrixInfo matrix = data.getMatrices()[0];

        Assertions.assertNull(matrix.getPriceBorders());
        Assertions.assertNull(matrix.getWeightBorders());
    }


    private TariffInfo createTariffInfo() {
        final MatrixInfo matrixInfo = createMatrixInfo();
        final TariffInfo tariffInfo = new TariffInfo();
        tariffInfo.setCampaignType("SHOP");
        tariffInfo.setMatrices(new MatrixInfo[]{matrixInfo});
        return tariffInfo;
    }

    private MatrixInfo createMatrixInfo() {
        final MatrixInfo info = new MatrixInfo();
        info.setBucketIds(new long[]{1, 2});
        info.setCurrency(CurrencyUtils.DEFAULT_CURRENCY);
        info.setStrategy(DeliveryTariffStrategy.CATEGORY);
        info.setDefaultCategoryIndex(0);
        return info;
    }

}

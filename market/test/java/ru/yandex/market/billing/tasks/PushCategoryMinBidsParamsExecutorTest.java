package ru.yandex.market.billing.tasks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ru.yandex.market.api.CategoryMinBidsParams;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;

import static org.junit.Assert.assertEquals;

/**
 * Тесты для {@link PushCategoryMinBidsParamsExecutor}
 */
public class PushCategoryMinBidsParamsExecutorTest {

    /**
     * Создание тестового набора данных.
     * У {@link CategoryMinBidsParams} может не быть одного из из наборов параметров для расчёт bid или cbid
     *
     * @return список тестовых данных
     */
    private List<CategoryMinBidsParams> createTestCategoryMinBidsParamsList() throws Exception {
        return Arrays.asList(
                new CategoryMinBidsParams(1, 0.123, 12.32, 10, 21.4356534, 32123.00009, 1),
                new CategoryMinBidsParams(2, null, null, null, 42.94394, 123.4567, 2),
                new CategoryMinBidsParams(3, 43.32, 654.321, 5, null, null, null)
        );
    }

    /**
     * Проверка корректности сериализации/десериализации в/из xml
     */
    @Test
    public void testGetCategoryMinBidsParams() throws Exception {
        List<CategoryMinBidsParams> categoryMinBidsParamsList = createTestCategoryMinBidsParamsList();

        File GetCategoryMinBidsParamsFile = TempFileUtils.createTempFile();

        PushCategoryMinBidsParamsExecutor.writeCategoryMinBidsParamsFile(
                new PushCategoryMinBidsParamsExecutor.XMLCategoriesMinBidsParamsList(categoryMinBidsParamsList),
                GetCategoryMinBidsParamsFile);

        List<CategoryMinBidsParams> result =
                getCategoryMinBidsParams(new ByteArrayInputStream(Files.readAllBytes(GetCategoryMinBidsParamsFile.toPath())));

        assertEquals("CategoryMinBidsParams lists should be equal", categoryMinBidsParamsList, result);
    }

    @Nonnull
    private List<CategoryMinBidsParams> getCategoryMinBidsParams(InputStream categoryMinBidsParamsInputStream) throws Exception {
        Document document = getDocument(categoryMinBidsParamsInputStream);

        List<CategoryMinBidsParams> result = new ArrayList<>();

        NodeList categoryNodes = document.getElementsByTagName("category");
        for (int i = 0; i < categoryNodes.getLength(); i++) {
            Node categoryNode = categoryNodes.item(i);
            NamedNodeMap attributes = categoryNode.getAttributes();
            int hyperId = Integer.parseInt(attributes.getNamedItem("id").getNodeValue());
            Double bidCoefficient = null;
            Double bidPower = null;
            Integer maxBid = null;
            Double cbidCoefficient = null;
            Double cbidPower = null;
            Integer maxCbid = null;
            NodeList childNodes = categoryNode.getChildNodes();
            for (int childIndex = 0; childIndex < childNodes.getLength(); childIndex++) {
                Node bidNode = childNodes.item(childIndex);

                NamedNodeMap bidAttributes = bidNode.getAttributes();
                double coefficient = Double.parseDouble(bidAttributes.getNamedItem("coefficient").getNodeValue());
                double power = Double.parseDouble(bidAttributes.getNamedItem("power").getNodeValue());
                int bid = Integer.parseInt(bidAttributes.getNamedItem("maxBid").getNodeValue());

                String nodeName = bidNode.getNodeName();
                if ("searchBidParams".equals(nodeName)) {
                    bidCoefficient = coefficient;
                    bidPower = power;
                    maxBid = bid;
                }
                if ("cardBidParams".equals(nodeName)) {
                    cbidCoefficient = coefficient;
                    cbidPower = power;
                    maxCbid = bid;
                }
            }

            result.add(new CategoryMinBidsParams(hyperId, bidCoefficient, bidPower, maxBid, cbidCoefficient, cbidPower, maxCbid));
        }
        return result;
    }

    private Document getDocument(InputStream categoryMinBidsParamsInputStream) throws Exception {
        StringWriter writer = new StringWriter();
        IOUtils.copy(categoryMinBidsParamsInputStream, writer, StandardCharsets.UTF_8);
        String xmlString = writer.toString();
        return loadXMLFromString(xmlString);
    }

    private Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

}

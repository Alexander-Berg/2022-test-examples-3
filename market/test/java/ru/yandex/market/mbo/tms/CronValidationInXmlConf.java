package ru.yandex.market.mbo.tms;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.quartz.CronExpression;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CronValidationInXmlConf {

    @Test
    public void cronTest() throws ParserConfigurationException, URISyntaxException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = factory.newDocumentBuilder();

        File dir = new File(this.getClass().getResource("/mbo-tms").toURI());
        final List<File> files = Arrays.asList(dir.listFiles());
        files.forEach(file -> assertTrue(file.exists()));
        files.forEach(file -> validate(documentBuilder, file));

    }

    private void validate(DocumentBuilder documentBuilder, File file) {
        String expression = "";
        try {
            final Document parse = documentBuilder.parse(file);
            XPathFactory xPathFactory = XPathFactory.newInstance();
            final XPath xPath = xPathFactory.newXPath();
            final XPathExpression compile = xPath.compile("/beans/bean/property[@name='cronExpression']/@value");
            final NodeList nodeList = (NodeList) compile.evaluate(parse, XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); i++) {
                expression = nodeList.item(i).getNodeValue();
                new CronExpression(expression); // validation in constructor
            }

        } catch (XPathExpressionException | IOException | SAXException e) {
            throw new RuntimeException("Error in test", e);
        } catch (ParseException e) {
            fail("Not valid expression: " + expression + " in file: " + file.getName() + ". Exception: " + e);
        }

    }
}

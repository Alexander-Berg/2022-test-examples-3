package ru.yandex.market.abo.core.spark;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.xml.transform.StringSource;

import ru.yandex.market.abo.core.spark.data.CheckCompany;
import ru.yandex.market.abo.core.spark.data.CompanyArbitrationSummary;
import ru.yandex.market.abo.core.spark.data.CompanyCoownersHistory;
import ru.yandex.market.abo.core.spark.data.CompanyList;
import ru.yandex.market.abo.core.spark.data.CompanyRiskFactors;
import ru.yandex.market.abo.core.spark.data.CompanySparkRisksReportXML;
import ru.yandex.market.abo.core.spark.data.CompanyStructure;
import ru.yandex.market.abo.core.spark.data.EntrepreneurArbitrationSummary;
import ru.yandex.market.abo.core.spark.data.EntrepreneurShortReport;
import ru.yandex.market.abo.core.spark.data.ExtendedReport;
import ru.yandex.market.abo.core.spark.data.PersonSparkRisksReportXML;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author kukabara
 */
public class SparkMappingTest {
    private JAXBContext jaxbContext;

    @BeforeEach
    public void setUp() throws Exception {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("ru.yandex.market.abo.core.spark.data");
        this.jaxbContext = marshaller.getJaxbContext();
    }

    @Test
    public void testCheckCompanyStatus() throws Exception {
        test(CheckCompany.class, "/spark/CheckCompanyStatus.xml");
        test(ExtendedReport.class, "/spark/GetCompanyExtendedReport.xml");
        test(CompanyArbitrationSummary.class, "/spark/GetCompanyArbitrationSummary.xml");
        test(CompanyStructure.class, "/spark/GetCompanyStructure.xml");
        test(CompanyList.class, "/spark/GetCompanyListByFIO.xml");
        test(CompanyCoownersHistory.class, "/spark/GetCompanyCoownersHistory.xml");
        test(CompanyRiskFactors.class, "/spark/GetCompanyRiskFactors.xml");
        test(EntrepreneurShortReport.class, "/spark/GetEntrepreneurShortReport.xml");
        test(EntrepreneurArbitrationSummary.class, "/spark/GetEntrepreneurArbitrationSummary.xml");
        test(CompanySparkRisksReportXML.class, "/spark/GetCompanySparkRisksReportXML.xml");
        test(PersonSparkRisksReportXML.class, "/spark/GetPersonSparkRisksReportXML.xml");
    }

    public <T> void test(Class<T> clazz, String file) throws Exception {
        T t = readAndConvert(clazz, file);
        assertNotNull(t);
        System.out.println(t);
    }

    private <T> T readAndConvert(Class<T> clazz, String fileName) throws Exception {
        InputStream inputStream = SparkMappingTest.class.getResourceAsStream(fileName);
        String xmlData = IOUtils.toString(inputStream, "UTF-8");

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (T) unmarshaller.unmarshal(new StringSource(xmlData), clazz).getValue();
    }
}

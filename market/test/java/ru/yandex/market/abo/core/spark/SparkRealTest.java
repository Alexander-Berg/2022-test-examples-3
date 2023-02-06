package ru.yandex.market.abo.core.spark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.core.spark.api.SparkApiDataLoader;
import ru.yandex.market.abo.core.spark.api.SparkClient;
import ru.yandex.market.abo.core.spark.api.SparkEntity;
import ru.yandex.market.abo.core.spark.api.SparkLockService;
import ru.yandex.market.abo.core.spark.data.CheckCompany;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Логирует ответы из СПАРК-а в папку {@link SparkClient#logResponsesDirectory}.
 *
 * @author kukabara
 */
@Disabled
public class SparkRealTest extends EmptyTest {
    private static final Logger log = Logger.getLogger(SparkRealTest.class);

    private static final String TEST_OGRN_OOO = "1027739244741";
    private static final String TEST_OGRN_IP = "309501014900026";
    @Value("${abo.spark.login}")
    private String login;
    @Value("${abo.spark.password}")
    private String password;
    @Autowired
    private HttpClient sparkHttpClient;
    @Autowired
    private Jaxb2Marshaller sparkMarshaller;
    @Autowired
    private SaajSoapMessageFactory messageFactory;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;
    @Autowired
    private SparkLockService sparkLockService;
    @Autowired
    private ConfigurationService coreConfigService;

    private SparkApiDataLoader sparkManager;
    private SparkClient sparkClient;

    @BeforeEach
    public void setUp() throws Exception {
        sparkClient = new SparkClient(sparkHttpClient, login, password, sparkLockService);
        sparkClient.setMessageFactory(messageFactory);

        sparkClient.setMarshaller(sparkMarshaller);
        sparkClient.setUnmarshaller(sparkMarshaller);
        sparkClient.setJaxbContext(sparkMarshaller.getJaxbContext());
        sparkClient.setLogResponsesDirectory("spark_responses");

        sparkManager = new SparkApiDataLoader(sparkClient, coreConfigService);
    }

    @Test
    @Disabled
    public void testCheckCompanyStatus() throws Exception {
        CheckCompany companyStatus = sparkClient.checkCompanyOrEntrepreneurStatus(TEST_OGRN_OOO);
        assertNotNull(companyStatus);

        companyStatus = sparkClient.checkCompanyOrEntrepreneurStatus(TEST_OGRN_IP);
        assertNotNull(companyStatus);
    }

    @Test
    @Disabled
    public void testGetCompany() throws Exception {
        sparkManager.loadFromSparkCompany(TEST_OGRN_OOO);
    }

    @Test
    @Disabled
    void getCompanies() throws IOException {
        String ogrnsFile = "your_ogrns_file";
        List<String> wrongOgrns = new ArrayList<>();
        Files.lines(Paths.get(ogrnsFile)).filter(StringUtils::isNotBlank)
                .forEach(ogrn -> {
                    switch (ogrn.length()) {
                        case Spark.OGRN_LENGTH:
                            sparkClient.getCompanyExtendedReport(new SparkEntity(SparkEntity.Type.OGRN, ogrn));
                            break;
                        case Spark.OGRN_IP_LENGTH:
                            sparkClient.getEntrepreneurShortReport(new SparkEntity(SparkEntity.Type.OGRN, ogrn));
                            break;
                        default:
                            log.warn("Wrong ogrn " + ogrn);
                            wrongOgrns.add(ogrn);
                    }
                });

        if (!wrongOgrns.isEmpty()) {
            Files.write(Paths.get("wrong-ogrns.csv"), wrongOgrns);
        }
    }

    @Test
    @Disabled
    public void testGetIp() throws Exception {
        sparkManager.loadFromSparkIp(TEST_OGRN_IP);
    }

    /**
     * По всем активным* магазинам разово загружаем информацию из СПАРК-а.
     */
    @Disabled
    @Test
    public void test() throws Exception {
        List<String> ogrnList = pgJdbcTemplate.queryForList("SELECT distinct i.ogrn\n" +
                        "FROM  ext_organization_info i\n" +
                        "  JOIN shop s ON s.id = i.datasource_id\n" +
                        "  JOIN ext_shop_region r ON s.id = r.datasource_id\n" +
                        "  LEFT JOIN shop_exception e ON s.id = e.shop_id AND NOT e.deleted AND e.type = 15\n" +
                        "  LEFT JOIN spark_shop_data sd ON sd.ogrn = i.ogrn OR sd.prev_ogrn_array @> ARRAY[i.ogrn]::text[]\n" +
                        "WHERE NOT s.is_offline\n" +
                        "      AND NOT s.is_global\n" +
                        "      AND country_id = " + Regions.RUSSIA +
                        "      AND e.shop_id IS NULL\n" +
                        "      AND s.age > 0\n" +
                        "      AND (s.cpc = 'ON' or s.cpa = 'ON')\n" +
                        "      and (length(i.ogrn) = 13 or length(i.ogrn) = 15)\n" +
                        "      and (sd.ogrn is null or not sd.is_full_info)",
                String.class);
        log.info("Load " + ogrnList.size() + " for checking OGRN in SPARK");

        List<String> wrongOgrn = new ArrayList<>();
        for (String ogrn : ogrnList) {
            try {
                sparkManager.loadFromSparkCompany(ogrn);
            } catch (Throwable e) {
                log.warn("Wrong ogrn " + ogrn, e);
                wrongOgrn.add(ogrn);
            }
        }
        log.warn(wrongOgrn);
    }
}

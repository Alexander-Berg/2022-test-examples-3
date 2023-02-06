package ru.yandex.market.tsum.sox.validation;

import com.google.common.base.Preconditions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.sox.config.TsumSoxValidationConfig;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Issue;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 09/11/2018
 * <p>
 * To run this tests temporary add your startrek token to /tsum-sox/src/test/resources/tsum-sox-test.properties
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TsumSoxValidationConfig.class, SoxValidationIntegrationTest.Config.class})
public class SoxValidationIntegrationTest {
    @Autowired
    private Issues startrekIssues;

    @Autowired
    private SoxValidationService validationService;

    @Before
    public void setup() {

    }

    @Test
    public void indexerTest() {
        Issue issue = startrekIssues.get("MARKETINDEXER-18984");
        Preconditions.checkNotNull(issue);

        SoxValidationResult result = validationService.validate("yandex-market-indexer", issue);

        Assert.assertTrue(result.isValid());
    }

    @Test
    public void robotIndexerTest() {
        Issue issue = startrekIssues.get("MARKETINDEXER-18773");
        Preconditions.checkNotNull(issue);

        SoxValidationResult result = validationService.validate("yandex-market-offers-robot2", issue);

        Assert.assertTrue(result.isValid());
    }

    @Configuration
    @PropertySource({
        "classpath:00_tsum-sox.properties", "05_tsum-credentials.properties", "classpath:tsum-sox-test.properties"
    })
    protected static class Config {

    }
}

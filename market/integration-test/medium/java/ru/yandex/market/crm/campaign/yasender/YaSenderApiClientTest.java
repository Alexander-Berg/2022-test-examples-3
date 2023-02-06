package ru.yandex.market.crm.campaign.yasender;

import java.util.Collections;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.crm.campaign.TestExternalServicesConfig;
import ru.yandex.market.crm.campaign.placeholders.AppPropertiesConfiguration;
import ru.yandex.market.crm.core.services.external.yandexsender.YaSenderApiClient;
import ru.yandex.market.crm.core.services.external.yandexsender.YaSenderResponse;
import ru.yandex.market.crm.core.services.jackson.JacksonConfig;
import ru.yandex.market.crm.core.test.TestEnvironmentResolver;
import ru.yandex.market.mcrm.http.HttpClientConfiguration;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = {
        AppPropertiesConfiguration.class,
        TestEnvironmentResolver.class,
        HttpClientConfiguration.class,
        TestExternalServicesConfig.class,
        JacksonConfig.class
})
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class YaSenderApiClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(YaSenderApiClientTest.class);

    @Inject
    private YaSenderApiClient client;

    @Test
    @Ignore
    public void check() {
        YaSenderResponse resp = client.sendTransactional("OCBUMVS2-XUP1", "wanderer25@yandex-team.ru",
                ImmutableMap.of("subject", "Привет я!!!",
                        "name", "Имя пользователя",
                        "html",
                        """
                                <html>
                                <head>
                                    <title></title>
                                    <meta content="text/html; charset=utf-8" http-equiv="Content-Type"/>
                                </head>
                                <body>
                                    <div style="font-family:arial,sans-serif;font-style:normal;font-variant-caps:normal;font-variant-ligatures:normal;font-weight:normal;line-height:22.5px;text-align:start;text-transform:none;white-space:normal;">
                                        <p>Dear {{ name|default('ААААА') }}!
                                        <p>Not interested? <a href="https://market.yandex.ru">Unusbscribe</a>
                                    </div>
                                </body>
                                </html>"""
                ),
                false,
                Collections.emptyList()
        );
        Assert.assertEquals(YaSenderResponse.Status.OK, resp.getResult().getStatus());
    }
}

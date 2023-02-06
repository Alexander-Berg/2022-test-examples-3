package ru.yandex.manual;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.apache.commons.dbcp2.BasicDataSource;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bannerstorage.harvester.configs.queues.RtbIntegrationConfig;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.creativechanged.CreativeChangedMessage;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.creativechanged.CreativeChangedQueueObserver;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.creativechanged.CreativeChangedRequest;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.creativechanged.services.impl.JdbcCreativeService;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.infrastructure.RtbIntegrationHealthService;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.infrastructure.impl.BsApiClientService;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.infrastructure.impl.HttpRtbClientService;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.postmoderation.models.UnmoderatedAssembly;
import ru.yandex.bannerstorage.harvester.utils.RestTemplateFactory;
import ru.yandex.direct.bs.dspcreative.service.DspCreativeYtExporter;

/**
 * Ручная отправка в РТБХост
 */
@Ignore("Для ручного запуска")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {RtbIntegrationConfig.class})
public class SendToRTBHostTest {
    // ssh -L 8098:bssoap.yandex.ru:80 bannerstorage-harvester01man.haze.yandex.net

    @Autowired
    private DspCreativeYtExporter dspCreativeYtExporter;

    @Test
    public void test() {
        HttpRtbClientService httpRtbClientService = new HttpRtbClientService(
                new RestTemplateFactory(100, 100, false),
                "http://bssoap.yandex.ru:8098/export",
                10000,
                10000
        );
        List<UnmoderatedAssembly> unmoderatedAssemblies =
                httpRtbClientService.getUnmoderatedAssemblies(287687, 1);
        System.out.println("");
    }

    // socat tcp-l:5050,fork,reuseaddr tcp6:bssoap.yandex.ru:80
    // ssh -L 8098:localhost:5050 root@harvester.ivhfycqbkutzolyw.iva.yp-c.yandex.net
    @Test
    public void test2() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("net.sourceforge.jtds.jdbc.Driver");
        dataSource.setUrl("jdbc:jtds:sqlserver://rapidc.yandex.ru/BannerStorage");
        //dataSource.setUrl("jdbc:jtds:sqlserver://display-win-sas-c32m32d300-02.sas.yp-c.yandex.net:2433/BannerStorage");
        dataSource.setUsername("changeit");
        dataSource.setPassword("changeit");
        dataSource.setValidationQuery("select 1");
        dataSource.setMaxTotal(10);
        dataSource.setMaxIdle(3);
        dataSource.setRemoveAbandonedTimeout(300);

        CreativeChangedQueueObserver observer = new CreativeChangedQueueObserver(
                new JdbcCreativeService(
                        new JdbcTemplate(dataSource)
                ),
                new HttpRtbClientService(
                        new RestTemplateFactory(100, 100, false),
                        "http://bssoap.yandex.ru:8098/export",
                        //"http://rtbhost-stub.bqnkvikkocivxpu2.myt.yp-c.yandex.net/export",
                        //"http://bssoap-pre01i.yabs.yandex.ru:8098/export",
                        10000,
                        10000
                ),
                new RtbIntegrationHealthService() {
                    @Override
                    public void notifyAlive(@NotNull RtbIntegrationHealthService.@NotNull Queue queue) {
                        // Do nothing
                    }
                },
                new BsApiClientService(
                        new RestTemplateFactory(100, 100, false),
                        "https://rest-api.bannerstorage.yandex.net"
                ),
                dspCreativeYtExporter
        );
        ImmutableList<Integer> creativeVersions = ImmutableList.of(
                7816563
        );
        for (Integer creativeVersion : creativeVersions) {
            CreativeChangedMessage creativeChangedMessage = new CreativeChangedMessage();
            creativeChangedMessage.setCreativeVersionId(creativeVersion);
            creativeChangedMessage.setOrderNo(1);
            CreativeChangedRequest creativeChangedRequest = new CreativeChangedRequest();
            creativeChangedRequest.setCreativeVersions(Collections.singletonList(creativeChangedMessage));
            observer.doProcessMessage(creativeChangedRequest);
        }
    }
}

package ru.yandex.market.crm.operatorwindow.services.metainfo;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.metadata.conf.metaclass.MetaclassConf;
import ru.yandex.market.jmf.metadata.impl.MetadataInitializer;
import ru.yandex.market.jmf.metainfo.MetaInfoStorageService;
import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.module.ou.security.ModuleOuSecurityConfiguration;
import ru.yandex.market.jmf.timings.test.TimingTestConfiguration;
import ru.yandex.market.jmf.tx.TxService;

/**
 * Этот тест возник в ходе разбора тикета OCRM-5929,
 * при попытке обновить метаинформацию из разных мест возникала ошибка вида
 * {@code Not an entity: class jmf.pool.db.v1.Entity$$employeeRole}
 */
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = MetainfoReloadTest.TestConfiguration.class)
public class MetainfoReloadTest {

    @Inject
    TxService txService;
    @Inject
    MetaInfoStorageService metaInfoStorageService;

    // По непонятной причине падает только при inject-е этого сервиса
    @Inject
    BcpService bcpService;

    @Test
    public void multipleReload() {
        txService.runInTx(() ->
                metaInfoStorageService.saveWithoutDeduplication(MetaclassConf.TYPE, "any1", null,
                        MetadataInitializer.KEY));

        txService.runInTx(() ->
                metaInfoStorageService.saveWithoutDeduplication(MetaclassConf.TYPE, "any2", null,
                        MetadataInitializer.KEY));
    }

    @Import({
            ModuleDefaultTestConfiguration.class,
            ModuleOuSecurityConfiguration.class,
            TimingTestConfiguration.class,
    })
    static class TestConfiguration {
    }
}

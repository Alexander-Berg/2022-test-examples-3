package ru.yandex.market.mbo.configs.initializers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import ru.yandex.market.application.properties.etcd.EtcdClient;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Initializer, который загружает tnsnames.ora из хранилища etcd для запуска тестов.
 *
 * @author s-ermakov
 */
public class EtcdTnsnamesPostProcessor implements BeanFactoryPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(EtcdTnsnamesPostProcessor.class);

    private static final String ORACLE_NET_TNS_ADMIN_PROPERTY = "oracle.net.tns_admin";
    private static final String TNSNAMES_ORA = "tnsnames.ora";

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Path folder = createTnsAdminFolder();
        log.debug("Get '{}' as tnsAdminFolder", folder);

        String content = getTnsNamesContent(beanFactory);
        log.debug("Load tnsnames.ora");

        File file = folder.resolve(TNSNAMES_ORA).toFile();
        writeFile(file, content);
        log.debug("Created file in {}", file);
    }

    @Nonnull
    private String getTnsNamesContent(ConfigurableListableBeanFactory beanFactory) {
        EtcdClient etcdClient = beanFactory.getBean(EtcdClient.class);
        Map<String, String> properties = etcdClient.getProperties("/datasources/development/oracle/tnsnames.ora");
        String content = properties.get("content");
        if (content == null) {
            throw new IllegalStateException("No tnsnames.ora content in etcd");
        }
        return content;
    }

    @Nonnull
    private Path createTnsAdminFolder() {
        try {
            Path tempDirectory = Files.createTempDirectory(null);
            System.setProperty(ORACLE_NET_TNS_ADMIN_PROPERTY, tempDirectory.toString());
            log.debug("Set '{}' to '{}'", ORACLE_NET_TNS_ADMIN_PROPERTY, tempDirectory);

            return tempDirectory;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeFile(File file, String content) {
        try (PrintWriter out = new PrintWriter(file)) {
            out.print(content);
        } catch (FileNotFoundException e) {
            throw new RuntimeException();
        }
    }
}

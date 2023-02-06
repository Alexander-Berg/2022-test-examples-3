package ru.yandex.market.application.properties.etcd;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import ru.yandex.market.application.properties.AppPropertyScanner;
import ru.yandex.market.application.properties.etcd.resources.VirtualFileEtcdResource;
import ru.yandex.market.application.properties.utils.Environments;

import java.util.Arrays;
import java.util.List;

/**
 * @author s-ermakov
 */
public class VirtualFileEtcdAppPropertyScannerTest {
    private FileSystemResource applicationResource = new FileSystemResource(
        "mbo-category/mboc-app/src/main/properties.d/00-application.properties");
    private FileSystemResource securityResource = new FileSystemResource(
        "mbo-category/mboc-app/src/main/properties.d/02-security.properties");
    private FileSystemResource localEnvResource = new FileSystemResource(
        "mbo-category/mboc-app/src/main/properties.d/local/00-environment.properties");
    private FileSystemResource localSecurityResource = new FileSystemResource(
        "mbo-category/mboc-app/src/main/properties.d/local/02-security.properties");

    @Test
    public void testCorrectOrdering() {
        EtcdClientMock etcdClientMock = new EtcdClientMock();
        AppPropertyScanner propertyScannerMock = Mockito.mock(AppPropertyScanner.class);
        Mockito.when(propertyScannerMock.getResources()).then(__ -> Arrays.asList(
            applicationResource, securityResource, localEnvResource, localSecurityResource
        ).toArray(new Resource[0]));
        Mockito.when(propertyScannerMock.getEnvironment()).then(__ -> Environments.LOCAL);

        VirtualFileEtcdAppPropertyScanner virtualFileEtcdAppPropertyScanner = new VirtualFileEtcdAppPropertyScanner(
            propertyScannerMock, etcdClientMock,
            VirtualFileEtcdConfig.newBuilder()
                .generalDatasource()
                .datasources("00-application.properties", "application.properties")
                .datasources("local/03-my.properties", "my/user/data.properties")
                .datasources("dev/05-dev.properties", "dev.properties")
                .resource("local/01-app.properties", "/data/app.properties")
                .build());

        List<Resource> actualResources = Arrays.asList(virtualFileEtcdAppPropertyScanner.getResources());
        List<Resource> expectedResources = Arrays.asList(
            new VirtualFileEtcdResource("00-application.properties",
                "/datasources/development/yandex/market-datasources/application.properties",
                etcdClientMock),
            securityResource,
            new VirtualFileEtcdResource("50_datasources.properties",
                "/datasources/development/yandex/market-datasources/datasources.properties",
                etcdClientMock),
            localEnvResource,
            new VirtualFileEtcdResource("local/01-app.properties",
                "/data/app.properties", etcdClientMock),
            localSecurityResource,
            new VirtualFileEtcdResource("local/03-my.properties",
                "/datasources/development/yandex/market-datasources/my/user/data.properties", etcdClientMock)
        );
        Assert.assertEquals(expectedResources, actualResources);
    }
}

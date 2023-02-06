package ru.yandex.market.mboc.app;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.deepmind.common.config.AvailabilityTaskQueueConfig;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.mvc.ParseQueryListController;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.config.KeyValueConfig;
import ru.yandex.market.mboc.common.config.LazyBeanFactoryPostProcessor;
import ru.yandex.market.mboc.common.config.TestYtConfig;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;

/**
 * @author yuramalinov
 * @created 25.09.18
 */
@TestConfiguration
@Import({
    TestYtConfig.class,
    ParseQueryListController.class,
    KeyValueConfig.class,
})
@RequiredArgsConstructor
public class AppTestConfiguration {
    private final KeyValueConfig keyValueConfig;

    @Bean
    public static LazyBeanFactoryPostProcessor lazyBeanFactoryPostProcessor() {
        return new LazyBeanFactoryPostProcessor(AvailabilityTaskQueueConfig.class);
    }

    @Bean
    @Primary
    public MasterDataHelperService masterDataHelperService() {
        MasterDataServiceMock masterDataServiceMock =
            new MasterDataServiceMock();
        SupplierDocumentServiceMock supplierDocumentServiceMock =
            new SupplierDocumentServiceMock(masterDataServiceMock);
        MasterDataHelperService result =
            new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
                new SupplierConverterServiceMock(), keyValueConfig.storageKeyValueService());
        return result;
    }
}

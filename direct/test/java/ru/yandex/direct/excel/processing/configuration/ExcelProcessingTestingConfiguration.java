package ru.yandex.direct.excel.processing.configuration;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.excel.processing.service.internalad.CryptaSegmentDictionariesService;

@Configuration
@Import({ExcelProcessingConfiguration.class, CoreTestingConfiguration.class})
@ParametersAreNonnullByDefault
class ExcelProcessingTestingConfiguration {

    @MockBean
    public CryptaSegmentDictionariesService cryptaSegmentDictionariesService;

}

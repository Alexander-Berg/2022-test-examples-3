package ru.yandex.market.wms.common.spring.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.model.enums.ReceiptType;
import ru.yandex.market.wms.common.model.enums.ReceivingContainerType;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.implementation.AltSkuDao;

import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.common.spring.service.ReceivingContainerService.YM_ANOID_INPUT_REGEX;
import static ru.yandex.market.wms.common.spring.service.ReceivingContainerService.YM_ID_INPUT_REGEX;

@ExtendWith(MockitoExtension.class)
class ReceivingContainerServiceTest extends BaseTest {

    @Mock
    private DbConfigService configService;
    @Mock
    private AltSkuDao altSkuDao;

    private ReceivingContainerService service;

    @BeforeEach
    void setUp() {
        this.service = new ReceivingContainerService(null, configService, altSkuDao,
                null, null, null, null, null);
    }

    @Test
    void validateContainerIdAnomalyOk() {
        String containerId = "AN123";

        when(configService.getConfig(YM_ANOID_INPUT_REGEX, ".*")).thenReturn("^(AN|ANOLD)\\d+$");

        boolean result = service.validateContainerId(containerId, ReceivingContainerType.ANOMALY, ReceiptType.DEFAULT);
        assertions.assertThat(result).isTrue();
    }

    @Test
    void validateContainerIdAnomalyException() {
        String containerId = "CART123";

        when(configService.getConfig(YM_ANOID_INPUT_REGEX, ".*")).thenReturn("^(AN|ANOLD)\\d+$");

        assertions.assertThatThrownBy(
                        () -> service
                                .validateContainerId(containerId, ReceivingContainerType.ANOMALY, ReceiptType.DEFAULT))
                .hasMessageContaining("Container id %s does not match regex".formatted(containerId));
    }

    @Test
    void validateContainerIdRegularOkReceiptNull() {
        String containerId = "CART123";

        when(configService.getConfig(YM_ID_INPUT_REGEX, ".*")).thenReturn("^(CART|PLT|L|TOT|CDR|RCP|VS|TM|BL|BM)\\d+$");

        boolean result = service.validateContainerId(containerId, ReceivingContainerType.STOCK, null);
        assertions.assertThat(result).isTrue();
    }

    @Test
    void validateContainerIdRegularOk() {
        String containerId = "CART123";

        boolean result = service.validateContainerId(containerId, ReceivingContainerType.STOCK, ReceiptType.DEFAULT);
        assertions.assertThat(result).isTrue();
    }

    @Test
    void validateContainerIdRegularException() {
        String containerId = "AN123";

        assertions.assertThatThrownBy(
                        () -> service
                                .validateContainerId(containerId, ReceivingContainerType.STOCK, ReceiptType.DEFAULT))
                .hasMessageContaining("Container id %s does not match regex".formatted(containerId));
    }

    @Test
    void validateContainerIdRegularExceptionReceiptNull() {
        String containerId = "AN123";

        when(configService.getConfig(YM_ID_INPUT_REGEX, ".*")).thenReturn("^(CART|PLT|L|TOT|CDR|RCP|VS|TM|BL|BM)\\d+$");

        assertions.assertThatThrownBy(
                        () -> service
                                .validateContainerId(containerId, ReceivingContainerType.STOCK, null))
                .hasMessageContaining("Container id %s does not match regex".formatted(containerId));
    }
}

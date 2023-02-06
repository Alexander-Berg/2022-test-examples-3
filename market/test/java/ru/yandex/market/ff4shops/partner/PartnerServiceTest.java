package ru.yandex.market.ff4shops.partner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.environment.EnvironmentService;
import ru.yandex.market.ff4shops.partner.dao.model.PartnerEntity;
import ru.yandex.market.ff4shops.partner.service.PartnerService;
import ru.yandex.market.ff4shops.partner.service.StocksByPiExperiment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PartnerServiceTest extends FunctionalTest {
    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private StocksByPiExperiment stocksByPiExperiment;

    @Autowired
    private PartnerService partnerService;

    @BeforeEach
    public void init() {
        stocksByPiExperiment.resetCachingVariables();
        environmentService.setValue(StocksByPiExperiment.STOCKS_BY_NEW_FLAG_VAR, "false");
    }

    @Test
    public void testStocksByPiOnAndExperimentOn() {
        environmentService.setValue(StocksByPiExperiment.STOCKS_BY_NEW_FLAG_VAR, "true");

        assertThat(partnerService.worksWithStocksByPartnerInterface(mockEntityWithStocksByPiOnAndCpaOff())).isTrue();
    }

    @Test
    public void testStocksByPiOnAndExperimentOff() {
        assertThat(partnerService.worksWithStocksByPartnerInterface(mockEntityWithStocksByPiOnAndCpaOff())).isFalse();
    }

    @Test
    public void testStocksByPiOffAndExperimentOn() {
        environmentService.setValue(StocksByPiExperiment.STOCKS_BY_NEW_FLAG_VAR, "true");

        assertThat(partnerService.worksWithStocksByPartnerInterface(mockEntityWithStocksByPiOffAndCpaOn())).isFalse();
    }

    @Test
    public void testStocksByPiOffAndExperimentOff() {
        assertThat(partnerService.worksWithStocksByPartnerInterface(mockEntityWithStocksByPiOffAndCpaOn())).isTrue();
    }

    private PartnerEntity mockEntityWithStocksByPiOnAndCpaOff() {
        PartnerEntity entity = new PartnerEntity();
        entity.setId(1);
        entity.setCpaPartnerInterface(false);
        entity.setStocksByPartnerInterface(true);

        return entity;
    }

    private PartnerEntity mockEntityWithStocksByPiOffAndCpaOn() {
        PartnerEntity entity = new PartnerEntity();
        entity.setId(1);
        entity.setCpaPartnerInterface(true);
        entity.setStocksByPartnerInterface(false);

        return entity;
    }
}

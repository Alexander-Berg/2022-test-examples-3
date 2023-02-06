package ru.yandex.market.load.admin.dao;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.load.admin.AbstractFunctionalTest;
import ru.yandex.market.load.admin.entity.PromoConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by aproskriakov on 4/11/22
 */
public class PromoConfigurationDaoTest extends AbstractFunctionalTest {

    @Autowired
    protected PromoConfigurationDao dao;

    @Test
    void testGetPromoConfigurationByName() throws JsonProcessingException {
        PromoConfiguration promoConfiguration = PromoConfiguration.builder()
                .name("test")
                .description("test")
                .config(PromoConfiguration.Config.builder()
                        .minimumSetOfOffers(1)
                        .stocksThresholdForOffer(2)
                        .totalStocksAmount(20)
                        .stores(List.of(new PromoConfiguration.Config.Store(1, 1)))
                        .build())
                .build();

        dao.saveOrUpdatePromoConfiguration(promoConfiguration);


        PromoConfiguration returnedPromoConfig = dao.getPromoConfigurationByName("test").get();
        assertEquals(returnedPromoConfig.getName(), "test");
        assertEquals(returnedPromoConfig.getConfig().getStores().get(0).getRegion(), 1);
    }
}

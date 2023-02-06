package ru.yandex.market.fulfillment.wrap.marschroute.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.marschroute.api.response.services.OrderService;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteRawService;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.service.MarschrouteService;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class MarschrouteRawServiceRepositoryTest extends RepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MarschrouteRawServiceRepository repository;

    /**
     * Проверяем то, что в БД успешно сохраняется информация об услуге в первоначальном виде в JSONB формате.
     */
    @Test
    @DatabaseSetup("classpath:repository/raw_marschroute_order_service/save_before.xml")
    @ExpectedDatabase(value = "classpath:repository/raw_marschroute_order_service/save_after.xml", assertionMode = NON_STRICT_UNORDERED)
    void saveRawService() {
        transactionTemplate.execute(t -> {
            MarschrouteService service = entityManager.find(MarschrouteService.class, 0L);

            MarschrouteRawService raw = new MarschrouteRawService(
                new OrderService()
                    .setId(6L)
                    .setDate(LocalDate.of(1970, 1, 1).atStartOfDay())
                    .setOrderId("1")
                    .setSumNds(BigDecimal.ZERO)
                    .setNameService("Упаковка заказа"),
                service
            );

            repository.save(raw);
            entityManager.flush();

            return null;
        });
    }
}

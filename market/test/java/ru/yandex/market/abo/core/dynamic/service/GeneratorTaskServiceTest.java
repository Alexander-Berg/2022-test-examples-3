package ru.yandex.market.abo.core.dynamic.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.gen.model.GeneratorProfile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 *         created on 15.06.17.
 */
@Transactional("jpaPgTransactionManager")
public class GeneratorTaskServiceTest extends EmptyTest {
    @Autowired
    private GeneratorTaskService generatorTaskService;

    @Test
    public void getAllActive() throws Exception {
        List<GeneratorProfile> activeGens = generatorTaskService.getAllTasks();
        activeGens.forEach(g -> {
            assertNotNull(g.getCronExpression());
            assertTrue(g.isActive());
        });
    }

    @Test
    public void getTicketBuilders() throws Exception {
        List<GeneratorProfile> ticketBuilders = generatorTaskService.getTicketBuilders();
        ticketBuilders.forEach(gen -> assertTrue(gen.getGenType().getTicketBuilder()));
    }
}
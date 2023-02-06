package ru.yandex.market.adv.promo.service.promo.priorities;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PromoPrioritiesServiceTest extends FunctionalTest {
    @Autowired
    private PromoPrioritiesService promoPrioritiesService;

    @Test
    public void testGetNextPriority() {
        long nextPriority = promoPrioritiesService.getPriority();
        assertThat(nextPriority).isEqualTo(1);
        nextPriority = promoPrioritiesService.getPriority();
        assertThat(nextPriority).isEqualTo(2);
        nextPriority = promoPrioritiesService.getPriority();
        assertThat(nextPriority).isEqualTo(3);
    }
}

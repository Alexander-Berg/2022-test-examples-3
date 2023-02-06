package ru.yandex.market.global.index.domain.index;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.index.BaseLocalTest;

@Slf4j
@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReindexServiceLocalTest extends BaseLocalTest {
    private final ReindexService reindexService;

    @Test
    public void testMigrateAll() {
        reindexService.migrateAll();
    }

    @Test
    public void testReindexAll() {
        reindexService.reindexAll();
    }
}

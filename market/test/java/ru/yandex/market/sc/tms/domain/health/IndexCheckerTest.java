package ru.yandex.market.sc.tms.domain.health;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;

@EmbeddedDbTmsTest
@AllArgsConstructor(onConstructor = @__(@Autowired))
class IndexCheckerTest {
    IndexChecker indexChecker;

    @Test
    @DisplayName("Проверяем, что запрос запускается")
    public void checkQueryExecuted() {
        indexChecker.pushIndexErrorsToJuggler();
    }
}

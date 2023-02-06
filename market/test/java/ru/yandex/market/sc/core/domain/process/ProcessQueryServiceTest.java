package ru.yandex.market.sc.core.domain.process;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.process.repository.Process;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ProcessQueryServiceTest {
    private final TestFactory testFactory;
    private final ProcessQueryService processQueryService;

    @Test
    void getAllProcessesTest() {
        var p1 = testFactory.storedProcess("system-1", "display-1");
        var p2 = testFactory.storedProcess("system-2", "display-2");
        var processes = processQueryService.findAll();
        assertThat(processes).containsExactlyInAnyOrder(
                new Process().setId(p1.getId()).setSystemName("system-1").setDisplayName("display-1"),
                new Process().setId(p2.getId()).setSystemName("system-2").setDisplayName("display-2")
        );
    }
}

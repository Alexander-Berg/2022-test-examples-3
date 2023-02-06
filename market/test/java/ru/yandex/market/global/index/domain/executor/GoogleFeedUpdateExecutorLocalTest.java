package ru.yandex.market.global.index.domain.executor;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.index.BaseLocalTest;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GoogleFeedUpdateExecutorLocalTest extends BaseLocalTest {
    private final GoogleFeedUpdateExecutor googleFeedUpdateExecutor;

    @Test
    public void test() {
        googleFeedUpdateExecutor.doRealJob(null);
    }
}

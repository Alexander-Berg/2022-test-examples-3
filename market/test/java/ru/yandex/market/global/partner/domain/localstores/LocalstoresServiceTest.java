package ru.yandex.market.global.partner.domain.localstores;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.partner.BaseLocalTest;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Disabled
public class LocalstoresServiceTest extends BaseLocalTest {
    private final LocalstoresService localstoresService;

    @Test
    public void test() {
        localstoresService.processLocalstoresTicket("LOCALSTORES-468");
        localstoresService.processLocalstoresTicket("LOCALSTORES-468");
    }
}

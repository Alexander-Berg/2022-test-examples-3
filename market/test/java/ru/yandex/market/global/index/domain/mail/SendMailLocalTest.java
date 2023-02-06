package ru.yandex.market.global.index.domain.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.index.BaseLocalTest;

@Slf4j
@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SendMailLocalTest extends BaseLocalTest {
    private final SendMailService sendMailService;

    @Test
    public void test() {
        sendMailService.sendHmlMessage(
                "moskovkin@yandex-team.ru",
                "gmcontent@yandex-team.ru",
                "subbj",
                "body"
        );
    }
}

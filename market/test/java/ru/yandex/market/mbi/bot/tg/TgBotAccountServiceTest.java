package ru.yandex.market.mbi.bot.tg;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.bot.FunctionalTest;
import ru.yandex.market.mbi.bot.tg.model.TgBotAccount;
import ru.yandex.market.mbi.bot.tg.service.TgBotAccountService;
import ru.yandex.market.mbi.bot.tg.service.impl.TgBotAccountServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Функциональные тесты на {@link TgBotAccountServiceImpl}.
 */
class TgBotAccountServiceTest extends FunctionalTest {

    private static final String BOT_1 = "bot_1";
    private static final String BOT_1_TOKEN_HASH = "3c469e9d6c5875d37a43f353d4f88e61fcf812c66eee3457465a40b0da4153e0";
    private static final String BOT_2 = "bot_2";
    private static final String BOT_2_TOKEN_HASH = "391d1a5ed8124a6087c3ea4f3dd2003004b7ffdbb7866fe37c1a6562b9f5c074";
    private static final BiMap<String, String> TG_TOKEN_HASH = HashBiMap.create(Map.of(
            BOT_1, BOT_1_TOKEN_HASH,
            BOT_2, BOT_2_TOKEN_HASH
    ));

    @Autowired
    private TgBotAccountService tgBotAccountService;

    @DisplayName("Проверка получения аккаунтов по id")
    @ParameterizedTest
    @ValueSource(strings = {BOT_1, BOT_2})
    void testGetAccounts(String id) {
        TgBotAccount account = tgBotAccountService.getAccount(id);
        assertThat(account)
                .returns(id, Assertions.from(TgBotAccount::getId))
                .returns(TG_TOKEN_HASH.get(id), Assertions.from(TgBotAccount::getTokenHash));
    }

    @DisplayName("Проверка получения аккаунтов по token hash")
    @ParameterizedTest
    @ValueSource(strings = {BOT_1_TOKEN_HASH, BOT_2_TOKEN_HASH})
    void testGetAccountsByTokenHash(String tokenHash) {
        TgBotAccount account = tgBotAccountService.getAccountByTokenHash(tokenHash);
        assertThat(account)
                .returns(tokenHash, Assertions.from(TgBotAccount::getTokenHash))
                .returns(TG_TOKEN_HASH.inverse().get(tokenHash), Assertions.from(TgBotAccount::getId));
    }

    @Test
    @DisplayName("Проверка получения всех аккаунтов")
    void testGetAllAccounts() {
        Collection<TgBotAccount> allAccounts = tgBotAccountService.getAllAccounts();
        assertThat(allAccounts)
                .map(TgBotAccount::getId)
                .containsExactlyInAnyOrder(BOT_1, BOT_2);
    }

    @Test
    @DisplayName("Проверка получения дефолтного аккаунта")
    void testGetDefaultAccount() {
        TgBotAccount defaultAccount = tgBotAccountService.getDefaultAccount();

        assertThat(defaultAccount).returns(BOT_1, Assertions.from(TgBotAccount::getId));
    }

    @ParameterizedTest
    @CsvSource(value = {BOT_1 + ",true", BOT_2 + ",true", "unknown,false"})
    void testCheckAccountExistence(String botId, boolean expected) {
        assertThat(tgBotAccountService.accountExists(botId)).isEqualTo(expected);
    }
}

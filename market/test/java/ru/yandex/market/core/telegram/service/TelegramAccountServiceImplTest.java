package ru.yandex.market.core.telegram.service;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.contact.model.ContactEmail;
import ru.yandex.market.core.contact.model.ContactWithEmail;
import ru.yandex.market.core.telegram.dao.TelegramAccountDao;
import ru.yandex.market.core.telegram.model.TelegramAccount;
import ru.yandex.market.notification.telegram.bot.client.PartnerBotRestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelegramAccountServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long TELEGRAM_ID = 100L;
    private static final String BOT_ID = "IAmTgRobot";
    private static final String EMAIL = "a@ya.ru";

    private final TelegramAccountDao telegramAccountDao = mock(TelegramAccountDao.class);
    private final PartnerBotRestClient partnerBotRestClient = mock(PartnerBotRestClient.class);
    private final ContactService contactService = mock(ContactService.class);

    private final TelegramAccountService telegramAccountService = new TelegramAccountServiceImpl(
            telegramAccountDao,
            partnerBotRestClient,
            contactService
    );


    @Test
    @DisplayName("getContactEmails возвращает пустой список если нет аккаунтов с telegramId")
    void getContactEmailsShouldReturnEmptyListIThereAreNoTgAccounts() {
        when(telegramAccountDao.findByTelegramId(BOT_ID, TELEGRAM_ID)).thenReturn(Optional.empty());

        var actual = telegramAccountService.getContactEmails(BOT_ID, TELEGRAM_ID);
        assertEquals(Set.of(), actual);
    }

    @Test
    @DisplayName("getContactEmails возвращает пустой список если нет аккаунтов валидным email")
    void getContactEmailsShouldReturnEmptyListIfhThereAreNoValidEmails() {
        var telegramAccount = createTelegramAccount();
        when(telegramAccountDao.findByTelegramId(BOT_ID, TELEGRAM_ID)).thenReturn(Optional.of(telegramAccount));

        var actual = telegramAccountService.getContactEmails(BOT_ID, TELEGRAM_ID);
        assertEquals(Set.of(), actual);
    }

    @Test
    @DisplayName("getContactEmails возвращает список валидных email")
    void getContactEmailsShouldReturnEmptyListIfThereAreNoValidEmails() {
        var telegramAccount = createTelegramAccount();
        var contact = createContact();
        when(telegramAccountDao.findByTelegramId(BOT_ID, TELEGRAM_ID)).thenReturn(Optional.of(telegramAccount));
        when(contactService.getContactWithEmailByUid(USER_ID)).thenReturn(contact);

        var actual = telegramAccountService.getContactEmails(BOT_ID, TELEGRAM_ID);
        assertEquals(Set.of(EMAIL), actual);
    }


    private ContactWithEmail createContact() {
        final ContactWithEmail contact = new ContactWithEmail();
        contact.setUserId(USER_ID);
        contact.setEmails(Set.of(new ContactEmail(1L, EMAIL, true, true)));
        return contact;
    }

    private TelegramAccount createTelegramAccount() {
        final TelegramAccount telegramAccount = new TelegramAccount();
        telegramAccount.setTgId(TELEGRAM_ID);
        telegramAccount.setUserId(USER_ID);
        telegramAccount.setBotId(BOT_ID);
        return telegramAccount;
    }

}

package ru.yandex.market.checkout.referee.controller;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.PrivacyMode;
import ru.yandex.market.checkout.entity.RefereeRole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author kukabara
 */
public class UtilTest {

    @Test
    public void guardFileName() {
        assertEquals("Заявление_на__возврат.pdf", Util.guardFileName("Заявление на  возврат.pdf"));
        assertEquals("Super_File_With_Punctual_Symbol.txt", Util.guardFileName("Super-File,With!Punctual_Symbol.txt"));
    }

    @Test
    public <E> void testResolvePrivacy() {
        ConversationStatus open = ConversationStatus.OPEN;
        ConversationStatus arbitrage = ConversationStatus.ARBITRAGE;

        assertNull(Util.resolvePrivacy(open, RefereeRole.USER, null));
        assertEquals(PrivacyMode.PM_TO_USER, Util.resolvePrivacy(open, RefereeRole.USER, PrivacyMode.PM_TO_USER),
                "пользователь может писать сообщения для себя, если общается с чатботом");

        assertNull(Util.resolvePrivacy(open, RefereeRole.SHOP, null));
        assertNull(Util.resolvePrivacy(open, RefereeRole.SHOP, PrivacyMode.PM_TO_SHOP),
                "магазин не может писать сообщения для себя");

        // если идёт арбитраж, то только приватно
        assertEquals(PrivacyMode.PM_TO_USER, Util.resolvePrivacy(arbitrage, RefereeRole.USER, PrivacyMode.PM_TO_USER));
        assertEquals(PrivacyMode.PM_TO_USER, Util.resolvePrivacy(arbitrage, RefereeRole.USER, null));

        assertEquals(PrivacyMode.PM_TO_SHOP, Util.resolvePrivacy(arbitrage, RefereeRole.SHOP, PrivacyMode.PM_TO_SHOP));
        assertEquals(PrivacyMode.PM_TO_SHOP, Util.resolvePrivacy(arbitrage, RefereeRole.SHOP, null));

        // арбитр пишет кому хочет
        assertEquals(PrivacyMode.PM_TO_SHOP, Util.resolvePrivacy(arbitrage, RefereeRole.ARBITER, PrivacyMode.PM_TO_SHOP));
        assertEquals(PrivacyMode.PM_TO_USER, Util.resolvePrivacy(arbitrage, RefereeRole.ARBITER, PrivacyMode.PM_TO_USER));
        assertNull(Util.resolvePrivacy(arbitrage, RefereeRole.ARBITER, null));
    }
}

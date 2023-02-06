package ru.yandex.market.abo.mm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author kukabara
 */
public class MailArchiverTest extends EmptyTest {
    @Autowired
    private MailArchiver mailArchiver;

    @Test
    public void archiveEmail() throws Exception {
        assertTrue(mailArchiver.archiveEmail());
    }

}

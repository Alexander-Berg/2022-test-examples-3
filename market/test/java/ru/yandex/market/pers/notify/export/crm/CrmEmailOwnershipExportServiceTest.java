package ru.yandex.market.pers.notify.export.crm;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.pers.notify.export.ChangedEmailOwnershipDAO;
import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.EmailOwnership;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author apershukov
 */
class CrmEmailOwnershipExportServiceTest extends MarketMailerMockedDbTest {

    private static class OwnershipMatcher extends BaseMatcher<EmailOwnership> {

        private final EmailOwnership expected;

        private OwnershipMatcher(EmailOwnership expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(Object item) {
            if (!(item instanceof EmailOwnership)) {
                return false;
            }
            EmailOwnership actual = (EmailOwnership) item;

            return Objects.equals(expected.getId(), actual.getId()) &&
                    Objects.equals(expected.getEmail(), actual.getEmail()) &&
                    Objects.equals(expected.getIdentity(), actual.getIdentity()) &&
                    Objects.equals(expected.getSource(), actual.getSource()) &&
                    Objects.equals(expected.getStatus(), actual.getStatus()) &&
                    Objects.equals(expected.isActive(), actual.isActive());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expected.toString());
        }
    }

    private static OwnershipMatcher eq(EmailOwnership ownership) {
        return new OwnershipMatcher(ownership);
    }

    private static final Uid UID_1 = new Uid(111L);
    private static final Uid UID_2 = new Uid(222L);

    private static final String EMAIL = "foo@yandex.ru";

    @Autowired
    private ChangedEmailOwnershipDAO changedEmailOwnershipDAO;

    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;

    private TestCrmEmailOwnershipWriter writer = new TestCrmEmailOwnershipWriter();

    private CrmEmailOwnershipExportService service;

    @BeforeEach
    void setUp() {
        service = new CrmEmailOwnershipExportService(jdbcTemplate, writer, changedEmailOwnershipDAO);
    }

    @Test
    void testExportAll() {
        blackBoxPassportService.doReturn(UID_1.getValue(), EMAIL);

        EmailOwnership ownership1 = subscriptionAndIdentityService.createEmailOwnershipIfNecessary(
            UID_1,
            EMAIL,
            false
        );

        EmailOwnership ownership2 = subscriptionAndIdentityService.createEmailOwnershipIfNecessary(
            UID_2,
            "bar@yandex.ru",
            false
        );

        subscriptionAndIdentityService.setActiveEmail(UID_1, EMAIL);
        ownership1.setActive(true);

        service.exportAll();

        List<EmailOwnership> written = writer.getOwnerships();
        assertEquals(2, written.size());

        assertThat(written.get(0), eq(ownership1));
        assertThat(written.get(1), eq(ownership2));
    }

    @Test
    void testExportChanged() {
        EmailOwnership ownership1 = subscriptionAndIdentityService.createEmailOwnershipIfNecessary(
            UID_1,
            "foo@yandex.ru",
            false
        );

        subscriptionAndIdentityService.createEmailOwnershipIfNecessary(
            UID_2,
            "bar@yandex.ru",
            false
        );

        // Удаляем возможные предыдущие записи
        jdbcTemplate.update("DELETE FROM CRM_CHANGED_EMAIL_OWNERSHIP");

        changedEmailOwnershipDAO.markAsChanged(ownership1.getId());

        service.exportChanged();

        List<EmailOwnership> written = writer.getOwnerships();
        assertEquals(1, written.size());
        assertThat(written.get(0), eq(ownership1));

        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM CRM_CHANGED_EMAIL_OWNERSHIP",
            Integer.class
        );
        assertEquals(0, count == null ? 0 : count);
    }
}

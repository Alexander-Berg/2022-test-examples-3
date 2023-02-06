package ru.yandex.market.abo.core.authority;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.EmptyTest;
import ru.yandex.market.security.model.Authority;
import ru.yandex.market.security.model.Uidable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 *         @date 02.08.2007
 */
@ContextConfiguration("classpath:abo-core-security.xml")
@Transactional("pgTransactionManager")
public class AssessorCheckerTest extends EmptyTest {

    private static final long ASSESSOR_UID = -111L;
    private static final long FAILED_UID = 23075721L; // bolshakova

    @Autowired
    private AssessorChecker newbieAssessorChecker;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Test
    public void testCheck() {
        pgJdbcTemplate.update("insert into assessor_permission values (-111, 8, now())");

        Uidable uid = () -> ASSESSOR_UID;
        assertTrue(newbieAssessorChecker.check(uid, new Authority("", "")));

        Uidable failedUid = () -> FAILED_UID;
        assertFalse(newbieAssessorChecker.check(failedUid, new Authority("", "")));
    }
}

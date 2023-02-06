package ru.yandex.market.abo.core.problem.partner;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.problem.partner.PartnerProblem;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author artemmz
 *         created on 19.05.17.
 */
public class CpcPartnerProblemProviderTest extends EmptyTest {
    @Autowired
    private PartnerProblemProvider cpcPartnerProblemProvider;

    @Test
    public void getProblems() throws Exception {
        Set<PartnerProblem> problems = cpcPartnerProblemProvider.getProblems(774, false);
        assertNotNull(problems);
    }
}
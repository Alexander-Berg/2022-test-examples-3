package ru.yandex.market.abo.core.problem.partner;

import java.util.Date;
import java.util.List;
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
public class CpaPartnerProblemProviderTest extends EmptyTest {
    @Autowired
    private PartnerProblemProvider cpaPartnerProblemProvider;

    @Test
    public void getProblems() throws Exception {
        Set<PartnerProblem> problems = cpaPartnerProblemProvider.getProblems(10216833, false);
        assertNotNull(problems);
    }

    @Test
    public void getShopsWithCriticalProblems() throws Exception {
        List<Long> shopsWithCriticalProblems = cpaPartnerProblemProvider.getShopsWithCriticalProblems(new Date());
        assertNotNull(shopsWithCriticalProblems);
    }
}
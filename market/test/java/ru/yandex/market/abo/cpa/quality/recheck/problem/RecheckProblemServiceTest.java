package ru.yandex.market.abo.cpa.quality.recheck.problem;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.quality.recheck.problem.model.RecheckProblem;
import ru.yandex.market.abo.cpa.quality.recheck.problem.model.RecheckProblemType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author imelnikov
 */
public class RecheckProblemServiceTest extends EmptyTest {

    @Autowired
    private RecheckProblemService recheckProblemService;

    @Test
    public void getList() {
        final AtomicInteger size = new AtomicInteger(0);
        boolean result = recheckProblemService.getProblemClasses().stream()
                .anyMatch(cl -> {
                    int current = cl.getTypes().size();
                    if (size.get() == 0) {
                        size.set(current);
                        return false;
                    } else {
                        if (current != size.get()) {
                            return true;
                        }
                        return false;
                    }
                });
        assertTrue(result, "Classes should have different number of types");
    }

    @Test
    public void saveProblems() {
        long ticketId = -1L;
        long userId = 2L;

        assertTrue(recheckProblemService.getProblems(ticketId).isEmpty());

        recheckProblemService.getProblemClasses().forEach(pClass -> {
            for (RecheckProblemType type : pClass.getTypes()) {

                RecheckProblem p = RecheckProblem.builder()
                        .ticketId(ticketId)
                        .typeId(type.getId())
                        .userId(userId)
                        .text("3")
                        .build();
                recheckProblemService.save(p);
            }
        });

        assertFalse(recheckProblemService.getProblems(ticketId).isEmpty());

        recheckProblemService.getProblemClasses().forEach(pClass -> {
            for (RecheckProblemType type : pClass.getTypes()) {
                recheckProblemService.deleteByTicketIdAndTypeId(ticketId, type.getId());
            }
        });
    }

    @Test
    public void unique() {
        RecheckProblemType type = recheckProblemService.getProblemClasses().get(0).getTypes().get(0);
        long ticketId = 1L;

        RecheckProblem p1 = RecheckProblem.builder()
                .ticketId(ticketId)
                .typeId(type.getId())
                .build();
        recheckProblemService.save(p1);

        RecheckProblem p2 = RecheckProblem.builder()
                .ticketId(ticketId)
                .typeId(type.getId())
                .build();

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                recheckProblemService.save(p2));
    }

}

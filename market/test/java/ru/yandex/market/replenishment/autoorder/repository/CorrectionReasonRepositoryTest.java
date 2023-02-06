package ru.yandex.market.replenishment.autoorder.repository;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.dto.CorrectionReasonDTO;
import ru.yandex.market.replenishment.autoorder.repository.postgres.CorrectionReasonRepository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
public class CorrectionReasonRepositoryTest extends FunctionalTest {
    @Autowired
    private CorrectionReasonRepository correctionReasonRepository;

    @Test
    public void testFindAll() {
        List<CorrectionReasonDTO> reasons = correctionReasonRepository
                .findAllByActiveOrderByPositionAsc(true).stream()
                .map(cr -> new CorrectionReasonDTO(cr.getPosition(), cr.getName())).collect(Collectors.toList());

        assertThat(reasons, hasSize(21));
        CorrectionReasonDTO reason = reasons.get(0);

        assertEquals(1, reason.getId());
        assertEquals("1. Не согласен с прогнозом", reason.getName());
    }
}

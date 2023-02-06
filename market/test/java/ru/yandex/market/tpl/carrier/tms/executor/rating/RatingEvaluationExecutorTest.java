package ru.yandex.market.tpl.carrier.tms.executor.rating;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.rating.RatingRepository;
import ru.yandex.market.tpl.carrier.core.domain.rating.model.Rating;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.tms.TmsIntTest;


@TmsIntTest
@RequiredArgsConstructor(onConstructor_=@Autowired)
class RatingEvaluationExecutorTest {

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    private final RatingEvaluationExecutor ratingEvaluationExecutor;
    private final RunGenerator runGenerator;
    private final RatingRepository ratingRepository;

    @BeforeEach
    void setUp() {
        runGenerator.generate();
        runGenerator.generate();
        runGenerator.generate();
        runGenerator.generate();
        runGenerator.generate();
    }

    @Test
    @SneakyThrows
    void shouldEvaluateRating() {
        ratingEvaluationExecutor.doRealJob(null);
        List<Rating> ratingList = ratingRepository.findAll();
        Assertions.assertTrue(ratingList.size() % 3 == 0 && ratingList.size() > 0);
    }
}

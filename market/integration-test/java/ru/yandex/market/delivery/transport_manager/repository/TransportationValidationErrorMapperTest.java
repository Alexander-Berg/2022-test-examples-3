package ru.yandex.market.delivery.transport_manager.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationValidationError;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationValidationErrorMapper;

import static org.hamcrest.MatcherAssert.assertThat;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/transportation/validation_errors.xml"
})
class TransportationValidationErrorMapperTest extends AbstractContextualTest {
    @Autowired
    private TransportationValidationErrorMapper mapper;

    @Test
    void get() {
        var transportationValidationErrors = mapper.get(1L);

        softly.assertThat(transportationValidationErrors)
            .containsExactlyInAnyOrder(
                error(1L, 1L, "some error"),
                error(2L, 1L, "another error")
            );
    }

    @Test
    void getMultiple() {
        Map<Long, String> errors = mapper.getErrors(Set.of(1L, 2L));

        assertThat(errors, Is.is(Map.of(
            1L, "some error; another error",
            2L, "some error"
        )));
    }

    @Test
    void delete() {
        mapper.deleteForTransportation(1L);

        softly.assertThat(mapper.get(1L)).isEmpty();
        softly.assertThat(mapper.get(2L)).isNotEmpty();
    }

    @Test
    void save() {
        mapper.insert(3L, List.of("error1", "error2", "error3"));

        softly.assertThat(mapper.get(3L))
            .containsExactlyInAnyOrder(
                error(4L, 3L, "error1"),
                error(5L, 3L, "error2"),
                error(6L, 3L, "error3")
            );
    }

    private static TransportationValidationError error(Long id, Long transportationId, String error) {
        return new TransportationValidationError()
            .setId(id)
            .setTransportationId(transportationId)
            .setError(error);
    }
}

package ru.yandex.market.abo.core.ticket.listener.problem.event;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.core.problem.model.ImmutableProblem;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.ticket.model.TicketTag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author artemmz
 * @date 21.05.18.
 */
class ProblemUpdateEventTest {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void serializeDeserialize() throws IOException {
        Problem problem = Problem.newBuilder()
                .ticketId(1)
                .problemTypeId(1)
                .status(ProblemStatus.NEW)
                .creationTime(new Date())
                .build();
        TicketTag tag = new TicketTag(1);
        tag.setId(0);
        problem.setModificationTag(tag);

        ImmutableProblem immutableCopy = problem.createImmutableCopy();
        ImmutableProblem fromJson = MAPPER.readValue(MAPPER.writeValueAsString(immutableCopy), ImmutableProblem.class);

        assertNotNull(fromJson);
        assertEquals(problem, fromJson);
        assertEquals(problem.getModificationTag(), fromJson.getModificationTag());
        assertEquals(problem.getCreationTime(), fromJson.getCreationTime());
    }
}
package ru.yandex.market.logistics.management.controller;

import java.util.Collections;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.util.collections.Quadruple;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.logistic.edge.LogisticEdgeDto;
import ru.yandex.market.logistics.management.entity.request.logistic.edge.UpdateLogisticEdgesRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.hasResolvedExceptionContainingMessage;

@DatabaseSetup("/data/controller/logisticEdge/before/prepare_data.xml")
class LogisticEdgeControllerTest extends AbstractContextualTest {
    @MethodSource
    @DisplayName("Невалидный запрос")
    @ParameterizedTest(name = "[{index}] {0} {1} {2}")
    void invalidRequest(
        String objectName,
        String fieldName,
        String errorMessage,
        UnaryOperator<UpdateLogisticEdgesRequest.Builder> requestBuilderModifier
    ) throws Exception {
        UpdateLogisticEdgesRequest request = requestBuilderModifier
            .apply(defaultBuilder())
            .build();

        ResultActions resultActions = updateEdges(request)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].objectName").value(objectName))
            .andExpect(jsonPath("errors[0].defaultMessage").value(errorMessage));

        if (fieldName != null) {
            resultActions.andExpect(jsonPath("errors[0].field").value(fieldName));
        }
    }

    @Nonnull
    private static Stream<Arguments> invalidRequest() {
        return Stream.<Quadruple<String, String, String, UnaryOperator<UpdateLogisticEdgesRequest.Builder>>>of(
            Quadruple.of(
                "updateLogisticEdgesRequest",
                "createEdges[]",
                "must not be null",
                b -> b.createEdges(Collections.singleton(null))
            ),
            Quadruple.of(
                "updateLogisticEdgesRequest",
                "deleteEdges[]",
                "must not be null",
                b -> b.deleteEdges(Collections.singleton(null))
            ),
            Quadruple.of(
                "updateLogisticEdgesRequest",
                null,
                "At least one of [createEdges, deleteEdges] must be not empty",
                b -> b.createEdges(null).deleteEdges(null)
            ),
            Quadruple.of(
                "updateLogisticEdgesRequest",
                null,
                "At least one of [createEdges, deleteEdges] must be not empty",
                b -> b.createEdges(Collections.emptySet()).deleteEdges(Collections.emptySet())
            )
        )
            .map(q -> Arguments.of(q.getFirst(), q.getSecond(), q.getThird(), q.getFourth()));
    }

    @Nonnull
    private UpdateLogisticEdgesRequest.Builder defaultBuilder() {
        return UpdateLogisticEdgesRequest.newBuilder()
            .createEdges(Set.of(LogisticEdgeDto.of(1L, 2L)))
            .deleteEdges(Set.of(LogisticEdgeDto.of(2L, 1L)));
    }

    @Test
    @DisplayName("Множества создаваемых и удаляемых ребер пересекаются")
    @ExpectedDatabase(
        value = "/data/controller/logisticEdge/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createAndDeleteSetsIntersect() throws Exception {
        updateEdges(
            UpdateLogisticEdgesRequest.newBuilder()
                .createEdges(Set.of(LogisticEdgeDto.of(10001L, 10004L), LogisticEdgeDto.of(10001L, 10005L)))
                .deleteEdges(Set.of(LogisticEdgeDto.of(10001L, 10004L), LogisticEdgeDto.of(10002L, 10004L)))
                .build()
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].objectName").value("updateLogisticEdgesRequest"))
            .andExpect(
                jsonPath("errors[0].defaultMessage")
                    .value("createEdges and deleteEdges intersect on [(10001, 10004)]")
            );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Создать невалидные ребра")
    @ExpectedDatabase(
        value = "/data/controller/logisticEdge/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void invalidEdges(String displayName, Long from, Long to) throws Exception {
        updateEdges(
            UpdateLogisticEdgesRequest.newBuilder()
                .createEdges(Set.of(LogisticEdgeDto.of(from, to)))
                .build()
        )
            .andExpect(status().isBadRequest())
            .andExpect(hasResolvedExceptionContainingMessage(String.format(
                "Validation errors: invalid segment type connections: [(%s, %s)]",
                from,
                to
            )));
    }

    @Nonnull
    private static Stream<Arguments> invalidEdges() {
        long warehouse = 10003;
        long movement = 10004;
        long linehaul = 10005;
        long pickup = 10006;
        long handing = 10007;

        return Stream.of(
            Arguments.of("warehouse -> warehouse", warehouse, warehouse),
            Arguments.of("warehouse -> linehaul", warehouse, linehaul),
            Arguments.of("warehouse -> pickup", warehouse, pickup),
            Arguments.of("warehouse -> handing", warehouse, handing),
            Arguments.of("movement -> movement", movement, movement),
            Arguments.of("movement -> pickup", movement, pickup),
            Arguments.of("movement -> handing", movement, handing),
            Arguments.of("linehaul -> warehouse", linehaul, warehouse),
            Arguments.of("linehaul -> movement", linehaul, movement),
            Arguments.of("linehaul -> linehaul", linehaul, linehaul),
            Arguments.of("linehaul -> pickup", linehaul, pickup),
            Arguments.of("linehaul -> handing", linehaul, handing),
            Arguments.of("pickup -> warehouse", pickup, warehouse),
            Arguments.of("pickup -> movement", pickup, movement),
            Arguments.of("pickup -> linehaul", pickup, linehaul),
            Arguments.of("pickup -> pickup", pickup, pickup),
            Arguments.of("pickup -> handing", pickup, handing),
            Arguments.of("handing -> warehouse", handing, warehouse),
            Arguments.of("handing -> movement", handing, movement),
            Arguments.of("handing -> linehaul", handing, linehaul),
            Arguments.of("handing -> pickup", handing, pickup),
            Arguments.of("handing -> handing", handing, handing)
        );
    }

    @Test
    @DisplayName("Создать ребра между несуществующими сегментами")
    @ExpectedDatabase(
        value = "/data/controller/logisticEdge/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createEdgesBetweenNonExistingSegments() throws Exception {
        updateEdges(
            UpdateLogisticEdgesRequest.newBuilder()
                .createEdges(Set.of(LogisticEdgeDto.of(10001L, 1L), LogisticEdgeDto.of(10001L, 2L)))
                .build()
        )
            .andExpect(status().isNotFound())
            .andExpect(hasResolvedExceptionContainingMessage("Logistic segments with ids = [1, 2] not found"));
    }

    @Test
    @DisplayName("Создать уже существующие ребра")
    @ExpectedDatabase(
        value = "/data/controller/logisticEdge/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createAlreadyExistingEdges() throws Exception {
        updateEdges(
            UpdateLogisticEdgesRequest.newBuilder()
                .createEdges(Set.of(LogisticEdgeDto.of(10001L, 10002L), LogisticEdgeDto.of(10002L, 10003L)))
                .build()
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создать новые ребра успешно")
    @ExpectedDatabase(
        value = "/data/controller/logisticEdge/after/created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createEdges() throws Exception {
        updateEdges(
            UpdateLogisticEdgesRequest.newBuilder()
                .createEdges(Set.of(LogisticEdgeDto.of(10001L, 10004L), LogisticEdgeDto.of(10002L, 10008L)))
                .build()
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Удалить несуществующие ребра существующих сегментов")
    @ExpectedDatabase(
        value = "/data/controller/logisticEdge/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteNonExistingEdgesBetweenExistingSegments() throws Exception {
        updateEdges(
            UpdateLogisticEdgesRequest.newBuilder()
                .deleteEdges(Set.of(LogisticEdgeDto.of(10001L, 10003L), LogisticEdgeDto.of(10002L, 10004L)))
                .build()
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Удалить несуществующие ребра между несуществующими сегментами")
    @ExpectedDatabase(
        value = "/data/controller/logisticEdge/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteNonExistingEdgesBetweenNonExistingSegments() throws Exception {
        updateEdges(
            UpdateLogisticEdgesRequest.newBuilder()
                .deleteEdges(Set.of(LogisticEdgeDto.of(10001L, 1L), LogisticEdgeDto.of(10002L, 2L)))
                .build()
        )
            .andExpect(status().isNotFound())
            .andExpect(hasResolvedExceptionContainingMessage("Logistic segments with ids = [1, 2] not found"));
    }

    @Test
    @DisplayName("Удалить ребра успешно")
    @ExpectedDatabase(
        value = "/data/controller/logisticEdge/after/deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteEdges() throws Exception {
        updateEdges(
            UpdateLogisticEdgesRequest.newBuilder()
                .deleteEdges(Set.of(LogisticEdgeDto.of(10001L, 10002L), LogisticEdgeDto.of(10005L, 10006L)))
                .build()
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Не удалять ничего если пришел пустой список ребер на удаление")
    @ExpectedDatabase(
        value = "/data/controller/logisticEdge/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteEmptyEdges() throws Exception {
        updateEdges(
            UpdateLogisticEdgesRequest.newBuilder()
                .createEdges(Set.of(LogisticEdgeDto.of(10001L, 10002L)))
                .deleteEdges(Set.of())
                .build()
        )
            .andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions updateEdges(UpdateLogisticEdgesRequest request) throws Exception {
        return mockMvc.perform(
            post("/externalApi/logistic-edges")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );
    }
}

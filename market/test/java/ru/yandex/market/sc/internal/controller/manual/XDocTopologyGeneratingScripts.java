package ru.yandex.market.sc.internal.controller.manual;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sc.core.domain.cell.model.CellSubType;

import static ru.yandex.market.sc.internal.controller.manual.xdoc.TopologyGenerator.createTopologyRequest;
import static ru.yandex.market.sc.internal.controller.manual.xdoc.TopologyGenerator.evenOddCellNames;
import static ru.yandex.market.sc.internal.controller.manual.xdoc.TopologyGenerator.rectangularCellNames;

public class XDocTopologyGeneratingScripts {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Disabled
    @DisplayName("Топология с четными/нечетными рядами")
    @Test
    void evenOddTopologyCellNames() {
        var request = createTopologyRequest(
                CellSubType.BUFFER_XDOC_LOCATION,
                "10001787516",
                282L,
                evenOddCellNames(1, 10, "SAM_", "_", "")
        );

        System.out.println(toJson(request));
    }

    @Disabled
    @DisplayName("Прямоугольная топология")
    @Test
    void rectangularTopology() {
        var request = createTopologyRequest(
                CellSubType.BUFFER_XDOC_LOCATION,
                "10000010731",
                284L,
                rectangularCellNames(19, 30, 4, "RND_", "_", "b", rn -> rn % 2 == 1)
        );

        System.out.println(toJson(request));
    }

    @SneakyThrows
    private static String toJson(Object any) {
        return MAPPER.writeValueAsString(any);
    }

}

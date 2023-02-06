package ru.yandex.market.logistics.nesu.controller.document;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.MdsFileDto;
import ru.yandex.market.logistics.lom.model.dto.OrderLabelDto;
import ru.yandex.market.logistics.lom.model.enums.FileType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение ярлыка заказа")
class DocumentGelLabelTest extends AbstractContextualTest {
    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Получить ярлык заказа")
    void getLabelOk() throws Exception {
        when(lomClient.getLabel(1L)).thenReturn(Optional.of(createOrderLabelDto()));
        getLabel()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/document/label_1.json"));
        verify(lomClient).getLabel(1L);
    }

    @Test
    @DisplayName("Ярлык не найден")
    void getLabelNotFound() throws Exception {
        when(lomClient.getLabel(1L)).thenReturn(Optional.empty());
        getLabel()
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/document/label_not_found.json"));
        verify(lomClient).getLabel(1L);
    }

    @Nonnull
    private OrderLabelDto createOrderLabelDto() {
        return OrderLabelDto.builder()
            .orderId(1L)
            .partnerId(2L)
            .labelFile(createMdsFileDto(1L))
            .build();
    }

    @Nonnull
    private MdsFileDto createMdsFileDto(long id) {
        return MdsFileDto.builder()
            .id(id)
            .fileName(String.format("label-%d.pdf", id))
            .fileType(FileType.ORDER_LABEL)
            .mimeType("application/pdf")
            .url("https://mds.url/lom-doc-test/" + id)
            .build();
    }

    @Nonnull
    private ResultActions getLabel() throws Exception {
        return mockMvc.perform(
            get("/back-office/documents/label").param("orderId", String.valueOf(1L))
        );
    }
}

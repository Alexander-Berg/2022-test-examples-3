package ru.yandex.market.tpl.core.domain.receipt;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.receipt.CreateReceiptRequestDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptDataDto;
import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.receipt.ReceiptDataRepositoryTest.filledData;

/**
 * @author valter
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class ReceiptDtoMapperTest {

    private final ReceiptDtoMapper receiptDtoMapper;
    private final ReceiptDataRepository receiptDataRepository;

    @Test
    void mapReceiptDataDto() {
        ReceiptData expected = receiptDataRepository.save(filledData());
        ReceiptDataDto dto = receiptDtoMapper.mapToDto(expected, null).getReceipt();

        ReceiptData actual = receiptDtoMapper.mapFromDto(
                new CreateReceiptRequestDto(
                        expected.getReceiptId(),
                        null,
                        dto,
                        expected.getPayload(),
                        null
                ), expected.getServiceClient(),
                null
        );
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id", "updatedAt", "createdAt", "status", "errorDescription", "items")
                .isEqualTo(expected);

        for (int i = 0; i < expected.getItems().size(); i++) {
            assertThat(actual.getItems().get(i))
                    .usingRecursiveComparison()
                    .ignoringFields("id", "updatedAt", "createdAt", "orderNum", "receiptData")
                    .isEqualTo(expected.getItems().get(i));
        }
    }

}

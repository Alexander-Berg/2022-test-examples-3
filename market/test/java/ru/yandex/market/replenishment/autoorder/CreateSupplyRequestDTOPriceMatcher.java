package ru.yandex.market.replenishment.autoorder;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.mockito.ArgumentMatcher;

import ru.yandex.market.ff.client.dto.CreateSupplyRequestDTO;

@RequiredArgsConstructor
public class CreateSupplyRequestDTOPriceMatcher implements ArgumentMatcher<CreateSupplyRequestDTO> {

    private final BigDecimal price;

    @Override
    public boolean matches(CreateSupplyRequestDTO argument) {
        return argument.getItems()
            .stream()
            .allMatch(createSupplyRequestDTO -> price.equals(createSupplyRequestDTO.getPrice()));
    }

}

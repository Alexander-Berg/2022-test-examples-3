package ru.yandex.market.api.partner.controllers.outlet.model;

import java.util.Arrays;
import java.util.Objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.core.orginfo.model.OrganizationType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Тесты для {@link OutletLegalOrganizationTypeDTO}.
 *
 * @author Vladislav Bauer
 */
class OutletLegalOrganizationTypeDTOTest {

    @Test
    @DisplayName("Присутствуют все необходимые mapping'и для преобразования OrganizationType в OutletLegalOrganizationTypeDTO")
    void testFrom() {
        final long count = Arrays.stream(OrganizationType.values())
                .map(type -> {
                    try {
                        return OutletLegalOrganizationTypeDTO.from(type);
                    } catch (final IllegalArgumentException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .count();

        assertThat("Нужно поправить данный тест или OutletLegalOrganizationTypeDTO", count, equalTo(6L));
    }

    @ParameterizedTest
    @EnumSource(OutletLegalOrganizationTypeDTO.class)
    @DisplayName("Присутствуют все mapping'и для преобразования OutletLegalOrganizationTypeDTO в OrganizationType")
    void testToModel(final OutletLegalOrganizationTypeDTO type) {
        assertThat(OutletLegalOrganizationTypeDTO.toModel(type), notNullValue());
    }

}

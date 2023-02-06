package ru.yandex.market.logistics.management.facade;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.converter.RegionEntityConverter;
import ru.yandex.market.logistics.management.domain.converter.lgw.LocationFilterConverter;
import ru.yandex.market.logistics.management.domain.dto.front.ReferenceHelper;
import ru.yandex.market.logistics.management.domain.dto.front.partnerCountry.PartnerCountryDetailDto;
import ru.yandex.market.logistics.management.domain.dto.front.partnerCountry.PartnerCountryGridDto;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerCountry;
import ru.yandex.market.logistics.management.domain.entity.RegionEntity;
import ru.yandex.market.logistics.management.service.client.PartnerCountryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Работа со странами партнёров")
class PartnerCountryFacadeTest extends AbstractTest {

    private static final long PARTNER_ID_WITH_COUNTRIES = 100;
    private static final long PARTNER_ID_WITH_NO_COUNTRIES = 102;

    private static final int BELARUS = 149;

    private PartnerCountryService partnerCountryService;

    private PartnerCountryFacade partnerCountryFacade;

    @BeforeEach
    void setUp() {
        partnerCountryService = mock(PartnerCountryService.class);

        partnerCountryFacade = new PartnerCountryFacade(
            partnerCountryService,
            new LocationFilterConverter(),
            new RegionEntityConverter()
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(partnerCountryService);
    }

    @Test
    @DisplayName("Получение фильтров по локациям для партнёра со связанными странами доставки")
    void testGetLocationFiltersForPartnerNotEmpty() {
        doReturn(
            List.of(
                new RegionEntity().setId(1).setName("country 1"),
                new RegionEntity().setId(2).setName("country 2")
            )
        ).when(partnerCountryService).getPartnerCountries(eq(PARTNER_ID_WITH_COUNTRIES));

        assertThat(
            partnerCountryFacade.getLocationFiltersForPartnerOrNullIfEmpty(PARTNER_ID_WITH_COUNTRIES)
        ).hasSize(2);

        verify(partnerCountryService).getPartnerCountries(eq(PARTNER_ID_WITH_COUNTRIES));
    }

    @Test
    @DisplayName("Получение фильтров по локациям для партнёра без связанных стран доставки - возвращается null")
    void testGetLocationFiltersForPartnerIsEmptySoNullReturned() {
        doReturn(
            List.of()
        ).when(partnerCountryService).getPartnerCountries(eq(PARTNER_ID_WITH_NO_COUNTRIES));

        assertThat(
            partnerCountryFacade.getLocationFiltersForPartnerOrNullIfEmpty(PARTNER_ID_WITH_NO_COUNTRIES)
        )
            .isNull();

        verify(partnerCountryService).getPartnerCountries(eq(PARTNER_ID_WITH_NO_COUNTRIES));
    }

    @Test
    @DisplayName("Получение таблицы стран партнёра для админки")
    void testGetPartnerCountryGrid() {
        when(partnerCountryService.getPartnerCountries(
            eq(Map.of("partnerId", String.valueOf(PARTNER_ID_WITH_COUNTRIES))),
            eq(PageRequest.of(0, 10))
        )).thenReturn(
            new PageImpl<>(List.of(createBelarusRegionEntity()))
        );

        PartnerCountryGridDto gridDto = partnerCountryFacade.getPartnerCountryGrid(
            Map.of("partnerId", String.valueOf(PARTNER_ID_WITH_COUNTRIES)),
            PageRequest.of(0, 10)
        ).getContent().get(0);

        verify(partnerCountryService).getPartnerCountries(
            eq(Map.of("partnerId", String.valueOf(PARTNER_ID_WITH_COUNTRIES))),
            eq(PageRequest.of(0, 10))
        );

        assertThat(gridDto)
            .as("Asserting that the grid DTO is valid")
            .isEqualTo(PartnerCountryGridDto.builder().id(BELARUS).countryId(BELARUS).countryName("Беларусь").build());
    }

    @Test
    @DisplayName("Получение страны партнёра для админки")
    void testGetPartnerCountryDetails() {
        Partner partner = new Partner().setId(PARTNER_ID_WITH_COUNTRIES).setName("Партнёр 1");

        when(partnerCountryService.findPartnerCountry(eq(PARTNER_ID_WITH_COUNTRIES), eq(BELARUS)))
            .thenReturn(new PartnerCountry(partner, createBelarusRegionEntity()));

        PartnerCountryDetailDto detailDto =
            partnerCountryFacade.getPartnerCountryDetails(PARTNER_ID_WITH_COUNTRIES, BELARUS);

        verify(partnerCountryService).findPartnerCountry(eq(PARTNER_ID_WITH_COUNTRIES), eq(BELARUS));

        assertThat(detailDto)
            .as("Asserting that the detail DTO")
            .isEqualTo(
                new PartnerCountryDetailDto(BELARUS, ReferenceHelper.getPartnerReference(partner), "149 : Беларусь")
            );
    }

    @Test
    @DisplayName("Создание страны партнёра")
    void testCreatePartnerCountry() {
        partnerCountryFacade.createPartnerCountry(PARTNER_ID_WITH_COUNTRIES, BELARUS);

        verify(partnerCountryService).createPartnerCountry(eq(PARTNER_ID_WITH_COUNTRIES), eq(BELARUS));
    }

    @Test
    @DisplayName("Удаление страны партнёра")
    void testDeletePartnerCountry() {
        partnerCountryFacade.deletePartnerCountry(PARTNER_ID_WITH_COUNTRIES, BELARUS);

        verify(partnerCountryService).deletePartnerCountry(eq(PARTNER_ID_WITH_COUNTRIES), eq(BELARUS));
    }

    private RegionEntity createBelarusRegionEntity() {
        return new RegionEntity().setId(149).setName("Беларусь").setType(RegionType.COUNTRY);
    }
}

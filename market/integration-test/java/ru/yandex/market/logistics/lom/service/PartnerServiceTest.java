package ru.yandex.market.logistics.lom.service;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.converter.lms.PartnerTypeLmsConverter;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.service.partner.PartnerService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Поиск партнёров")
class PartnerServiceTest extends AbstractContextualTest {

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private PartnerTypeLmsConverter partnerTypeLmsConverter;

    @Test
    @DisplayName("Получение самого раннего катофа для связки")
    void getCutoffForPartner() {
        LocalTime testEarliestCutoff = LocalTime.of(10, 12);
        PartnerRelationEntityDto testRelation = PartnerRelationEntityDto.newBuilder()
            .cutoffs(Set.of(
                CutoffResponse.newBuilder().cutoffTime(LocalTime.of(12, 32)).build(),
                CutoffResponse.newBuilder().cutoffTime(testEarliestCutoff).build()
            ))
            .build();
        softly.assertThat(partnerService.getCutoffForPartnerRelationOptional(testRelation))
            .contains(testEarliestCutoff);
    }

    @Test
    @DisplayName("Для пустых связок не возвращается катоф")
    void getCutoffForPartnerReturnNothingIfNoCutoff() {
        PartnerRelationEntityDto testEmptyRelation = PartnerRelationEntityDto.newBuilder()
            .cutoffs(Set.of())
            .build();
        softly.assertThat(partnerService.getCutoffForPartnerRelationOptional(testEmptyRelation)).isEmpty();
    }

    @MethodSource("hasPartnerSettingsMethodsArgs")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Проверка наличия реализации метода у партнера")
    void hasPartnerSettingMethod(
        String displayName,
        List<SettingsMethodDto> lmsResult,
        boolean hasPartnerMethod
    ) {
        String methodType = "type";
        long partnerId = 1L;

        doReturn(lmsResult)
            .when(lmsClient).searchPartnerApiSettingsMethods(buildSettingsMethodFilter(methodType, partnerId));

        softly.assertThat(partnerService.hasPartnerSettingMethod(methodType, partnerId)).isEqualTo(hasPartnerMethod);
        verify(lmsClient).searchPartnerApiSettingsMethods(buildSettingsMethodFilter(methodType, partnerId));

        verifyNoMoreInteractions(lmsClient);
    }

    @Nonnull
    private static Stream<Arguments> hasPartnerSettingsMethodsArgs() {
        return Stream.of(
            Arguments.of(
                "Проверка наличия реализации метода у партнера, если метод реализован",
                List.of(SettingsMethodDto.newBuilder().active(true).build()),
                true
            ),
            Arguments.of(
                "Проверка наличия реализации метода у партнера, если не метод реализован",
                List.of(),
                false
            )
        );
    }

    @Nonnull
    private SearchPartnerFilter buildFilter(@Nonnull Set<PartnerType> partnerTypes) {
        return SearchPartnerFilter.builder()
            .setTypes(partnerTypes.stream().map(partnerTypeLmsConverter::toExternal).collect(Collectors.toSet()))
            .build();
    }

    @Nonnull
    private SettingsMethodFilter buildSettingsMethodFilter(String methodType, long partnerId) {
        return SettingsMethodFilter.newBuilder()
            .methodTypes(Set.of(methodType))
            .partnerIds(Set.of(partnerId))
            .build();
    }
}

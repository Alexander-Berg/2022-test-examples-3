package ru.yandex.market.logistics.lms.client.yt;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.lms.model.PartnerRelationLightModel;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@DisplayName("Получение связок партнеров")
class LmsLomYtClientGetPartnerRelationTest extends LmsLomYtAbstractTest {

    private static final String CUTOFFS_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/partner_relation_dyn] "
        + "WHERE partner_from = 1 AND partner_to = 2";

    private static final String PARTNER_RETURNS_QUERY = "* FROM [//home/2022-03-02T08:05:24Z/partner_relation_to_dyn] "
        + "WHERE partner_from IN (";

    private static final PartnerRelationFilter CUTOFFS_FILTER = buildFilter(Set.of(1L), Set.of(2L));
    private static final PartnerRelationFilter PARTNER_RETURNS_FILTER = buildFilter(Set.of(1L, 2L), null);

    @Test
    @DisplayName("Ищем связки партнеров с данными по катоффам, поход в YT выключен")
    void searchPartnerRelationsWithCutoffsGoToYtDisabled() {
        softly.assertThat(
                lmsLomYtClient.searchPartnerRelationWithCutoffs(CUTOFFS_FILTER)
            )
            .isEmpty();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Ищем связки партнеров с данными по катоффам, некорректный фильтр")
    @DatabaseSetup("/lms/client/yt/get_partner_relation_from_yt_enabled.xml")
    void searchPartnerRelationsWithCutoffsInvalidFilter(
        @SuppressWarnings("unused") String displayName,
        PartnerRelationFilter filter
    ) {
        softly.assertThatThrownBy(() -> lmsLomYtClient.searchPartnerRelationWithCutoffs(filter))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Filter must contain exactly one 'fromPartnerId' and 'toPartnerId'");
    }

    @Test
    @DisplayName("Ищем связки партнеров с данными по катоффам, данные не найдены")
    @DatabaseSetup("/lms/client/yt/get_partner_relation_from_yt_enabled.xml")
    void searchPartnerRelationsWithCutoffsNoData() {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            List.of(),
            CUTOFFS_QUERY
        );

        softly.assertThat(lmsLomYtClient.searchPartnerRelationWithCutoffs(CUTOFFS_FILTER))
            .isEmpty();

        verifyYtCalling(CUTOFFS_QUERY);
    }

    @Test
    @DisplayName("Ищем связки партнеров с данными по катоффам, данные найдены")
    @DatabaseSetup("/lms/client/yt/get_partner_relation_from_yt_enabled.xml")
    void searchPartnerRelationsWithCutoffs() {
        List<PartnerRelationLightModel> expected = getLightModelList();
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            expected,
            CUTOFFS_QUERY
        );

        softly.assertThat(lmsLomYtClient.searchPartnerRelationWithCutoffs(CUTOFFS_FILTER))
            .containsExactlyInAnyOrderElementsOf(expected);

        verifyYtCalling(CUTOFFS_QUERY);
    }

    @Test
    @DisplayName("Ищем связки партнеров с данными по возвратным партнерам, поход в YT выключен")
    void searchPartnerRelationsWithReturnPartnersGoToYtDisabled() {
        softly.assertThat(
                lmsLomYtClient.searchPartnerRelationsWithReturnPartners(CUTOFFS_FILTER)
            )
            .isEmpty();
    }

    @Test
    @DisplayName("Ищем связки партнеров с данными по возвратным партнерам, некорректный фильтр")
    @DatabaseSetup("/lms/client/yt/get_partner_relation_from_yt_enabled.xml")
    void searchPartnerRelationsWithReturnPartnersInvalidFilter() {
        softly.assertThatThrownBy(
                () -> lmsLomYtClient.searchPartnerRelationsWithReturnPartners(
                    buildFilter(null, null)
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Field 'fromPartnerIds' in filter must not be null");
    }

    @Test
    @DisplayName("Ищем связки партнеров с данными по возвратным партнерам, пустой фильтр")
    @DatabaseSetup("/lms/client/yt/get_partner_relation_from_yt_enabled.xml")
    void searchPartnerRelationsWithReturnPartnersEmptyFilter() {
        softly.assertThat(
                lmsLomYtClient.searchPartnerRelationsWithReturnPartners(
                    buildFilter(Set.of(), null)
                )
            )
            .isEmpty();
    }

    @Test
    @DisplayName("Ищем связки партнеров с данными по возвратным партнерам, данные не найдены")
    @DatabaseSetup("/lms/client/yt/get_partner_relation_from_yt_enabled.xml")
    void searchPartnerRelationsWithReturnPartnersNoData() {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            List.of(),
            PARTNER_RETURNS_QUERY
        );

        softly.assertThat(
                lmsLomYtClient.searchPartnerRelationsWithReturnPartners(PARTNER_RETURNS_FILTER)
            )
            .isEmpty();

        verifyYtCalling(PARTNER_RETURNS_QUERY);
    }

    @Test
    @DisplayName("Ищем связки партнеров с данными по возвратным партнерам, данные найдены")
    @DatabaseSetup("/lms/client/yt/get_partner_relation_from_yt_enabled.xml")
    void searchPartnerRelationsWithReturnPartners() {
        List<PartnerRelationLightModel> expected = getLightModelList();
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            expected,
            PARTNER_RETURNS_QUERY
        );

        softly.assertThat(
                lmsLomYtClient.searchPartnerRelationsWithReturnPartners(PARTNER_RETURNS_FILTER)
            )
            .containsExactlyInAnyOrderElementsOf(expected);

        verifyYtCalling(PARTNER_RETURNS_QUERY);
    }

    @NotNull
    private List<PartnerRelationLightModel> getLightModelList() {
        return List.of(
            PartnerRelationLightModel.build(PartnerRelationLightModel.newBuilder().fromPartnerId(1L).build()),
            PartnerRelationLightModel.build(PartnerRelationLightModel.newBuilder().fromPartnerId(2L).build())
        );
    }

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> searchPartnerRelationsWithCutoffsInvalidFilter() {
        return Stream.of(
            Arguments.of("fromPartnerId и toPartnerId не заданы", buildFilter(null, null)),
            Arguments.of("fromPartnerId пустой", buildFilter(null, Set.of(1L))),
            Arguments.of("toPartnerId пустой", buildFilter(Set.of(1L), null)),
            Arguments.of("размер fromPartnerId не равен 1", buildFilter(Set.of(1L, 2L), Set.of(1L))),
            Arguments.of("размер toPartnerId не равен 1", buildFilter(Set.of(1L), Set.of(1L, 2L)))
        );
    }

    @Nonnull
    private static PartnerRelationFilter buildFilter(@Nullable Set<Long> fromIds, @Nullable Set<Long> toIds) {
        return PartnerRelationFilter.newBuilder().fromPartnersIds(fromIds).toPartnersIds(toIds).build();
    }

    private void verifyYtCalling(String query) {
        verify(hahnYt, times(2)).tables();

        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractionsQueryStartsWith(
            ytTables,
            query
        );
    }

}

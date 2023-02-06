package ru.yandex.market.core.offer.content;

import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.offer.content.parameter.ParameterDefinition;
import ru.yandex.market.ir.http.PartnerContentApi;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Тесты для {@link PartnerContentConversions}
 */
@ParametersAreNonnullByDefault
class CategoryContentStructurePartnerContentConversionsTest {
    /**
     * Тест для {@link PartnerContentConversions#toCategoryContentStructure(List, List, List)}.
     */
    @Test
    void testToParameterDefinitionMinimumFields() {
        List<PartnerContentApi.Parameter> irModelParameters = Arrays.asList(
                PartnerContentApi.Parameter.newBuilder()
                        .setId(2357250)
                        .setName("Размер")
                        .setMandatory(false)
                        .setMultivalue(false)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.BOOL)
                                .build())
                        .build(),
                PartnerContentApi.Parameter.newBuilder()
                        .setId(3562825)
                        .setName("Цвет")
                        .setMandatory(false)
                        .setMultivalue(false)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.BOOL)
                                .build())
                        .build()
        );
        List<PartnerContentApi.Parameter> irSkuDefiningParameters = Arrays.asList(
                PartnerContentApi.Parameter.newBuilder()
                        .setId(2835628)
                        .setName("Название")
                        .setMandatory(false)
                        .setMultivalue(false)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.BOOL)
                                .build())
                        .build(),
                PartnerContentApi.Parameter.newBuilder()
                        .setId(3456739)
                        .setName("Вибрация")
                        .setMandatory(false)
                        .setMultivalue(false)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.BOOL)
                                .build())
                        .build()
        );
        List<PartnerContentApi.Parameter> irSkuInformationalParameters = Arrays.asList(
                PartnerContentApi.Parameter.newBuilder()
                        .setId(8935629)
                        .setName("Серия")
                        .setMandatory(false)
                        .setMultivalue(false)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.BOOL)
                                .build())
                        .build(),
                PartnerContentApi.Parameter.newBuilder()
                        .setId(5989834)
                        .setName("Год первого выпуска")
                        .setMandatory(false)
                        .setMultivalue(false)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.BOOL)
                                .build())
                        .build()
        );
        CategoryContentStructure categoryContentStructure = PartnerContentConversions.toCategoryContentStructure(
                irModelParameters,
                irSkuDefiningParameters,
                irSkuInformationalParameters
        );
        MatcherAssert.assertThat(
                categoryContentStructure,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                CategoryContentStructure::modelParameters,
                                Matchers.contains(Arrays.asList(
                                        MbiMatchers.transformedBy(ParameterDefinition::name, Matchers.is("Размер")),
                                        MbiMatchers.transformedBy(ParameterDefinition::name, Matchers.is("Цвет"))
                                ))
                        ),
                        MbiMatchers.transformedBy(
                                CategoryContentStructure::skuDefiningParameters,
                                Matchers.contains(Arrays.asList(
                                        MbiMatchers.transformedBy(ParameterDefinition::name, Matchers.is("Название")),
                                        MbiMatchers.transformedBy(ParameterDefinition::name, Matchers.is("Вибрация"))
                                ))
                        ),
                        MbiMatchers.transformedBy(
                                CategoryContentStructure::skuInformationalParameters,
                                Matchers.contains(Arrays.asList(
                                        MbiMatchers.transformedBy(ParameterDefinition::name, Matchers.is("Серия")),
                                        MbiMatchers.transformedBy(ParameterDefinition::name, Matchers.is("Год первого выпуска"))
                                ))
                        )
                )
        );
    }
}

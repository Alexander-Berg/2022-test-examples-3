package ru.yandex.market.core.offer.content;

import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.offer.content.parameter.BooleanCategoryParameterType;
import ru.yandex.market.core.offer.content.parameter.ParameterDefinition;
import ru.yandex.market.core.offer.content.parameter.ParameterDefinition.OccurrenceBounds.MaxOccurs;
import ru.yandex.market.core.offer.content.parameter.ParameterDefinition.OccurrenceBounds.MinOccurs;
import ru.yandex.market.ir.http.PartnerContentApi;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Тесты для {@link PartnerContentConversions}
 */
@ParametersAreNonnullByDefault
class ParameterDefinitionPartnerContentConversionsTest {
    /**
     * Тест для {@link PartnerContentConversions#toParameterDefinition(PartnerContentApi.Parameter)}.
     */
    @Test
    void testToParameterDefinitionMinimumFields() {
        PartnerContentApi.Parameter irParameter =
                PartnerContentApi.Parameter.newBuilder()
                        .setId(2357250)
                        .setName("Размер")
                        .setMandatory(false)
                        .setMultivalue(false)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.BOOL)
                                .build())
                        .build();
        ParameterDefinition parameterDefinition = PartnerContentConversions.toParameterDefinition(irParameter);
        MatcherAssert.assertThat(
                parameterDefinition,
                Matchers.allOf(
                        MbiMatchers.transformedBy(ParameterDefinition::id, Matchers.is(2357250L)),
                        MbiMatchers.transformedBy(ParameterDefinition::name, Matchers.is("Размер")),
                        MbiMatchers.transformedBy(ParameterDefinition::description, Matchers.is(Optional.empty())),
                        MbiMatchers.transformedBy(ParameterDefinition::minOccurs, Matchers.is(MinOccurs.ZERO)),
                        MbiMatchers.transformedBy(ParameterDefinition::maxOccurs, Matchers.is(MaxOccurs.ONE)),
                        MbiMatchers.transformedBy(ParameterDefinition::type, Matchers.instanceOf(BooleanCategoryParameterType.class))
                )
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toParameterDefinition(PartnerContentApi.Parameter)}.
     */
    @Test
    void testToParameterDefinitionMaximimFields() {
        PartnerContentApi.Parameter irParameter =
                PartnerContentApi.Parameter.newBuilder()
                        .setId(39485893)
                        .setName("Комплектация")
                        .setDescription("Cписок предметов в коробке")
                        .setMandatory(true)
                        .setMultivalue(true)
                        .setParameterDomain(PartnerContentApi.ParameterDomain.newBuilder()
                                .setType(PartnerContentApi.ParameterType.BOOL)
                                .build())
                        .build();
        ParameterDefinition parameterDefinition = PartnerContentConversions.toParameterDefinition(irParameter);
        MatcherAssert.assertThat(
                parameterDefinition,
                Matchers.allOf(
                        MbiMatchers.transformedBy(ParameterDefinition::id, Matchers.is(39485893L)),
                        MbiMatchers.transformedBy(ParameterDefinition::name, Matchers.is("Комплектация")),
                        MbiMatchers.transformedBy(ParameterDefinition::description, MbiMatchers.isPresent("Cписок предметов в коробке")),
                        MbiMatchers.transformedBy(ParameterDefinition::minOccurs, Matchers.is(MinOccurs.ONE)),
                        MbiMatchers.transformedBy(ParameterDefinition::maxOccurs, Matchers.is(MaxOccurs.UNBOUNDED)),
                        MbiMatchers.transformedBy(ParameterDefinition::type, Matchers.instanceOf(BooleanCategoryParameterType.class))
                )
        );
    }

}

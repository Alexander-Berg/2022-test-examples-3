package ru.yandex.market.gutgin.tms.matching.alias;

import org.junit.Test;
import ru.yandex.market.gutgin.tms.matching.CategoryModelMatcher;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.entity.Model;
import ru.yandex.market.partner.content.common.entity.ParameterType;
import ru.yandex.market.partner.content.common.entity.ParameterValue;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class AliasMatcherTest {

    @Test
    public void testClosestMatches() {
        checkMatches(
            of("iphone", "Iphone", "IPhone", "Айфон"),
            of(
                vendorParamExpected(1),
                paramExpected(1)
                    .setType(ParameterType.ENUM)
                    .setOptionId(6),
                paramExpected(2)
                    .setType(ParameterType.NUMERIC)
                    .setNumericValue(64.)
            ),
            2L,
            model(
                1,
                1,
                of("Iphone"),
                param(1, 7),
                param(3, true)
            ),
            model(
                2,
                1,
                of("IPhone"),
                param(1, 6),
                param(2, 64.),
                param(4, "none") // strings don't count
            ),
            model(
                3,
                1,
                of("Samsung"),
                param(1, 5),
                param(2, 128.),
                param(3, false),
                param(5, "some")
            ),
            model(
                4,
                1,
                of("iphone", "IPhone"),
                param(1, 6),
                param(2, 64.),
                param(3, 1.)
            )
        );
    }

    @Test
    public void testWithStringAmbiguous() {
        checkNoneMatches(
            of("iphone", "Iphone", "IPhone", "Айфон"),
            of(
                vendorParamExpected(1),
                paramExpected(1)
                    .setType(ParameterType.ENUM)
                    .setOptionId(6),
                paramExpected(2)
                    .setType(ParameterType.NUMERIC)
                    .setNumericValue(64.)
            ),
            model(
                1,
                1,
                of("Iphone"),
                param(1, 7),
                param(3, true)
            ),
            model(
                2,
                1,
                of("IPhone"),
                param(1, 6),
                param(2, 64.),
                param(3, false),
                param(4, "none")
            ),
            model(
                3,
                1,
                of("Samsung"),
                param(1, 5),
                param(2, 128.),
                param(3, false),
                param(5, "some")
            ),
            model(
                4,
                1,
                of("iphone", "IPhone"),
                param(1, 6),
                param(2, 64.),
                param(3, 1.)
            )
        );
    }

    @Test
    public void testStringIgnoredAndMatches() {
        checkMatches(
            of("iphone"),
            of(
                vendorParamExpected(1),
                paramExpected(1)
                    .setType(ParameterType.ENUM)
                    .setOptionId(7),
                paramExpected(2)
                    .setType(ParameterType.NUMERIC)
                    .setNumericValue(42.)
            ),
            2L,
            model(
                1,
                1,
                of("iPhone"),
                param(1, 7),
                param(3, true)
            ),
            model(
                2,
                1,
                of("IPHONE"),
                param(1, 7),
                param(2, 42.),
                param(4, "none")
            ),
            model(
                3,
                1,
                of("IPHONE"),
                param(1, 7),
                param(2, 42.),
                param(4, 8)
            ),
            model(
                4,
                1,
                of("iphone"),
                param(1, 7),
                param(2, 42.),
                param(3, false)
            )
        );
    }

    @Test
    public void testIfTwoClosestAmbiguousNoneMatches() {
        checkNoneMatches(
            of("iphone"),
            of(
                vendorParamExpected(1),
                paramExpected(1)
                    .setType(ParameterType.ENUM)
                    .setOptionId(7),
                paramExpected(2)
                    .setType(ParameterType.NUMERIC)
                    .setNumericValue(42.)
            ),
            model(
                1,
                1,
                of("iPhone"),
                param(1, 7),
                param(3, true)
            ),
            model(
                2,
                1,
                of("IPHONE"),
                param(1, 7),
                param(2, 42.),
                param(4, 8)
            ),
            model(
                3,
                1,
                of("iphone"),
                param(1, 7),
                param(2, 42.),
                param(3, false)
            )
        );
    }

    @Test
    public void testNoneMatches() {
        checkNoneMatches(
            of("iphone"),
            of(
                vendorParamExpected(1),
                paramExpected(1)
                    .setType(ParameterType.ENUM)
                    .setOptionId(7),
                paramExpected(2)
                    .setType(ParameterType.NUMERIC)
                    .setNumericValue(42.)
            ),
            model(
                1,
                1,
                of("iphone"),
                param(2, 42.),
                param(3, true)
            ),
            model(
                2,
                1,
                of("iphone"),
                param(1, 7),
                param(4, "none")
            ),
            model(
                3,
                1,
                of("Zhiguli"),
                param(1, 7),
                param(2, 42.)
            ),
            model(
                4,
                1,
                of("Iphone"),
                param(1, 8),
                param(2, 42.)
            ),
            model(
                5,
                1,
                of("iPhone"),
                param(1, 7),
                param(2, 28.)
            )
        );
    }

    @Test
    public void testMultiValueMatchesClosest() {
        checkMatches(
            of("iphone"),
            of(
                vendorParamExpected(1),
                paramExpected(1)
                    .setType(ParameterType.ENUM)
                    .setOptionId(7),
                paramExpected(2)
                    .setType(ParameterType.NUMERIC)
                    .setNumericValue(42.),
                paramExpected(3)
                    .setType(ParameterType.ENUM)
                    .setOptionId(1)
            ),
            5L,
            model(
                1,
                1,
                of("iphone"),
                param(1, 7),
                param(2, 42.),
                param(3, 2)
            ),
            model(
                4,
                1,
                of("Iphone"),
                param(1, 7),
                param(2, 42.),
                param(3, 1),
                param(3, 2)
            ),
            model(
                5,
                1,
                of("iPhone"),
                param(1, 7),
                param(2, 42.),
                param(3, 1)
            )
        );
    }

    @Test
    public void testSimplyMatches() {
        checkMatches(
            of("iphone"),
            of(
                vendorParamExpected(1),
                paramExpected(1)
                    .setType(ParameterType.ENUM)
                    .setOptionId(7),
                paramExpected(2)
                    .setType(ParameterType.NUMERIC)
                    .setNumericValue(42.)
            ),
            5L,
            model(
                1,
                1,
                of("iphone"),
                param(2, 42.),
                param(3, true)
            ),
            model(
                2,
                1,
                of("iphone"),
                param(1, 7),
                param(4, "none")
            ),
            model(
                3,
                1,
                of("Zhiguli"),
                param(1, 7),
                param(2, 42.)
            ),
            model(
                4,
                1,
                of("Iphone"),
                param(1, 8),
                param(2, 42.)
            ),
            model(
                5,
                1,
                of("IPHONE"),
                param(1, 7),
                param(2, 42.),
                param(3, true)
            )
        );
    }

    @Test
    public void testSimplyMatchesByAlias() {
        checkMatchesByAlias(
            of("iphone"),
            1,
            1L,
            model(
                1,
                1,
                of("iphone")
            ),
            model(
                2,
                1,
                of("Samsung")
            ),
            model(
                3,
                1,
                of("Zhiguli")
            ),
            model(
                5,
                1,
                of("Nokia")
            )
        );
    }

    @Test
    public void testMatchesIphoneDeterministic() {
        checkMatchesByAlias(
            of("iphone", "Iphone", "IPhone", "Айфон"),
            1,
            1L,
            model(
                1,
                1,
                of("iphone")
            ),
            model(
                2,
                1,
                of("IPhone")
            ),
            model(
                3,
                1,
                of("Айфон")
            ),
            model(
                4,
                1,
                of("Xiaomi")
            ),
            model(
                5,
                1,
                of("IPHONE")
            )
        );
    }

    @Test
    public void testMatchesWithTabsAndSpaces() {
        checkMatchesByAlias(
            of("Name with tab and double whitespace"),
            1,
            1L,
            model(1, 1,
                of("Name with tab\tand double  whitespace")
            )
        );
    }

    @Test
    public void testMatchesWithTabsAndSpacesViceVersa() {
        checkMatchesByAlias(
            of("Name with tab\tand double  whitespace"),
            1,
            1L,
            model(1, 1,
                of("Name with tab and double whitespace")
            )
        );
    }

    @Test
    public void testNoneMatchesByAlias() {
        checkNoneMatchesByAlias(
            of("Samsung"),
            1,
            model(
                1,
                1,
                of("iphone")
            ),
            model(
                2,
                1,
                of("IPhone")
            ),
            model(
                3,
                1,
                of("Айфон")
            ),
            model(
                4,
                1,
                of("Xiaomi")
            ),
            model(
                5,
                1,
                of("IPHONE")
            )
        );
    }

    @Test
    public void testSimplyMatchesByAliasDiffVendors() {
        checkMatchesByAlias(
            of("iphone"),
            2,
            2L,
            model(
                1,
                1,
                of("iphone")
            ),
            model(
                2,
                2,
                of("iphone")
            ),
            model(
                3,
                3,
                of("iphone")
            ),
            model(
                5,
                5,
                of("iphone")
            )
        );
    }

    @Test
    public void testSimplyNoMatchesByAliasDiffVendors() {
        checkNoneMatchesByAlias(
            of("iphone"),
            1,
            model(
                1,
                2,
                of("iphone")
            ),
            model(
                2,
                3,
                of("iphone")
            ),
            model(
                3,
                4,
                of("iphone")
            ),
            model(
                5,
                5,
                of("iphone")
            )
        );
    }

    private ParameterValue paramExpected(long id) {
        return new ParameterValue().setParamId(id);
    }


    private ParameterValue vendorParamExpected(int optionId) {
        return paramExpected(ParameterValueComposer.VENDOR_ID)
            .setType(ParameterType.ENUM)
            .setOptionId(optionId);
    }

    private ModelStorage.ParameterValue param(long id, String value) {
        return param(id, p -> p.setValueType(MboParameters.ValueType.STRING)
            .addStrValue(str().setValue(value).build()));
    }

    private ModelStorage.ParameterValue param(long id, boolean value) {
        return param(id, p -> p.setValueType(MboParameters.ValueType.BOOLEAN)
            .setBoolValue(value).build());
    }

    private ModelStorage.ParameterValue param(long id, double value) {
        return param(id, p -> p.setValueType(MboParameters.ValueType.NUMERIC)
            .setNumericValue(value + "").build());
    }

    private ModelStorage.ParameterValue param(long id, int optionId) {
        return param(id, p -> p.setValueType(MboParameters.ValueType.ENUM)
            .setOptionId(optionId).build());
    }

    private ModelStorage.ParameterValue param(long id, Consumer<ModelStorage.ParameterValue.Builder> setParams) {
        ModelStorage.ParameterValue.Builder builder = ModelStorage.ParameterValue.newBuilder()
            .setParamId(id);
        setParams.accept(builder);
        return builder.build();
    }

    private ModelStorage.Model model(long id, int vendorId, List<String> aliases, ModelStorage.ParameterValue... parameters) {

        return ModelStorage.Model.newBuilder()
            .setId(id)
            .setVendorId(vendorId)
            .addAllAliases(aliases.stream()
                .map(value -> str().setValue(value).build())
                .collect(Collectors.toList()))
            .addAllParameterValues(
                Stream
                    .concat(
                        Stream.of(parameters),
                        Stream.of(param(ParameterValueComposer.VENDOR_ID, vendorId))
                    )
                    .collect(Collectors.toList())
            )
            .build();
    }

    private ModelStorage.LocalizedString.Builder str() {
        return ModelStorage.LocalizedString.newBuilder().setIsoCode("ru");
    }

    private void checkNoneMatches(List<String> aliases, List<ParameterValue> parameterList,
                                  ModelStorage.Model... modelsToMatch) {
        checkMatches(aliases, parameterList, null, modelsToMatch);
    }

    private void checkMatches(List<String> aliases, List<ParameterValue> parameterList,
                              Long expectedModelId, ModelStorage.Model... modelsToMatch) {
        checkSetup(aliases, parameterList, expectedModelId, (m1, m2) -> m1, modelsToMatch);
    }

    private void checkMatchesByAlias(List<String> aliases, int vendorId,
                                     Long expectedModelId, ModelStorage.Model... modelsToMatch) {
        checkSetup(aliases, of(vendorParamExpected(vendorId)), expectedModelId, (m1, m2) -> m2, modelsToMatch);
    }

    private void checkNoneMatchesByAlias(List<String> aliases, int vendorId, ModelStorage.Model... modelsToMatch) {
        checkSetup(aliases, of(vendorParamExpected(vendorId)),  null, (m1, m2) -> m2, modelsToMatch);
    }

    private void checkSetup(List<String> aliases, List<ParameterValue> parameterList, Long expectedModelId,
                            BiFunction<AliasAndParametersMatcher, FindFirstByAliasMatcher, CategoryModelMatcher> matcherSelector,
                            ModelStorage.Model... modelsToMatch) {
        //given
        Model model = new Model();
        model.setName(aliases.get(0));
        model.setParameterList(parameterList);
        model.setAliases(aliases.subList(1, aliases.size()));

        ModelStorageHelper storageHelper = mock(ModelStorageHelper.class);
        doAnswer(inv -> modelById(inv.getArgument(1), modelsToMatch))
            .when(storageHelper)
            .getModel(anyLong(), anyLong());

        AliasMatchersBuilder builder = AliasMatchersBuilder.of(storageHelper,
            (cat, m1, m2, cdh) -> matcherSelector.apply(m1, m2),
            null);
        Stream.of(modelsToMatch).forEach(builder::addModel);

        //when
        CategoryModelMatcher matcher = builder.build();
        Optional<Long> match = matcher.match(model);

        //then
        if (expectedModelId != null) {
            assertThat(match).isNotEmpty().contains(expectedModelId);
        } else {
            assertThat(match).isEmpty();
        }
    }

    private ModelStorage.Model modelById(Long id, ModelStorage.Model[] modelsToMatch) {
        return Stream.of(modelsToMatch)
            .filter(m -> m.getId() == id)
            .findAny()
            .orElse(null);
    }

}
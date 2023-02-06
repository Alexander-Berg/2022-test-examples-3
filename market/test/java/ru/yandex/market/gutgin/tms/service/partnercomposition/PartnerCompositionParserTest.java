package ru.yandex.market.gutgin.tms.service.partnercomposition;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.content.common.csku.KnownTags;

import static org.mockito.Mockito.when;
import static ru.yandex.market.gutgin.tms.service.partnercomposition.PartnerCompositionParseResult.ErrorType;
import static ru.yandex.market.gutgin.tms.service.partnercomposition.PartnerCompositionParseResult.ErrorType.DUPLICATE_PARAMETER;
import static ru.yandex.market.gutgin.tms.service.partnercomposition.PartnerCompositionParseResult.ErrorType.INVALID_FORMAT;
import static ru.yandex.market.gutgin.tms.service.partnercomposition.PartnerCompositionParseResult.ErrorType.INVALID_SUM;
import static ru.yandex.market.gutgin.tms.service.partnercomposition.PartnerCompositionParseResult.ErrorType.UNKNOWN_PARAMETER;

@RunWith(Parameterized.class)
public class PartnerCompositionParserTest {

    private static final CategoryDataKnowledge categoryDataKnowledge = Mockito.mock(CategoryDataKnowledge.class);
    private static final PartnerCompositionParser parser = new PartnerCompositionParser(categoryDataKnowledge);
    private static final long CATEGORY_ID = 1L;

    @BeforeClass
    public static void init() {
        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);
        when(categoryDataMock.getParamIdsByTag(KnownTags.MATERIAL.getName()))
                .thenReturn(new LongArraySet(List.of(1L, 2L, 3L, 4L, 5L, 6L)));
        when(categoryDataMock.getParamById(1L))
                .thenReturn(createMaterialParam(1L, "Метанит", Set.of("люрекс", "алюнит", "Металлическая нить")));
        when(categoryDataMock.getParamById(2L))
                .thenReturn(createMaterialParam(2L, "Натуральная кожа", Set.of("кожа натуральная", "кожа")));
        when(categoryDataMock.getParamById(3L))
                .thenReturn(createMaterialParam(3L, "Искусственная кожа", Set.of("кож-зам", "кожа искусственная")));
        when(categoryDataMock.getParamById(4L))
                .thenReturn(createMaterialParam(4L, "Хлопок", Set.of("Х/б", "хлопок", "cottone")));
        when(categoryDataMock.getParamById(5L))
                .thenReturn(createMaterialParam(5L, "Материал:хлопок", Set.of()));
        when(categoryDataMock.getParamById(6L))
                .thenReturn(createMaterialParam(6L, "Спандекс", Set.of("спандекс (обкрученная высокоэластичная " +
                        "нить)")));

        when(categoryDataKnowledge.getCategoryData(CATEGORY_ID))
                .thenReturn(categoryDataMock);
    }

    private static MboParameters.Parameter createMaterialParam(long id, String nameForPartner, Set<String> aliases) {
        var builder = MboParameters.Parameter.newBuilder()
                .setId(id)
                .setXslName("param" + id)
                .setNameForPartnerNew(nameForPartner)
                .addTags(KnownTags.MATERIAL.getName());
        for (String alias : aliases) {
            builder.addAlias(MboParameters.Word.newBuilder().setName(alias).build());
        }

        return builder.build();
    }

    @Parameterized.Parameter(0)
    public String strToCheck;

    @Parameterized.Parameter(1)
    public Map<ErrorType, Set<String>> errors;

    @Parameterized.Parameter(2)
    public Map<Long, Short> materials;

    @Parameterized.Parameters(name = "{index}: \"{0}\" --> errors={1}, parsed = {2}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"метанит:100%", null, Map.of(1L, (short) 100)},
                {"метанит:100", null, Map.of(1L, (short) 100)},
                {"метанит:100;;;", null, Map.of(1L, (short) 100)},
                {"метанит:100,;,", null, Map.of(1L, (short) 100)},
                {"кожа 100% искусственная", null, Map.of(2L, (short) 100)},
                {"кожа искусственная 100%", null, Map.of(3L, (short) 100)},
                {"хлопок 100 кожа искусственная", null, Map.of(3L, (short) 100)},
                {"кож-зам-100%", null, Map.of(3L, (short) 100)},
                {"х/б волокно 100", null, Map.of(4L, (short) 100)},
                {"кожа-зам[80];х/б(10); алюнит {10}", null, Map.of(2L, (short) 80, 4L, (short) 10, 1L, (short) 10)},
                {"хлопок - 80 ; х/Б : 10", Map.of(DUPLICATE_PARAMETER, Set.of("хлопок - 80", "х/Б : 10"), INVALID_SUM
                        , Set.of()), Map.of(4L, (short) 80)},
                {"100", Map.of(INVALID_FORMAT, Set.of("100")), Map.of()},
                {"х/б", Map.of(INVALID_FORMAT, Set.of("х/б")), Map.of()},
                {"100 кожа искусственная 100", Map.of(INVALID_FORMAT, Set.of("100 кожа искусственная 100")), Map.of()},
                {"80 ; х/Б : 10", Map.of(INVALID_FORMAT, Set.of("80"), INVALID_SUM, Set.of()), Map.of(4L, (short) 10)},
                {"х/ б", Map.of(INVALID_FORMAT, Set.of("х/ б")), Map.of()},
                {"синтетика", Map.of(INVALID_FORMAT, Set.of("синтетика")), Map.of()},
                {"х/бб:100", Map.of(UNKNOWN_PARAMETER, Set.of("х/бб:100")), Map.of()},

                // реальные случаи с разделителем в названии материала
                {"материал:хлопок:100", null, Map.of(5L, (short) 100)},
                {"спандекс (обкрученная высокоэластичная нить)(100)", null, Map.of(6L, (short) 100)}
        });
    }

    @Test
    public void whenParseStringThenOk() {
        var result = parser.parse(CATEGORY_ID, strToCheck);

        if (errors == null) {
            Assert.assertTrue(result.getErrors().isEmpty());
        } else {
            var parsedErrors = result.getErrors();
            Assert.assertEquals("checking errors quantity", errors.size(), parsedErrors.size());
            for (ErrorType errorType : errors.keySet()) {
                Assert.assertTrue("not found expected error " + errorType, parsedErrors.containsKey(errorType));
                Assert.assertEquals("checking failed materials for " + errorType, errors.get(errorType),
                        parsedErrors.get(errorType));
            }
        }

        Set<Long> expectedMaterials = new HashSet<>(materials.keySet());
        for (PartnerCompositionMaterial material : result.getMaterials()) {
            Short expectedNum = materials.get(material.getParamId());
            if (expectedNum == null) {
                Assert.fail("group [" + material.getParamId() + "] is unexpected");
            }
            Assert.assertEquals("validate num by group [" + material.getParamId() + "]", expectedNum,
                    material.getPercent());
            expectedMaterials.remove(material.getParamId());
        }

        if (expectedMaterials.size() > 0) {
            Assert.fail("not found expected groups " + expectedMaterials);
        }
    }

}

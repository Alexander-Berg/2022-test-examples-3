package ru.yandex.direct.web.entity.bidmodifier.converter;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.dbschema.ppc.enums.DemographyMultiplierValuesAge;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDemographicsAdjustment;
import ru.yandex.direct.web.entity.bidmodifier.model.WebDemographicsBidModifier;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static ru.yandex.direct.web.entity.bidmodifier.converter.DemographicsBidModifierConverter.webDemographicsBidModifierToCore;

@RunWith(Parameterized.class)
public class DemographicsBidModifierConverterAgeTest {

    private static Set<DemographyMultiplierValuesAge> exclusions = ImmutableSet.of(DemographyMultiplierValuesAge._45_,
            DemographyMultiplierValuesAge.unknown);

    @Parameterized.Parameter
    public DemographyMultiplierValuesAge databaseAge;

    @Parameterized.Parameters(name = "database age = {0}")
    public static List<Object[]> databaseAges() {
        return StreamEx.of(DemographyMultiplierValuesAge.values())
                .remove(exclusions::contains)
                .map(dbAgeEnum -> new Object[]{dbAgeEnum})
                .toList();
    }

    @Test
    public void eachKnownDatabaseAgeIsCorrectlyConvertedToEnum() {
        WebDemographicsBidModifier webModifier = singleDemographicsBidModifier(databaseAge.getLiteral(), 123);

        String message = String.format("Поддерживаемый в базе тип возраста %s не поддерживается конвертером в web. "
                        + "Нужно либо поддержать его в конвертере, либо добавить в исключения в данном тесте - exclusions",
                databaseAge.getLiteral());

        BidModifierDemographics coreModifier = null;
        try {
            coreModifier = webDemographicsBidModifierToCore(webModifier);
        } catch (IllegalStateException e) {
            fail(message);
        }

        AgeType expectedCoreAge = AgeType.fromSource(databaseAge);
        assertThat(message,
                coreModifier.getDemographicsAdjustments().get(0).getAge(),
                is(expectedCoreAge));
    }

    private static WebDemographicsBidModifier singleDemographicsBidModifier(String age, Integer percent) {
        return new WebDemographicsBidModifier()
                .withEnabled(1)
                .withAdjustments(singletonList(demographicsAdjustment(age, percent)));
    }

    private static WebDemographicsAdjustment demographicsAdjustment(String age, Integer percent) {
        return (WebDemographicsAdjustment) new WebDemographicsAdjustment()
                .withAge(age)
                .withPercent(percent);
    }
}

package ru.yandex.direct.api.v5.units.logging;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.direct.api.v5.context.units.UnitsBucket;
import ru.yandex.direct.api.v5.context.units.UnitsLogData;
import ru.yandex.direct.core.units.api.UnitsBalance;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class UnitsBucketFactoryTest {

    private UnitsBucketFactory factory = new UnitsBucketFactory();

    @Parameter()
    public String description;

    @Parameter(1)
    public UnitsLogData unitsLogData;

    @Parameter(2)
    public UnitsBalance unitsBalance;

    @Parameter(3)
    public UnitsBalance operatorUnitsBalance;

    @Parameter(4)
    public UnitsBucket expectedUnitsHolderBucket;

    @Parameter(5)
    public UnitsBucket expectedOperatorBucket;

    @Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
        return UnitsBucketFactoryTestData.provideData();
    }

    @Test
    public void shouldProperlyBuildUnitsHolderBucket() {
        UnitsBucket actual = factory.createUnitsHolderBucket(unitsLogData, unitsBalance);

        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expectedUnitsHolderBucket);
    }

    @Test
    public void shouldProperlyBuildOperatorBucket() {
        UnitsBucket actual = factory.createOperatorBucket(unitsLogData, operatorUnitsBalance);

        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expectedOperatorBucket);
    }

    @Test
    public void shouldSetAllOtherBucketFields() {
        UnitsBucket bucket = factory.createUnitsHolderBucket(unitsLogData, unitsBalance);

        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(bucket.getBucketClientId()).isNotNull();
        sa.assertThat(bucket.getBucketUnitsBalance()).isNotNull();
        sa.assertThat(bucket.getBucketUnitsLimit()).isNotNull();
        sa.assertThat(bucket.getBucketUnitsSpent()).isNotNull();
        sa.assertAll();
    }

}

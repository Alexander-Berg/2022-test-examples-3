package ru.yandex.direct.api.v5.units.logging;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.units.UnitsBucket;
import ru.yandex.direct.api.v5.context.units.UnitsContext;
import ru.yandex.direct.api.v5.context.units.UnitsLogData;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.units.api.UnitsBalance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@ParametersAreNonnullByDefault
public class UnitsLogDataFactoryAddBucketTest {

    private static final String UNITS_USED_LOGIN = "uul";
    private static final String OPERATOR_LOGIN = "op";

    @Mock
    private UnitsBucketFactory unitsBucketFactory;

    @InjectMocks
    private UnitsLogDataFactory unitsLogDataFactory;

    @Mock
    private UnitsContext unitsContext;

    @Mock
    private ApiUser unitsUsedUser;

    @Mock
    private ApiUser operator;

    @Mock
    private UnitsBalance unitsBalance;

    @Mock
    private UnitsBalance operatorUnitsBalance;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private UnitsLogData unitsLogData;

    @Mock
    private UnitsBucket unitsBucket;

    @Before
    public void init() {
        initMocks(this);

        when(unitsContext.getUnitsLogData()).thenReturn(unitsLogData);
        when(unitsContext.getUnitsBalance()).thenReturn(unitsBalance);
        when(unitsContext.getOperatorUnitsBalance()).thenReturn(operatorUnitsBalance);
        when(unitsContext.getUnitsUsedUser()).thenReturn(unitsUsedUser);
        when(unitsContext.getOperator()).thenReturn(operator);
        when(unitsUsedUser.getLogin()).thenReturn(UNITS_USED_LOGIN);
        when(operator.getLogin()).thenReturn(OPERATOR_LOGIN);

        when(unitsBucketFactory.createUnitsHolderBucket(same(unitsLogData), same(unitsBalance)))
                .thenReturn(unitsBucket);
        when(unitsBucketFactory.createOperatorBucket(same(unitsLogData), same(operatorUnitsBalance)))
                .thenReturn(unitsBucket);
    }

    @Test
    public void shouldSetBucket_addUnitsHolderBucket() {
        addUnitsHolderBucket();
        assertThat(unitsLogData.getBucket()).isSameAs(unitsBucket);
    }

    @Test
    public void shouldSetBucket_addOperatorBucket() {
        addOperatorBucket();
        assertThat(unitsLogData.getBucket()).isSameAs(unitsBucket);
    }

    @Test
    public void shouldSetUnitsUsedLogin_addUnitsHolderBucket() {
        addUnitsHolderBucket();
        assertThat(unitsLogData.getUnitsUsedLogin()).isEqualTo(UNITS_USED_LOGIN);
    }

    @Test
    public void shouldSetUnitsUsedLogin_addOperatorBucket() {
        addOperatorBucket();
        assertThat(unitsLogData.getUnitsUsedLogin()).isEqualTo(OPERATOR_LOGIN);
    }

    @Test
    public void shouldSetOperatorFailureOnAddOperatorBucket() {
        addOperatorBucket();
        assertThat(unitsLogData.getOperatorFailure()).isTrue();
    }

    @Test
    public void shouldNotSetOperatorFailureOnAddUnitsHolderBucket() {
        addUnitsHolderBucket();
        assertThat(unitsLogData.getOperatorFailure()).isFalse();
    }

    @Test
    public void shouldNotFailIfUnitsBalanceIsNull_addUnitsHolderBucket() {
        when(unitsContext.getUnitsBalance()).thenReturn(null);
        addUnitsHolderBucket();
    }

    @Test
    public void shouldNotFailIfUnitsBalanceIsNull_addOperatorBucket() {
        when(unitsContext.getOperatorUnitsBalance()).thenReturn(null);
        addOperatorBucket();
    }

    @Test
    public void shouldNotFailIfUnitsLogDataIsNull_addUnitsHolderBucket() {
        when(unitsContext.getUnitsLogData()).thenReturn(null);
        addUnitsHolderBucket();
    }

    @Test
    public void shouldNotFailIfUnitsLogDataIsNull_addOperatorBucket() {
        when(unitsContext.getUnitsLogData()).thenReturn(null);
        addOperatorBucket();
    }

    private void addUnitsHolderBucket() {
        unitsLogDataFactory.addUnitsHolderBucket(unitsContext);
    }

    private void addOperatorBucket() {
        unitsLogDataFactory.addOperatorBucket(unitsContext);
    }

}

package ru.yandex.direct.core.units.api;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.units.service.UnitsService;
import ru.yandex.direct.core.units.storage.Storage;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.units.service.UnitsService.DEFAULT_INTERVALS;
import static ru.yandex.direct.core.units.service.UnitsService.DEFAULT_INTERVAL_SIZE_SEC;
import static ru.yandex.direct.core.units.service.UnitsService.DEFAULT_LIMIT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

public class UnitsServiceTest {

    private static final long CLIENT_ID = 1L;
    private static final String CLIENT_LOGIN = "client-login";
    private UnitsService testedService;

    private Storage emptyStorageMock;
    private Storage fillLastIntervalStorageMock;
    private ApiUser holderMock;
    private Storage fillFirstIntervalStorageMock;

    @Before
    public void setup() {
        emptyStorageMock = mock(Storage.class);
        when(emptyStorageMock.getMulti(anyCollection())).thenReturn(Collections.emptyMap());

        fillLastIntervalStorageMock = mock(Storage.class);
        when(fillLastIntervalStorageMock.getMulti(anyCollection()))
                .thenAnswer(invocation -> {
                    Collection<String> arg = (Collection<String>) invocation.getArguments()[0];

                    List<String> keysSorted = arg.stream().sorted().collect(Collectors.toList());
                    String lastKey = keysSorted.get(keysSorted.size() - 1);
                    return keysSorted.stream().collect(Collectors.toMap(key -> key,
                            key -> Objects.equals(key, lastKey) ? (int) DEFAULT_LIMIT : 0));
                });

        fillFirstIntervalStorageMock = mock(Storage.class);
        when(fillFirstIntervalStorageMock.getMulti(anyCollection()))
                .thenAnswer((Answer<Map>) invocation -> {
                    Collection<String> arg = (Collection<String>) invocation.getArguments()[0];

                    List<String> keysSorted = arg.stream().sorted().collect(Collectors.toList());
                    String firstKey = keysSorted.get(0);
                    return keysSorted.stream().collect(Collectors.toMap(key -> key,
                            key -> Objects.equals(key, firstKey) ? DEFAULT_LIMIT : 0));
                });

        holderMock = mock(ApiUser.class);
        when(holderMock.getApiUnitsDaily()).thenReturn((long) DEFAULT_LIMIT);
        when(holderMock.getClientId()).thenReturn(ClientId.fromLong(CLIENT_ID));
        when(holderMock.getLogin()).thenReturn(CLIENT_LOGIN);

    }

    @Test
    public void getUnitsBalance_returnsCorrectUnitsBalanceWhenStorageIsEmpty() {
        testedService = new UnitsService(emptyStorageMock);
        UnitsBalance unitsBalance = testedService.getUnitsBalance(holderMock);
        assertThat(unitsBalance,
                beanDiffer(new UnitsBalanceImpl(CLIENT_ID, DEFAULT_LIMIT, DEFAULT_LIMIT, 0)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getUnitsBalance_callStorageOnlyOnce() {
        testedService = new UnitsService(emptyStorageMock);
        UnitsBalance unitsBalance = testedService.getUnitsBalance(holderMock);
        verify(emptyStorageMock, times(1)).getMulti(anyCollection());
        verifyNoMoreInteractions(emptyStorageMock);
    }

    @Test
    public void getUnitsBalance_returnsCorrectUnitsBalanceWhenStorageIsFilled() {
        testedService = new UnitsService(fillLastIntervalStorageMock);
        UnitsBalance unitsBalance = testedService.getUnitsBalance(holderMock);
        assertThat(unitsBalance,
                beanDiffer(new UnitsBalanceImpl(CLIENT_ID, DEFAULT_LIMIT, 0, DEFAULT_LIMIT)));
    }

    @Test
    public void getUnitsBalance_returnsCorrectUnitsBalanceWhenStorageIsHalfFilled() {
        testedService = new UnitsService(fillFirstIntervalStorageMock);
        UnitsBalance unitsBalance = testedService.getUnitsBalance(holderMock);

        // Столько на самом деле должно быть вычтено из баланса за списание DEFAULT_LIMIT в самом начале скользящего окна
        int intervalLimit = (int) Math.round(1. * DEFAULT_LIMIT / DEFAULT_INTERVALS);
        int expectedBalance = DEFAULT_LIMIT - intervalLimit;
        assertThat(unitsBalance,
                beanDiffer(new UnitsBalanceImpl(CLIENT_ID, DEFAULT_LIMIT, expectedBalance, DEFAULT_LIMIT)));
    }

    @Test
    public void updateSpent_callStorageOnlyOnce() {
        int ttl = DEFAULT_INTERVAL_SIZE_SEC * DEFAULT_INTERVALS;
        int spent = 100;

        UnitsBalanceImpl unitsBalance = new UnitsBalanceImpl(CLIENT_ID, DEFAULT_LIMIT, DEFAULT_LIMIT, 0);
        unitsBalance.withdraw(spent);
        assumeThat(unitsBalance.spentInCurrentRequest(), is(spent));

        testedService = new UnitsService(emptyStorageMock);

        testedService.updateSpent(unitsBalance);
        verify(emptyStorageMock, times(1)).incrOrSet(any(), eq(spent), eq(ttl));
        verifyNoMoreInteractions(emptyStorageMock);
    }

    @Test
    public void updateSpent_skipCallStorageWhenNothingSpent() {
        UnitsBalanceImpl unitsBalance = new UnitsBalanceImpl(CLIENT_ID, DEFAULT_LIMIT, DEFAULT_LIMIT, 0);

        testedService = new UnitsService(emptyStorageMock);

        assumeThat(unitsBalance.spentInCurrentRequest(), is(0));
        testedService.updateSpent(unitsBalance);

        verifyZeroInteractions(emptyStorageMock);
    }

}

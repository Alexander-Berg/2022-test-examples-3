package ru.yandex.direct.api.v5.units.logging;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.units.UnitsLogData;
import ru.yandex.direct.api.v5.security.DirectApiPreAuthentication;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.service.ApiUserService;
import ru.yandex.direct.core.units.service.UnitsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.direct.api.v5.units.logging.UnitsLogDataFactoryCreateTestData.CLIENT;
import static ru.yandex.direct.api.v5.units.logging.UnitsLogDataFactoryCreateTestData.CLIENT_CHIEF;
import static ru.yandex.direct.api.v5.units.logging.UnitsLogDataFactoryCreateTestData.LIMIT;
import static ru.yandex.direct.api.v5.units.logging.UnitsLogDataFactoryCreateTestData.OPERATOR;
import static ru.yandex.direct.api.v5.units.logging.UnitsLogDataFactoryCreateTestData.OPERATOR_CHIEF;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class UnitsLogDataFactoryCreateTest {

    @Mock
    private UnitsService unitsService;

    @Mock
    private ApiUserService apiUserService;

    @InjectMocks
    private UnitsLogDataFactory unitsLogDataFactory;

    @Parameter()
    public String description;

    @Parameter(1)
    public DirectApiPreAuthentication auth;

    @Parameter(2)
    public ApiUser clientBrandChief;

    @Parameter(3)
    public ApiUser operatorBrandChief;

    @Parameter(4)
    public UnitsLogData expectedResult;

    @Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
        return UnitsLogDataFactoryCreateTestData.provideData();
    }

    @Before
    public void init() {
        openMocks(this);

        when(apiUserService.getBrandChiefRepFor(same(CLIENT))).thenReturn(clientBrandChief);
        when(apiUserService.getBrandChiefRepFor(same(CLIENT_CHIEF))).thenReturn(clientBrandChief);
        when(apiUserService.getBrandChiefRepFor(same(OPERATOR))).thenReturn(operatorBrandChief);
        when(apiUserService.getBrandChiefRepFor(same(OPERATOR_CHIEF))).thenReturn(operatorBrandChief);

        when(unitsService.getLimit(any(ApiUser.class))).thenReturn(LIMIT);
    }

    @Test
    public void shouldCreateExpectedUnitsLogDataStructure() {
        UnitsLogData actual = unitsLogDataFactory.createUnitsLogData(auth);

        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("requestId")
                .isEqualTo(expectedResult);
    }

}

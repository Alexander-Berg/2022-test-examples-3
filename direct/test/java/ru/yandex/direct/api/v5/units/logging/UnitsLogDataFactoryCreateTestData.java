package ru.yandex.direct.api.v5.units.logging;

import java.util.Arrays;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.api.v5.context.units.OperatorBrandData;
import ru.yandex.direct.api.v5.context.units.OperatorClientData;
import ru.yandex.direct.api.v5.context.units.OperatorData;
import ru.yandex.direct.api.v5.context.units.SubclientBrandData;
import ru.yandex.direct.api.v5.context.units.SubclientClientData;
import ru.yandex.direct.api.v5.context.units.SubclientData;
import ru.yandex.direct.api.v5.context.units.UnitsLogData;
import ru.yandex.direct.api.v5.security.DirectApiCredentials;
import ru.yandex.direct.api.v5.security.DirectApiPreAuthentication;
import ru.yandex.direct.api.v5.security.utils.ApiUserMockBuilder;
import ru.yandex.direct.api.v5.units.UseOperatorUnitsMode;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.rbac.RbacRole;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.units.UseOperatorUnitsMode.FALSE;
import static ru.yandex.direct.api.v5.units.UseOperatorUnitsMode.TRUE;

@ParametersAreNonnullByDefault
class UnitsLogDataFactoryCreateTestData {

    static final String APPLICATION_ID = "23904";
    static final Integer LIMIT = 100_000;
    private static final Long MANUAL_LIMIT = 80_000L;

    static final ApiUser OPERATOR = new ApiUserMockBuilder("op", 123, 2, RbacRole.AGENCY)
            .withManualUnitsLimit(MANUAL_LIMIT).build();
    static final ApiUser OPERATOR_CHIEF = new ApiUserMockBuilder("op_chief", 234, 2, RbacRole.AGENCY)
            .withManualUnitsLimit(MANUAL_LIMIT).build();
    static final ApiUser CLIENT = new ApiUserMockBuilder("client", 345, 3, RbacRole.CLIENT)
            .withManualUnitsLimit(MANUAL_LIMIT).build();
    static final ApiUser CLIENT_CHIEF = new ApiUserMockBuilder("client_chief", 456, 3, RbacRole.CLIENT)
            .withManualUnitsLimit(MANUAL_LIMIT).build();
    static final ApiUser CLIENT_BRAND_CHIEF = new ApiUserMockBuilder("client_brand_chief", 567, 4, RbacRole.CLIENT)
            .withManualUnitsLimit(MANUAL_LIMIT).build();
    static final ApiUser OPERATOR_BRAND_CHIEF = new ApiUserMockBuilder("operator_brand_chief", 675, 5, RbacRole.AGENCY)
            .withManualUnitsLimit(MANUAL_LIMIT).build();

    private static final OperatorClientData OPERATOR_CLIENT_DATA = new OperatorClientData()
            .withOperatorClientId(OPERATOR_CHIEF.getClientId().asLong())
            .withOperatorClientLogin(OPERATOR_CHIEF.getLogin())
            .withOperatorClientRole(OPERATOR_CHIEF.getRole())
            .withOperatorClientUnitsDaily(LIMIT)
            .withOperatorClientUnitsManual(MANUAL_LIMIT.intValue());

    private static final OperatorBrandData OPERATOR_BRAND_DATA = new OperatorBrandData()
            .withOperatorBrandClientId(OPERATOR_BRAND_CHIEF.getClientId().asLong())
            .withOperatorBrandLogin(OPERATOR_BRAND_CHIEF.getLogin())
            .withOperatorBrandUnitsDaily(LIMIT)
            .withOperatorBrandUnitsManual(MANUAL_LIMIT.intValue());

    private static final SubclientClientData SUBCLIENT_CLIENT_DATA = new SubclientClientData()
            .withSubclientClientId(CLIENT_CHIEF.getClientId().asLong())
            .withSubclientLogin(CLIENT_CHIEF.getLogin())
            .withSubclientUid(CLIENT_CHIEF.getUid())
            .withSubclientUnitsDaily(LIMIT)
            .withSubclientUnitsManual(MANUAL_LIMIT.intValue());

    private static final SubclientBrandData SUBCLIENT_BRAND_DATA = new SubclientBrandData()
            .withSubclientBrandClientId(CLIENT_BRAND_CHIEF.getClientId().asLong())
            .withSubclientBrandLogin(CLIENT_BRAND_CHIEF.getLogin())
            .withSubclientBrandUnitsDaily(LIMIT)
            .withSubclientBrandUnitsManual(MANUAL_LIMIT.intValue());

    static Iterable<Object[]> provideData() {
        return Arrays.asList(

                new TestCase("Клиент под брендом : оператор под брендом")
                        .clientUnderBrand()
                        .operatorUnderBrand()

                        .expect(new UnitsLogData()
                                .withBucket(null)
                                .withUseOperatorUnitsMode(FALSE)
                                .withClientLogin(CLIENT.getLogin())
                                .withOperator(new OperatorData()
                                        .withOperatorLogin(OPERATOR.getLogin())
                                        .withOperatorUid(OPERATOR.getUid())
                                        .withClient(OPERATOR_CLIENT_DATA)
                                        .withBrand(OPERATOR_BRAND_DATA))
                                .withSubcilent(new SubclientData()
                                        .withClient(SUBCLIENT_CLIENT_DATA)
                                        .withBrand(SUBCLIENT_BRAND_DATA))
                        ),

                new TestCase("Клиент под брендом : оператор вне бренда")
                        .clientUnderBrand()

                        .expect(new UnitsLogData()
                                .withBucket(null)
                                .withUseOperatorUnitsMode(FALSE)
                                .withClientLogin(CLIENT.getLogin())
                                .withOperator(new OperatorData()
                                        .withOperatorLogin(OPERATOR.getLogin())
                                        .withOperatorUid(OPERATOR.getUid())
                                        .withClient(OPERATOR_CLIENT_DATA)
                                        .withBrand(new OperatorBrandData()))    // <-
                                .withSubcilent(new SubclientData()
                                        .withClient(SUBCLIENT_CLIENT_DATA)
                                        .withBrand(SUBCLIENT_BRAND_DATA))
                        ),

                new TestCase("Клиент вне бренда : оператор под брендом : Use-Operator-Units")
                        .operatorUnderBrand()
                        .useOperatorUnits()

                        .expect(new UnitsLogData()
                                .withBucket(null)
                                .withUseOperatorUnitsMode(TRUE)                 // <-
                                .withClientLogin(CLIENT.getLogin())
                                .withOperator(new OperatorData()
                                        .withOperatorLogin(OPERATOR.getLogin())
                                        .withOperatorUid(OPERATOR.getUid())
                                        .withClient(OPERATOR_CLIENT_DATA)
                                        .withBrand(OPERATOR_BRAND_DATA))
                                .withSubcilent(new SubclientData()
                                        .withClient(SUBCLIENT_CLIENT_DATA)
                                        .withBrand(new SubclientBrandData()))   // <-
                        )
        );
    }

    private static class TestCase {

        private String description;
        private UseOperatorUnitsMode useOperatorUnitsMode = FALSE;
        private ApiUser clientBrandChief;
        private ApiUser operatorBrandChief;

        TestCase(String description) {
            this.description = description;
        }

        TestCase useOperatorUnits() {
            useOperatorUnitsMode = TRUE;
            return this;
        }

        TestCase clientUnderBrand() {
            this.clientBrandChief = CLIENT_BRAND_CHIEF;
            return this;
        }

        TestCase operatorUnderBrand() {
            this.operatorBrandChief = OPERATOR_BRAND_CHIEF;
            return this;
        }

        Object[] expect(UnitsLogData expectedResult) {
            DirectApiCredentials credentials = mock(DirectApiCredentials.class);
            when(credentials.getUseOperatorUnitsMode()).thenReturn(useOperatorUnitsMode);
            return new Object[]{
                    description,
                    new DirectApiPreAuthentication(
                            credentials,
                            APPLICATION_ID,
                            OPERATOR,
                            OPERATOR_CHIEF,
                            CLIENT,
                            CLIENT_CHIEF
                    ),
                    clientBrandChief,
                    operatorBrandChief,
                    expectedResult
            };
        }

    }

}

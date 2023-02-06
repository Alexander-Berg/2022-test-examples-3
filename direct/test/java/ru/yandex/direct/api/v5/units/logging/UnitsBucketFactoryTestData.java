package ru.yandex.direct.api.v5.units.logging;

import java.util.Arrays;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.api.v5.context.units.OperatorBrandData;
import ru.yandex.direct.api.v5.context.units.OperatorClientData;
import ru.yandex.direct.api.v5.context.units.OperatorData;
import ru.yandex.direct.api.v5.context.units.SubclientBrandData;
import ru.yandex.direct.api.v5.context.units.SubclientClientData;
import ru.yandex.direct.api.v5.context.units.SubclientData;
import ru.yandex.direct.api.v5.context.units.UnitsBucket;
import ru.yandex.direct.api.v5.context.units.UnitsLogData;
import ru.yandex.direct.api.v5.units.UseOperatorUnitsMode;
import ru.yandex.direct.core.units.api.UnitsBalance;
import ru.yandex.direct.core.units.api.UnitsBalanceImpl;

import static ru.yandex.direct.api.v5.units.UseOperatorUnitsMode.TRUE;
import static ru.yandex.direct.api.v5.units.logging.UnitsBucketFactoryTestData.TestCase.BalanceType.BRAND;
import static ru.yandex.direct.api.v5.units.logging.UnitsBucketFactoryTestData.TestCase.BalanceType.CLIENT;

@ParametersAreNonnullByDefault
class UnitsBucketFactoryTestData {

    private static final String SUB_CLIENT_LOGIN = "subclient-client-login";
    private static final String SUB_BRAND_LOGIN = "subclient-brand-login";
    private static final String OP_CLIENT_LOGIN = "operator-client-login";
    private static final String OP_BRAND_LOGIN = "operator-brand-login";

    private static final Long SUB_CLIENT_ID = 12345L;
    private static final Long SUB_BRAND_ID = 23451L;
    private static final Long OP_CLIENT_ID = 34512L;
    private static final Long OP_BRAND_ID = 45123L;

    private static final int LIMIT = 100_000;
    private static final int BALANCE = 80_000;

    static Iterable<Object[]> provideData() {
        return Arrays.asList(

                new TestCase("Клиент вне бренда : оператор вне бренда")

                        .expectUnitsHolderBucket(UnitsBucket.Type.SUBCLIENT, SUB_CLIENT_LOGIN)
                        .expectOperatorBucket(UnitsBucket.Type.OPERATOR, OP_CLIENT_LOGIN)
                        .build(),

                new TestCase("Клиент вне бренда : оператор вне бренда : Use-Operator-Units")
                        .useOperatorUnits(TRUE)

                        .expectUnitsHolderBucket(UnitsBucket.Type.OPERATOR, OP_CLIENT_LOGIN)
                        .expectOperatorBucket(UnitsBucket.Type.OPERATOR, OP_CLIENT_LOGIN)
                        .build(),

                new TestCase("Клиент под брендом : оператор вне бренда")
                        .clientUnderBrand()
                        .unitsBalanceOf(BRAND)

                        .expectUnitsHolderBucket(UnitsBucket.Type.SUBCLIENT_BRAND, SUB_BRAND_LOGIN)
                        .expectOperatorBucket(UnitsBucket.Type.OPERATOR, OP_CLIENT_LOGIN)
                        .build(),

                new TestCase("Клиент под брендом, но списание с клиента : оператор вне бренда")
                        .clientUnderBrand()
                        .unitsBalanceOf(CLIENT) // такое бывает, например, если у клиента лимит выше, чем у бренда

                        .expectUnitsHolderBucket(UnitsBucket.Type.SUBCLIENT, SUB_CLIENT_LOGIN)
                        .expectOperatorBucket(UnitsBucket.Type.OPERATOR, OP_CLIENT_LOGIN)
                        .build(),

                new TestCase("Клиент вне бренда : оператор под брендом")
                        .operatorUnderBrand()
                        .operatorUnitsBalanceOf(BRAND)

                        .expectUnitsHolderBucket(UnitsBucket.Type.SUBCLIENT, SUB_CLIENT_LOGIN)
                        .expectOperatorBucket(UnitsBucket.Type.OPERATOR_BRAND, OP_BRAND_LOGIN)
                        .build(),

                new TestCase("Клиент вне бренда : оператор под брендом, но списание с оператора")
                        .operatorUnderBrand()
                        .operatorUnitsBalanceOf(CLIENT)

                        .expectUnitsHolderBucket(UnitsBucket.Type.SUBCLIENT, SUB_CLIENT_LOGIN)
                        .expectOperatorBucket(UnitsBucket.Type.OPERATOR, OP_CLIENT_LOGIN)
                        .build(),

                new TestCase("Клиент под брендом : оператор под брендом")
                        .clientUnderBrand()
                        .operatorUnderBrand()
                        .unitsBalanceOf(BRAND)
                        .operatorUnitsBalanceOf(BRAND)

                        .expectUnitsHolderBucket(UnitsBucket.Type.SUBCLIENT_BRAND, SUB_BRAND_LOGIN)
                        .expectOperatorBucket(UnitsBucket.Type.OPERATOR_BRAND, OP_BRAND_LOGIN)
                        .build(),

                new TestCase("Клиент под брендом : оператор под брендом : Use-Operator-Units")
                        .useOperatorUnits(TRUE)
                        .clientUnderBrand()
                        .operatorUnderBrand()

                        .unitsBalanceOf(BRAND)
                        .operatorUnitsBalanceOf(BRAND)
                        .expectUnitsHolderBucket(UnitsBucket.Type.OPERATOR_BRAND, OP_BRAND_LOGIN)
                        .expectOperatorBucket(UnitsBucket.Type.OPERATOR_BRAND, OP_BRAND_LOGIN)
                        .build()

        );
    }

    static class TestCase {

        enum BalanceType {
            CLIENT,
            BRAND
        }

        private String description;

        private String subBrandLogin = null;
        private String opBrandLogin = null;

        private Long subBrandId = null;
        private Long opBrandId = null;

        private BalanceType unitsBalanceType = CLIENT;
        private BalanceType operatorUnitsBalanceType = CLIENT;

        private UseOperatorUnitsMode useOperatorUnitsMode;

        private UnitsBucket expectedUnitsHolderBucket;
        private UnitsBucket expectedOperatorBucket;

        TestCase(String description) {
            this.description = description;
        }

        Object[] build() {
            return new Object[]{
                    description,
                    createUnitsLogData(),
                    createUnitsBalance(),
                    createOperatorUnitsBalance(),
                    expectedUnitsHolderBucket,
                    expectedOperatorBucket
            };
        }

        private UnitsLogData createUnitsLogData() {
            return new UnitsLogData()
                    .withSubcilent(new SubclientData()
                            .withBrand(new SubclientBrandData()
                                    .withSubclientBrandLogin(subBrandLogin)
                                    .withSubclientBrandClientId(subBrandId))
                            .withClient(new SubclientClientData()
                                    .withSubclientLogin(SUB_CLIENT_LOGIN)
                                    .withSubclientClientId(SUB_CLIENT_ID)))
                    .withOperator(new OperatorData()
                            .withBrand(new OperatorBrandData()
                                    .withOperatorBrandLogin(opBrandLogin)
                                    .withOperatorBrandClientId(opBrandId))
                            .withClient(new OperatorClientData()
                                    .withOperatorClientLogin(OP_CLIENT_LOGIN)
                                    .withOperatorClientId(OP_CLIENT_ID)));
        }

        private UnitsBalance createUnitsBalance() {
            if (useOperatorUnitsMode == TRUE) {
                return createOperatorUnitsBalance();
            } else {
                return new UnitsBalanceImpl(
                        unitsBalanceType == BRAND ?
                                subBrandId : SUB_CLIENT_ID,
                        LIMIT, BALANCE, 0);
            }
        }

        private UnitsBalance createOperatorUnitsBalance() {
            return new UnitsBalanceImpl(
                    operatorUnitsBalanceType == BRAND ?
                            opBrandId : OP_CLIENT_ID,
                    LIMIT, BALANCE, 0);
        }

        TestCase clientUnderBrand() {
            subBrandLogin = SUB_BRAND_LOGIN;
            subBrandId = SUB_BRAND_ID;
            return this;
        }

        TestCase operatorUnderBrand() {
            opBrandLogin = OP_BRAND_LOGIN;
            opBrandId = OP_BRAND_ID;
            return this;
        }

        TestCase unitsBalanceOf(BalanceType type) {
            unitsBalanceType = type;
            return this;
        }

        TestCase operatorUnitsBalanceOf(BalanceType type) {
            operatorUnitsBalanceType = type;
            return this;
        }

        TestCase useOperatorUnits(UseOperatorUnitsMode useOperatorUnitsMode) {
            this.useOperatorUnitsMode = useOperatorUnitsMode;
            return this;
        }

        TestCase expectUnitsHolderBucket(UnitsBucket.Type type, String ownerLogin) {
            expectedUnitsHolderBucket = new UnitsBucket()
                    .withBucketType(type)
                    .withBucketLogin(ownerLogin);
            return this;
        }

        TestCase expectOperatorBucket(UnitsBucket.Type type, String ownerLogin) {
            expectedOperatorBucket = new UnitsBucket()
                    .withBucketType(type)
                    .withBucketLogin(ownerLogin);
            return this;
        }
    }

}

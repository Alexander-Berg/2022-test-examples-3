package ru.yandex.direct.api.v5.units;

import java.util.Arrays;

import javax.annotation.Nullable;

import com.google.common.base.Joiner;

import ru.yandex.direct.api.v5.security.DirectApiCredentials;
import ru.yandex.direct.api.v5.security.DirectApiPreAuthentication;
import ru.yandex.direct.api.v5.security.utils.ApiUserMockBuilder;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.rbac.RbacRole;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.units.UseOperatorUnitsMode.AUTO;
import static ru.yandex.direct.api.v5.units.UseOperatorUnitsMode.FALSE;
import static ru.yandex.direct.api.v5.units.UseOperatorUnitsMode.TRUE;

class UnitsHolderDetectorTestData {

    static final int BASIC_UNITS_LIMIT = 10_000;

    static final ApiUser AGENCY = new ApiUserMockBuilder("ag", 123, 2, RbacRole.AGENCY).build();
    static final ApiUser AGENCY_CHIEF = new ApiUserMockBuilder("ag_chief", 234, 2, RbacRole.AGENCY).build();
    static final ApiUser CLIENT = new ApiUserMockBuilder("client", 345, 3, RbacRole.CLIENT).build();
    static final ApiUser CLIENT_CHIEF = new ApiUserMockBuilder("client_chief", 456, 3, RbacRole.CLIENT).build();
    static final ApiUser BRAND_CHIEF = new ApiUserMockBuilder("brand_chief", 567, 4, RbacRole.CLIENT).build();

    static final String APPLICATION_ID = "23904";

    static Iterable<Object[]> provideData() {
        return Arrays.asList(

                new TestDataBuilder()
                        .operator(AGENCY)
                        .client(CLIENT)
                        .withUseOperatorUnitsMode(FALSE)
                        .expectedUnitsHolder(CLIENT_CHIEF)
                        .expectedOperatorUnitsHolder(AGENCY_CHIEF)
                        .expectedUnitsUsedLogin(CLIENT)
                        .build(),

                new TestDataBuilder()
                        .operator(AGENCY)
                        .client(CLIENT)
                        .withUseOperatorUnitsMode(TRUE)
                        .expectedUnitsHolder(AGENCY_CHIEF)
                        .expectedOperatorUnitsHolder(AGENCY_CHIEF)
                        .expectedUnitsUsedLogin(AGENCY)
                        .build(),

                new TestDataBuilder()
                        .operator(AGENCY)
                        .client(CLIENT)
                        .withUseOperatorUnitsMode(AUTO)
                        .expectedUnitsHolder(CLIENT_CHIEF)
                        .expectedOperatorUnitsHolder(AGENCY_CHIEF)
                        .expectedUnitsUsedLogin(CLIENT)
                        .build(),

                new TestDataBuilder()
                        .operator(AGENCY)
                        .client(CLIENT)
                        .withUseOperatorUnitsMode(AUTO)
                        .expectedUnitsHolder(AGENCY_CHIEF)
                        .expectedOperatorUnitsHolder(AGENCY_CHIEF)
                        .expectedUnitsUsedLogin(AGENCY)
                        .insufficientUnits()
                        .build(),

                new TestDataBuilder()
                        .operator(CLIENT)
                        .client(CLIENT)
                        .withUseOperatorUnitsMode(FALSE)
                        .clientHasBrand()
                        .expectedUnitsHolder(BRAND_CHIEF)
                        .expectedOperatorUnitsHolder(BRAND_CHIEF)
                        .expectedUnitsUsedLogin(CLIENT)
                        .build(),

                new TestDataBuilder()
                        .operator(AGENCY)
                        .client(CLIENT)
                        .withUseOperatorUnitsMode(FALSE)
                        .clientHasBrand()
                        .expectedUnitsHolder(BRAND_CHIEF)
                        .expectedOperatorUnitsHolder(AGENCY_CHIEF)
                        .expectedUnitsUsedLogin(CLIENT)
                        .build(),

                new TestDataBuilder()
                        .operator(AGENCY)
                        .client(CLIENT)
                        .withUseOperatorUnitsMode(TRUE)
                        .clientHasBrand()
                        .expectedUnitsHolder(AGENCY_CHIEF)
                        .expectedOperatorUnitsHolder(AGENCY_CHIEF)
                        .expectedUnitsUsedLogin(AGENCY)
                        .build(),

                new TestDataBuilder()
                        .operator(AGENCY)
                        .client(CLIENT)
                        .withUseOperatorUnitsMode(AUTO)
                        .clientHasBrand()
                        .expectedUnitsHolder(BRAND_CHIEF)
                        .expectedOperatorUnitsHolder(AGENCY_CHIEF)
                        .expectedUnitsUsedLogin(CLIENT)
                        .build(),

                new TestDataBuilder()
                        .operator(AGENCY)
                        .client(CLIENT)
                        .withUseOperatorUnitsMode(AUTO)
                        .clientHasBrand()
                        .expectedUnitsHolder(AGENCY_CHIEF)
                        .expectedOperatorUnitsHolder(AGENCY_CHIEF)
                        .expectedUnitsUsedLogin(AGENCY)
                        .insufficientUnits()
                        .build(),

                new TestDataBuilder()
                        .operator(AGENCY)
                        .client(CLIENT)
                        .withUseOperatorUnitsMode(FALSE)
                        .clientHasBrand()
                        .brandUnitsLimitIsGreaterBy(100)
                        .expectedUnitsHolder(BRAND_CHIEF)
                        .expectedOperatorUnitsHolder(AGENCY_CHIEF)
                        .expectedUnitsUsedLogin(CLIENT)
                        .build(),

                new TestDataBuilder()
                        .operator(AGENCY)
                        .client(CLIENT)
                        .withUseOperatorUnitsMode(FALSE)
                        .clientHasBrand()
                        .brandUnitsLimitIsGreaterBy(-100)
                        .expectedUnitsHolder(CLIENT_CHIEF)
                        .expectedOperatorUnitsHolder(AGENCY_CHIEF)
                        .expectedUnitsUsedLogin(CLIENT)
                        .build(),

                new TestDataBuilder()
                        .operator(AGENCY)
                        .withUseOperatorUnitsMode(FALSE)
                        .expectedOperatorUnitsHolder(AGENCY_CHIEF)
                        .expectedUnitsHolder(AGENCY_CHIEF)
                        .expectedUnitsUsedLogin(AGENCY)
                        .build()
        );
    }

    private static class TestDataBuilder {

        private ApiUser operator;
        private ApiUser client;
        private UseOperatorUnitsMode useOperatorUnitsMode;
        private ApiUser brandChief;
        private int unitsDiff;
        private ApiUser expectedUnitsHolder;
        private ApiUser expectedOperatorUnitsHolder;
        private ApiUser expectedUnitsUsedLogin;
        private boolean insufficientUnits;

        Object[] build() {
            boolean brand = brandChief != null && brandChief != client;
            String description = desc(operator, client, useOperatorUnitsMode, brand, unitsDiff);

            DirectApiPreAuthentication auth = auth(operator, client, useOperatorUnitsMode);

            return new Object[]{
                    description, auth, brandChief, unitsDiff,
                    expectedUnitsHolder, expectedOperatorUnitsHolder, expectedUnitsUsedLogin, insufficientUnits
            };
        }

        TestDataBuilder operator(ApiUser operator) {
            this.operator = operator;
            return this;
        }

        TestDataBuilder client(ApiUser client) {
            this.client = client;
            return this;
        }

        TestDataBuilder withUseOperatorUnitsMode(UseOperatorUnitsMode useOperatorUnitsMode) {
            this.useOperatorUnitsMode = useOperatorUnitsMode;
            return this;
        }

        TestDataBuilder clientHasBrand() {
            this.brandChief = BRAND_CHIEF;
            return this;
        }

        TestDataBuilder brandUnitsLimitIsGreaterBy(int unitsDiff) {
            this.unitsDiff = unitsDiff;
            return this;
        }

        TestDataBuilder expectedUnitsHolder(ApiUser expectedUnitsHolder) {
            this.expectedUnitsHolder = expectedUnitsHolder;
            return this;
        }

        TestDataBuilder expectedOperatorUnitsHolder(ApiUser expectedOperatorUnitsHolder) {
            this.expectedOperatorUnitsHolder = expectedOperatorUnitsHolder;
            return this;
        }

        TestDataBuilder expectedUnitsUsedLogin(ApiUser expectedUnitsUsedLogin) {
            this.expectedUnitsUsedLogin = expectedUnitsUsedLogin;
            return this;
        }

        TestDataBuilder insufficientUnits() {
            this.insufficientUnits = true;
            return this;
        }

    }

    private static String desc(ApiUser operator, @Nullable ApiUser client, UseOperatorUnitsMode useOperatorUnitsMode,
                               boolean brand, int limitsDiff) {

        StringBuilder desc = new StringBuilder(Joiner.on(" : ")
                .join(operator.getLogin(), client != null ? client.getLogin() : "--empty--", useOperatorUnitsMode.toString()));
        if (brand) {
            desc.append(" : Клиент под брендом");
            if (limitsDiff > 0) {
                desc.append(", лимит бренда больше.");
            } else if (limitsDiff < 0) {
                desc.append(", лимит клиента больше.");
            } else {
                desc.append(".");
            }
        }
        return desc.toString();
    }

    private static DirectApiPreAuthentication auth(ApiUser operator, @Nullable ApiUser clientLogin,
                                                   UseOperatorUnitsMode useOperatorUnitsMode) {

        final ApiUser chiefOperator;
        if (operator == AGENCY) {
            chiefOperator = AGENCY_CHIEF;
        } else if (operator == CLIENT) {
            chiefOperator = CLIENT_CHIEF;
        } else {
            chiefOperator = operator;
        }

        ApiUser chiefClient;

        if (clientLogin != null) {
            chiefClient = clientLogin == CLIENT ? CLIENT_CHIEF : clientLogin;
        } else {
            chiefClient = null;
        }

        DirectApiCredentials credentials = mock(DirectApiCredentials.class);
        when(credentials.getUseOperatorUnitsMode()).thenReturn(useOperatorUnitsMode);

        return new DirectApiPreAuthentication(credentials, "some-id", operator, chiefOperator, clientLogin,
                chiefClient);
    }

}

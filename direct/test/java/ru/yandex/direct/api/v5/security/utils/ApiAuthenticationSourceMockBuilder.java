package ru.yandex.direct.api.v5.security.utils;

import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.security.DirectApiAuthentication;
import ru.yandex.direct.core.entity.user.model.ApiUser;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ApiAuthenticationSourceMockBuilder {
    private ApiUser chiefOperator;
    private ApiUser operator;
    private ApiUser chiefSubclient;
    private ApiUser subclient;
    private String applicationId;
    private boolean shouldUseOperatorUnits;
    private boolean clientLoginIsEmpty;
    private String tvmUserTicket;

    public ApiAuthenticationSourceMockBuilder withChiefOperator(ApiUser chiefOperator) {
        this.chiefOperator = chiefOperator;
        return this;
    }

    public ApiAuthenticationSourceMockBuilder withOperator(ApiUser operator) {
        this.operator = operator;
        return this;
    }

    public ApiAuthenticationSourceMockBuilder withChiefSubclient(ApiUser chiefSubclient) {
        this.chiefSubclient = chiefSubclient;
        return this;
    }

    public ApiAuthenticationSourceMockBuilder withSubclient(ApiUser subclient) {
        this.subclient = subclient;
        return this;
    }

    public ApiAuthenticationSourceMockBuilder withApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public ApiAuthenticationSourceMockBuilder withShouldUseOperatorUnits(boolean shouldUseOperatorUnits) {
        this.shouldUseOperatorUnits = shouldUseOperatorUnits;
        return this;
    }

    public ApiAuthenticationSourceMockBuilder withClientLoginIsEmpty(boolean clientLoginIsEmpty) {
        this.clientLoginIsEmpty = clientLoginIsEmpty;
        return this;
    }

    public ApiAuthenticationSourceMockBuilder withTvmUserTicket(String tvmUserTicket) {
        this.tvmUserTicket = tvmUserTicket;
        return this;
    }

    public ApiAuthenticationSource toApiAuthenticationSource() {
        ApiAuthenticationSource result = mock(ApiAuthenticationSource.class);

        tuneAuthSourceMock(result);

        return result;
    }

    /**
     * Настроить указанный mock в соответствии с параметрами builder'а
     */
    public void tuneAuthSourceMock(ApiAuthenticationSource mock) {
        doReturn(chiefOperator != null ? chiefOperator : operator)
                .when(mock).getChiefOperator();

        doReturn(operator).when(mock).getOperator();

        doReturn(chiefSubclient != null ? chiefSubclient : subclient != null ? subclient : operator)
                .when(mock).getChiefSubclient();

        doReturn(subclient != null ? subclient : operator)
                .when(mock).getSubclient();

        doReturn(new DirectApiAuthentication(
                operator,
                chiefOperator,
                subclient,
                chiefSubclient,
                clientLoginIsEmpty,
                tvmUserTicket,
                "",
                null))
                .when(mock).getAuthentication();
    }
}

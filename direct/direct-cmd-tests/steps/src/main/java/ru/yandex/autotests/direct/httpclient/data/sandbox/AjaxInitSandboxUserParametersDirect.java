package ru.yandex.autotests.direct.httpclient.data.sandbox;

import ru.yandex.autotests.direct.httpclient.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * Created by proxeter (Nikolay Mulyar - proxeter@yandex-team.ru) on 19.05.2014.
 */
public class AjaxInitSandboxUserParametersDirect extends AbstractFormParameters {

    @FormParameter("sandbox_client_type")
    private String sandboxClientType;

    @FormParameter("init_test_data")
    private String initTestData;

    @FormParameter("initial_currency")
    private String initialCurrency;

    @FormParameter("enable_shared_account")
    private String enableSharedAccount;

    public String getSandboxClientType() {
        return sandboxClientType;
    }

    public void setSandboxClientType(String sandboxClientType) {
        this.sandboxClientType = sandboxClientType;
    }

    public String getInitTestData() {
        return initTestData;
    }

    public void setInitTestData(String initTestData) {
        this.initTestData = initTestData;
    }

    public String getInitialCurrency() {
        return initialCurrency;
    }

    public void setInitialCurrency(String initialCurrency) {
        this.initialCurrency = initialCurrency;
    }

    public String getEnableSharedAccount() {
        return enableSharedAccount;
    }

    public void setEnableSharedAccount(String enableSharedAccount) {
        this.enableSharedAccount = enableSharedAccount;
    }
}

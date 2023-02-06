package ru.yandex.direct.internaltools.tools.testtool.container;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.direct.internaltools.core.annotations.output.Enrich;
import ru.yandex.direct.internaltools.core.enums.InternalToolDetailKey;

@ParametersAreNonnullByDefault
public class TestEnrichToolInfo {
    @JsonProperty("BooleanYesNoFetcher")
    @Enrich(InternalToolDetailKey.BOOLEAN_REPLACE)
    private Boolean booleanValue = Boolean.TRUE;

    @JsonProperty("IpWhoisLinkFetcher")
    @Enrich(InternalToolDetailKey.IP_WHOIS_LINK_REPLACE)
    private String ip = "1.1.1.1";

    @JsonProperty("UserDataByLoginFetcher")
    @Enrich(InternalToolDetailKey.LOGIN)
    private String login = "spb-tester";

    @JsonProperty("CommonLinkFetcher")
    @Enrich(InternalToolDetailKey.COMMON_LINK_REPLACE)
    private String commonUrl = "https://st.yandex-team.ru/DIRECT-99895";

    public TestEnrichToolInfo withBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
        return this;
    }

    public TestEnrichToolInfo withIp(String ip) {
        this.ip = ip;
        return this;
    }

    public TestEnrichToolInfo withLogin(String login) {
        this.login = login;
        return this;
    }

    public TestEnrichToolInfo withCommonUrl(String commonUrl) {
        this.commonUrl = commonUrl;
        return this;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public String getIp() {
        return ip;
    }

    public String getLogin() {
        return login;
    }

    public String getCommonUrl() {
        return commonUrl;
    }
}

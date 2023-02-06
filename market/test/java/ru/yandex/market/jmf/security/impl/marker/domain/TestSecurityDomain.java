package ru.yandex.market.jmf.security.impl.marker.domain;

import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.security.Profile;

public class TestSecurityDomain extends SecurityDomain {
    public TestSecurityDomain(Metaclass metaclass, SecurityDomain parent) {
        super(metaclass, parent);
    }

    @Override
    public void setAllowed(String profileId, String markerId, boolean value) {
        super.setAllowed(profileId, markerId, value);
    }

    @Override
    public void addProfile(Profile profile) {
        super.addProfile(profile);
    }

    @Override
    public void setDecisionScriptCode(String profileId, String markerId, String decisionScriptCode) {
        super.setDecisionScriptCode(profileId, markerId, decisionScriptCode);
    }
}

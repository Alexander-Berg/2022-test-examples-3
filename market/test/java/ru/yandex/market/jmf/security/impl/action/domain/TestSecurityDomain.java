package ru.yandex.market.jmf.security.impl.action.domain;

import javax.annotation.Nonnull;

import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.security.Profile;
import ru.yandex.market.jmf.security.action.Action;

public class TestSecurityDomain extends SecurityDomain {
    public TestSecurityDomain(@Nonnull Metaclass domainMetaclass, @Nonnull SecurityDomain parent) {
        super(domainMetaclass, parent);
    }

    @Override
    public void addProfile(Profile profile) {
        super.addProfile(profile);
    }

    @Override
    public void addAction(Action action) {
        super.addAction(action);
    }

    @Override
    public void setAllowed(String profileId, String actionId, boolean value) {
        super.setAllowed(profileId, actionId, value);
    }

    @Override
    public void setDecisionScriptCode(String profileId, String actionId, String decisionScriptCode) {
        super.setDecisionScriptCode(profileId, actionId, decisionScriptCode);
    }
}

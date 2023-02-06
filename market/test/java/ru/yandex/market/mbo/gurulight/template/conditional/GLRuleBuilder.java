package ru.yandex.market.mbo.gurulight.template.conditional;

import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleImpl;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRulePredicate;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author commince
 */
public class GLRuleBuilder {

    private GLRule rule;

    public GLRuleBuilder(GLRuleType type, int weight) {
        rule = new GLRuleImpl();

        rule.setType(type);
        rule.setWeight(weight);
        rule.setIfs(new ArrayList<>());
        rule.setThens(new ArrayList<>());
        rule.setPublished(true);
    }

    public GLRuleBuilder addIf(Long paramId, Long valueId, String type) {
        GLRulePredicate p = new GLRulePredicate(paramId, valueId == null ? 0 : valueId, type);
        rule.getIfs().add(p);
        return this;
    }

    public GLRuleBuilder addIf(String tag, String type) {
        GLRulePredicate p = new GLRulePredicate(0, 0, type);
        p.setProcessingTag(tag);
        rule.getIfs().add(p);
        return this;
    }

    public GLRuleBuilder addThen(Long paramId, Long valueId, String type, List<Long> revokeValues) {
        GLRulePredicate p = new GLRulePredicate(paramId, valueId == null ? 0 : valueId, type);
        if (revokeValues != null) {
            p.setExcludeRevokeValueIds(revokeValues);
        }
        rule.getThens().add(p);
        return this;
    }

    public GLRuleBuilder addThen(String tag, String type) {
        GLRulePredicate p = new GLRulePredicate(0, 0, type);
        p.setProcessingTag(tag);
        rule.getThens().add(p);
        return this;
    }

    public GLRuleBuilder addThen(Long valueId, GLRulePredicate.Subject subject) {
        GLRulePredicate p = new GLRulePredicate(0, valueId, GLRulePredicate.ENUM_MATCHES);
        p.setSubject(subject);
        rule.getThens().add(p);
        return this;
    }

    public GLRuleBuilder addThen(Long paramId, Long valueId, String type) {
        return addThen(paramId, valueId, type, null);
    }

    public GLRuleBuilder setId(long id) {
        rule.setId(id);
        return this;
    }

    public GLRuleBuilder setHid(long hid) {
        rule.setHid(hid);
        return this;
    }

    public GLRuleBuilder setName(String name) {
        rule.setName(name);
        return this;
    }

    public GLRuleBuilder setPublished(boolean published) {
        rule.setPublished(published);
        return this;
    }

    public GLRule build() {
        return rule;
    }
}

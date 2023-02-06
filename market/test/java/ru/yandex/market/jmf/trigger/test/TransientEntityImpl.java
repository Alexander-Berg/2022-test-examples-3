package ru.yandex.market.jmf.trigger.test;

import javax.annotation.Nonnull;

import ru.yandex.market.jmf.entity.AbstractPersistentEntity;
import ru.yandex.market.jmf.metadata.Fqn;

public class TransientEntityImpl extends AbstractPersistentEntity {
    private Long attrInt;
    private String attrStr;

    public TransientEntityImpl() {
    }

    public TransientEntityImpl(Long attrInt, String attrStr) {
        this.attrInt = attrInt;
        this.attrStr = attrStr;
    }

    @Nonnull
    @Override
    public Fqn getFqn() {
        return Fqn.of("transientEntity");
    }

    @Override
    public String getGid() {
        return getFqn().gidOf("123");
    }

    public Long getAttrInt() {
        return attrInt;
    }

    public String getAttrStr() {
        return attrStr;
    }
}

package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.domain.model.Domain;

public class DomainInfo {

    private Domain domain;
    private Integer shard;

    public Domain getDomain() {
        return this.domain;
    }

    public DomainInfo withDomain(Domain domain) {
        this.domain = domain;
        return this;
    }

    public Long getDomainId() {
        return this.domain.getId();
    }

    public Integer getShard() {
        return shard;
    }

    public DomainInfo withShard(Integer shard) {
        this.shard = shard;
        return this;
    }
}

package ru.yandex.market.jmf.module.relation.test.impl;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.module.relation.Relation;

@Component
public class RelationTestUtils {
    @Inject
    private DbService dbService;

    public List<Relation> getRelations(Entity source, Entity target) {
        return dbService.list(
                Query.of(Relation.FQN)
                        .withFilters(Filters.eq(Relation.SOURCE, source.getGid()),
                                Filters.eq(Relation.TARGET, target.getGid())
                        )
        );
    }

    public List<Relation> getRelationsFrom(Entity source) {
        return dbService.list(
                Query.of(Relation.FQN)
                        .withFilters(Filters.eq(Relation.SOURCE, source.getGid()))
        );
    }

    public List<Relation> getRelationsTo(Entity target) {
        return dbService.list(
                Query.of(Relation.FQN)
                        .withFilters(Filters.eq(Relation.TARGET, target.getGid()))
        );
    }
}

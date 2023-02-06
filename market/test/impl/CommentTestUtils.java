package ru.yandex.market.jmf.module.comment.test.impl;

import java.util.List;

import org.springframework.stereotype.Component;

import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityHelper;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.query.SortingOrder;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.comment.Comment;

@Component
public class CommentTestUtils {
    private final DbService dbService;

    public CommentTestUtils(DbService dbService) {
        this.dbService = dbService;
    }

    public List<Comment> getComments(Entity entity) {
        Query q = Query.of(Comment.FQN)
                .withFilters(Filters.eq(Comment.ENTITY, entity));
        return dbService.list(q);
    }

    public List<Comment> getOrderedComments(Entity entity) {
        Query q = Query.of(Comment.FQN)
                .withFilters(Filters.eq(Comment.ENTITY, entity))
                .withSortingOrder(SortingOrder.asc(Comment.CREATION_TIME));
        return dbService.list(q);
    }

    public List<Comment> getCommentsOfType(Entity entity, Class<? extends Comment> commentType) {
        Fqn fqn = EntityHelper.fqnFromClass(commentType);
        Query q = Query.of(fqn)
                .withFilters(Filters.eq(Comment.ENTITY, entity));
        return dbService.list(q);
    }

    public Comment getLastComment(Entity entity, Fqn fqn) {
        Query q = Query.of(fqn)
                .withFilters(Filters.eq(Comment.ENTITY, entity))
                .withSortingOrder(SortingOrder.desc(Comment.CREATION_TIME))
                .withLimit(1);
        return dbService.<Comment>list(q).get(0);
    }

}

package ru.yandex.market.sre.services.tms.tasks.startrek.model;

import org.joda.time.Instant;

import ru.yandex.bolts.collection.Option;
import ru.yandex.startrek.client.model.Comment;

public class CommentExt extends Comment {
    public CommentExt(long id, String text, Instant createdAt, Instant updatedAt) {
        super(id, null, Option.of(text), null, null, null, null, null, createdAt, updatedAt, null);
    }
}

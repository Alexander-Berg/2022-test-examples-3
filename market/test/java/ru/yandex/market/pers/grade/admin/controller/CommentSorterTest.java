package ru.yandex.market.pers.grade.admin.controller;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.comments.model.Comment;
import ru.yandex.market.comments.model.legacy.Comment2;

import java.util.Arrays;
import java.util.List;

/**
 * @author dinyat
 *         05/04/2017
 */
public class CommentSorterTest {

    @Test
    public void newTopSortTest() throws Exception {
        Comment2 comment1 = new Comment2();
        comment1.deleted = true;
        comment1.updateTime = 1_000_000;
        Comment2 comment2 = new Comment2();
        comment2.deleted = false;
        comment2.updateTime = 900_000;
        Comment2 comment3 = new Comment2();
        comment3.deleted = false;
        comment3.updateTime = 1_100_000;
        List<Comment> comments = Arrays.asList(new Comment(comment1), new Comment(comment2), new Comment(comment3));

        comments = CommentSorter.NEW_TOP.sort(comments);

        Assert.assertEquals(comment3.updateTime * 1000, comments.get(0).getDate().getTime());
        Assert.assertEquals(comment1.updateTime * 1000, comments.get(1).getDate().getTime());
        Assert.assertEquals(comment2.updateTime * 1000, comments.get(2).getDate().getTime());
    }

    @Test
    public void commentToGradeTopSortTest() throws Exception {
        Comment2 comment1 = new Comment2();
        comment1.entity = "root-9-0-1234";
        Comment2 comment2 = new Comment2();
        comment2.entity = "root-6-0-1234";
        Comment2 comment3 = new Comment2();
        comment3.entity = "root-9-0-1234";
        List<Comment> comments = Arrays.asList(new Comment(comment1), new Comment(comment2), new Comment(comment3));

        comments = CommentSorter.COMMENT_TO_GRADE_TOP.sort(comments);

        Assert.assertEquals(comment1.entity, comments.get(0).getRoot());
        Assert.assertEquals(comment3.entity, comments.get(1).getRoot());
        Assert.assertEquals(comment2.entity, comments.get(2).getRoot());
    }

    @Test
    public void goodTopSortTest() throws Exception {
        Comment2 comment1 = new Comment2();
        comment1.deleted = true;
        Comment2 comment2 = new Comment2();
        comment2.deleted = false;
        Comment2 comment3 = new Comment2();
        comment3.deleted = true;
        List<Comment> comments = Arrays.asList(new Comment(comment1), new Comment(comment2), new Comment(comment3));

        comments = CommentSorter.GOOD_TOP.sort(comments);

        Assert.assertEquals(comment2.deleted, comments.get(0).isDeleted());
        Assert.assertEquals(comment1.deleted, comments.get(1).isDeleted());
        Assert.assertEquals(comment3.deleted, comments.get(2).isDeleted());
    }

    @Test
    public void badTopSortTest() throws Exception {
        Comment2 comment1 = new Comment2();
        comment1.deleted = true;
        Comment2 comment2 = new Comment2();
        comment2.deleted = false;
        Comment2 comment3 = new Comment2();
        comment3.deleted = true;
        List<Comment> comments = Arrays.asList(new Comment(comment1), new Comment(comment2), new Comment(comment3));

        comments = CommentSorter.BAD_TOP.sort(comments);

        Assert.assertEquals(comment1.deleted, comments.get(0).isDeleted());
        Assert.assertEquals(comment3.deleted, comments.get(1).isDeleted());
        Assert.assertEquals(comment2.deleted, comments.get(2).isDeleted());
    }
}
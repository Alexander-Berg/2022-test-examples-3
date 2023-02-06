package ru.yandex.market.pers.qa.mock.mvc;

import org.springframework.stereotype.Service;

import ru.yandex.market.pers.qa.client.model.CommentProject;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 13.09.2019
 */
@Service
public class ArticleCommentMvcMocks extends AbstractCommonCommentMvcMocks {
    public ArticleCommentMvcMocks() {
        super("article");
    }

    @Override
    public CommentProject getProject() {
        return CommentProject.ARTICLE;
    }
}

package ru.yandex.market.pers.qa.mock.mvc;

import org.springframework.stereotype.Service;

import ru.yandex.market.pers.qa.client.model.CommentProject;

@Service
public class VideoCommentMvcMocks extends AbstractCommonCommentMvcMocks {
    public VideoCommentMvcMocks() {
        super("video");
    }

    @Override
    public CommentProject getProject() {
        return CommentProject.VIDEO;
    }
}

package ru.yandex.market.pers.qa.service.post;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.qa.PersQATest;
import ru.yandex.market.pers.qa.exception.EntityNotFoundException;
import ru.yandex.market.pers.qa.exception.QaResult;
import ru.yandex.market.pers.qa.model.QaEntityType;
import ru.yandex.market.pers.qa.model.post.Video;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author grigor-vlad
 * 03.07.2022
 */
public class PostVideoServiceTest extends PersQATest {
    private static final String POST_ID = "1";
    private static final String VIDEO_ID = "videoId";

    @Autowired
    private PostVideoService postVideoService;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate qaJdbcTemplate;

    @Test
    public void testVideoSave() {
        Video video = new Video(QaEntityType.POST_V2, POST_ID, VIDEO_ID, 0);
        long videoId = postVideoService.createVideo(video);

        List<Video> videos = qaJdbcTemplate.query(
            "select * from post.video where id = ?",
            Video::valueOf, videoId);
        assertEquals(1, videos.size());
        //check videos fields
        Video videoFromDb = videos.get(0);
        assertEquals(videoId, videoFromDb.getId());
        assertEquals(QaEntityType.POST_V2, videoFromDb.getEntityType());
        assertEquals(POST_ID, videoFromDb.getEntityId());
        assertEquals(VIDEO_ID, videoFromDb.getVideoId());
        assertEquals(0, videoFromDb.getOrderNumber());
    }

    @Test
    public void testGetVideoById() {
        Video video = new Video(QaEntityType.POST_V2, POST_ID, VIDEO_ID, 0);
        long videoId = postVideoService.createVideo(video);

        Video videoById = postVideoService.getVideoById(videoId);
        //check videos fields
        assertEquals(videoId, videoById.getId());
        assertEquals(QaEntityType.POST_V2, videoById.getEntityType());
        assertEquals(POST_ID, videoById.getEntityId());
        assertEquals(VIDEO_ID, videoById.getVideoId());
        assertEquals(0, videoById.getOrderNumber());

        //check throw exception
        try {
            Video videoById2 = postVideoService.getVideoById(videoId + 1);
            Assertions.fail();
        } catch (EntityNotFoundException ex) {
            assertEquals(QaResult.VIDEO_NOT_FOUND, ex.getResult());
        }
    }

    @Test
    public void testGetVideoByEntity() {
        int videoCount = 3;
        for (int order = 0; order < videoCount; order++) {
            postVideoService.createVideo(new Video(QaEntityType.POST_V2, POST_ID, VIDEO_ID + order, order));
        }

        List<Video> videosByEntity = postVideoService.getVideoByEntity(QaEntityType.POST_V2, POST_ID);
        assertEquals(videoCount, videosByEntity.size());
    }
}

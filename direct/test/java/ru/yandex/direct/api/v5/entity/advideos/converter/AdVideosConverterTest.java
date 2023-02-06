package ru.yandex.direct.api.v5.entity.advideos.converter;

import java.util.List;

import com.yandex.direct.api.v5.advideos.AdVideoGetItem;
import com.yandex.direct.api.v5.advideos.AdVideosSelectionCriteria;
import com.yandex.direct.api.v5.advideos.GetResponse;
import com.yandex.direct.api.v5.advideos.StatusEnum;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.canvas.client.model.video.VideoUploadResponse;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.creative.service.CreativeService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.advideos.converter.AdVideosHelperConverter.DEFAULT_FIELD_NAMES;

public class AdVideosConverterTest {
    public AdVideosHelperConverter converter;

    @Autowired
    private ResultConverter resultConverter;

    private final PropertyFilter propertyFilter = new PropertyFilter();

    @Before
    public void prepare() {
        converter = new AdVideosHelperConverter(propertyFilter, resultConverter);
    }

    @Test
    public void convertVideoStatus() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(converter.convertVideoStatus(VideoUploadResponse.FileStatus.READY))
                    .isEqualTo(StatusEnum.READY);
            softly.assertThat(converter.convertVideoStatus(VideoUploadResponse.FileStatus.SEMI_READY))
                    .isEqualTo(StatusEnum.READY);
            softly.assertThat(converter.convertVideoStatus(VideoUploadResponse.FileStatus.ERROR))
                    .isEqualTo(StatusEnum.ERROR);
            softly.assertThat(converter.convertVideoStatus(VideoUploadResponse.FileStatus.CONVERTING))
                    .isEqualTo(StatusEnum.CONVERTING);
            softly.assertThat(converter.convertVideoStatus(VideoUploadResponse.FileStatus.NEW))
                    .isEqualTo(StatusEnum.NEW);
            softly.assertThat(converter.convertVideoStatus(null)).isEqualTo(null);
        });
    }

    @Test
    public void convertGetResponse() {
        GetResponse expected = adVideoGetResponse();

        var videoItems = List.of(new CreativeService.VideoItemWithStatus(5L, "60dd854d802a66ed486e6f64",
                VideoUploadResponse.FileStatus.READY));

        GetResponse response = converter.convertGetResponse(videoItems, DEFAULT_FIELD_NAMES, null);
        assertThat(response)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void convertGetSelectionCriteria() {
        var expected = new CreativeService.VideoItem(5L, "60dd854d802a66ed486e6f64");
        var selectionCriteria = new AdVideosSelectionCriteria().withIds("60dd854d802a66ed486e6f64005");
        CreativeService.VideoItem videoItem = converter.convertGetSelectionCriteria(selectionCriteria).get(0);

        assertThat(videoItem)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    private GetResponse adVideoGetResponse() {
        return new GetResponse()
                .withAdVideos(new AdVideoGetItem()
                        .withId("60dd854d802a66ed486e6f64005")
                        .withStatus(StatusEnum.READY));
    }
}

package ru.yandex.market.mbo.reactui.util;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.Video;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.reactui.dto.VideoDto;

import static org.junit.Assert.assertEquals;

public class DtoProtoConverterTest {

    @Test
    public void convertVideoDtoToProto() {
        final String url = "url";
        final String urlSource = "url_source";
        final long userId = 1L;
        final long date = 0;

        VideoDto dto = new VideoDto();
        dto.setSource(Video.VideoSource.YANDEX);
        dto.setUrl(url);
        dto.setModificationSource(ModificationSource.AUTO);
        dto.setUserId(userId);
        dto.setModificationDate(date);
        dto.setUrlSource(urlSource);

        ModelStorage.Video result = DtoProtoConverter.convert(dto);

        assertEquals(url, result.getUrl());
        assertEquals(userId, result.getUserId());
        assertEquals(date, result.getModificationDate());
        assertEquals(ModelStorage.VideoSource.YANDEX, result.getSource());
        assertEquals(ru.yandex.market.mbo.export.MboParameters.ModificationSource.AUTO, result.getModificationSource());
    }

}

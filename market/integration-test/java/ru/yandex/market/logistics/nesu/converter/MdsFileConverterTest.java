package ru.yandex.market.logistics.nesu.converter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.enums.FileExtension;
import ru.yandex.market.logistics.nesu.enums.FileType;
import ru.yandex.market.logistics.nesu.model.ModelFactory;
import ru.yandex.market.logistics.nesu.model.dto.MdsFileDto;

import static ru.yandex.market.logistics.nesu.model.ModelFactory.FILE_NAME;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.FILE_URL;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.MDS_FILE_ID;

class MdsFileConverterTest extends AbstractContextualTest {
    @Autowired
    private MdsFileConverter mdsFileConverter;

    @Test
    void convertMdsFile() {
        MdsFileDto expected = new MdsFileDto(MDS_FILE_ID, FILE_URL, FILE_NAME, FileType.FEED, FileExtension.XML);
        MdsFileDto actual = mdsFileConverter.toApi(ModelFactory.mdsFile());
        assertThatModelEquals(expected, actual);
    }
}

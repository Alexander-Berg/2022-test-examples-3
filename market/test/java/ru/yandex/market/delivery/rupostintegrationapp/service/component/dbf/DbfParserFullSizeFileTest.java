package ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;

import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class DbfParserFullSizeFileTest extends BaseTest {
    private final DbfParser parser = spy(new DbfParser());
    private static final String TEST_FILE = "/fixture/unit/yanm.dbf";
    private static final Integer TOTAL_CODES_COUNT = 13574; // кодов 13575, но уникальных 13574

    FileInputStream getFileStream() throws FileNotFoundException {
        return new FileInputStream(Objects.requireNonNull(getClass().getResource(TEST_FILE)).getFile());
    }

    @Test
    void testFullSizeDbfFile() throws FileNotFoundException {
        softly.assertThat(parser.readCodes(getFileStream())).hasSize(TOTAL_CODES_COUNT);
    }

    @Test
    void updateCodes() throws Exception {
        var codes = parser.readCodes(getFileStream());
        var file = parser.updateCodes(getFileStream(), codes);
        softly.assertThat(file).isInstanceOf(File.class);
        var updatedCodes = parser.readCodes(new FileInputStream(file));
        softly.assertThat(codes.size()).isEqualTo(updatedCodes.size());
    }
}

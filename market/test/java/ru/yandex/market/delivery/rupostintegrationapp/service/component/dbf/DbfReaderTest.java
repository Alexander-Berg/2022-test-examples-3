package ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.IntStream;

import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import steps.dbfsteps.DBFReaderSteps;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbfReaderTest extends BaseTest {
    private DbfReader reader;
    @Mock
    private DBFReader dbfReader;

    @BeforeEach
    void setUp() throws Exception {
        FileInputStream stream = new FileInputStream(
            Files.createTempFile("resource-", ".dbf").toFile()
        );

        reader = spy(new DbfReader(stream));

        doReturn(dbfReader).when(reader).makeReader();
    }

    @AfterEach
    void tearDown() {
        verify(reader).makeReader();
    }

    @Test
    void read() {
        List<DBFField> fields = DBFReaderSteps.createFields();
        List<Object[]> rows = DBFReaderSteps.createRows();
        when(dbfReader.getFieldCount())
            .thenReturn(fields.size());

        IntStream.range(0, fields.size())
            .forEach(
                i -> when(dbfReader.getField(eq(i))).thenReturn(fields.get(i))
            );

        final List<Object[]> readList = DBFReaderSteps.createRows();

        Answer answerFromList = invocation -> {
            // Remove and return first element
            if (!readList.isEmpty()) {
                return readList.remove(0);
            }
            return null;
        };

        when(dbfReader.nextRecord()).then(answerFromList);

        reader.read();

        softly.assertThat(reader.getFields()).hasSize(fields.size());
        softly.assertThat(reader.getRows()).hasSize(rows.size());
        softly.assertThat(reader.getFields()).isEqualTo(fields);

        IntStream.range(0, reader.getRows().size())
            .forEach(
                i -> softly.assertThat(reader.getRows().get(i)).isEqualTo(rows.get(i))
            );
    }

    @Test
    void readFail() {
        doThrow(new DBFException("test")).when(dbfReader).getFieldCount();

        softly.assertThatThrownBy(() -> reader.read()).isInstanceOf(DBFException.class);
    }
}

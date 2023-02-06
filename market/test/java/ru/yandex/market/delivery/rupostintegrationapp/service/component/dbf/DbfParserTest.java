package ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import steps.dbfsteps.DBFReaderSteps;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.DbfCode;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.enums.DbfOrderDeliveryType;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbfParserTest extends BaseTest {
    private static final String ORDERID1 = "123123";
    private static final String ORDERID2 = "12";
    private static final Integer SERVICEID1 = 139;
    private static final Integer SERVICEID2 = 12356;
    private DbfParser parser = spy(new DbfParser());
    @Mock
    private DbfReader reader;
    @Mock
    private DBFWriter writer;
    private FileInputStream stream;

    @BeforeEach
    void setUp() throws Exception {
        doReturn(reader)
            .when(parser).makeReader(any());
        stream = new FileInputStream(
            Files.createTempFile("resource-", ".dbf").toFile()
        );
        when(reader.getFields()).thenReturn(createFields());
        when(reader.getRows()).thenReturn(createRows());
    }

    @AfterEach
    void tearDown() {
        verify(parser).makeReader(stream);
    }

    @Test
    void readCodes() {
        List<DbfCode> codes = parser.readCodes(stream);

        softly.assertThat(codes).hasSize(4);
        softly.assertThat(codes.get(0).getCode()).isEqualTo(DBFReaderSteps.RPO1);
        softly.assertThat(codes.get(0).getDbfPaySum()).isEqualTo(DBFReaderSteps.PAYSUM1);
        softly.assertThat(codes.get(1).getCode()).isEqualTo(DBFReaderSteps.RPO2);
        softly.assertThat(codes.get(1).getDbfPaySum()).isEqualTo(DBFReaderSteps.PAYSUM2);
        softly.assertThat(codes.get(2).getCode()).isEqualTo(DBFReaderSteps.RPO4);
        softly.assertThat(codes.get(2).getDbfPaySum()).isEqualTo(DBFReaderSteps.PAYSUM2);
        softly.assertThat(codes.get(3).getCode()).isEqualTo(DBFReaderSteps.RPO5);
        softly.assertThat(codes.get(3).getDbfPaySum()).isEqualTo(DBFReaderSteps.PAYSUM2);
    }

    @Test
    void updateCodes() throws Exception {
        doReturn(writer)
            .when(parser).makeWriter(any());

        List<Object[]> rows = createRows();

        softly.assertThat(parser.updateCodes(stream, createCodes())).isInstanceOf(File.class);

        verify(writer).close();
        verify(writer).setFields(any());

        ArgumentCaptor<Object[]> argumentCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(writer, times(rows.size())).addRecord(argumentCaptor.capture());

        softly.assertThat(argumentCaptor.getAllValues().size()).isEqualTo(createRows().size());

        List<String> answers = getAnswers();
        IntStream.range(0, answers.size()).forEach(i ->
            softly.assertThat(argumentCaptor.getAllValues().get(i)[0])
                .isEqualTo(answers.get(i))
        );

        verify(parser).makeWriter(any());
    }

    private List<String> getAnswers() {
        return Arrays.asList(
            DBFReaderSteps.ELM_CHECK_TRUE,
            DBFReaderSteps.ELM_CHECK_TRUE,
            "АГА",
            DBFReaderSteps.ELM_CHECK_FALSE,
            DBFReaderSteps.ELM_CHECK_FALSE
        );
    }

    private List<Object[]> createRows() {
        return DBFReaderSteps.createRows();
    }

    private List<DBFField> createFields() {
        return DBFReaderSteps.createFields();
    }

    private List<DbfCode> createCodes() {
        ArrayList<List<Object>> data = new ArrayList();
        data.add(
            Arrays.asList(
                DBFReaderSteps.RPO1,
                DBFReaderSteps.PAYSUM1,
                ORDERID1,
                SERVICEID1,
                DbfOrderDeliveryType.CARD_ON_DELIVERY
            )
        );
        data.add(
            Arrays.asList(
                DBFReaderSteps.RPO2,
                DBFReaderSteps.PAYSUM2,
                ORDERID2,
                SERVICEID2,
                DbfOrderDeliveryType.CASH_ON_DELIVERY
            )
        );
        data.add(
            Arrays.asList(
                DBFReaderSteps.RPO3,
                DBFReaderSteps.PAYSUM3,
                null,
                SERVICEID2,
                DbfOrderDeliveryType.CASH_ON_DELIVERY
            )
        );
        data.add(
            Arrays.asList(
                DBFReaderSteps.RPO4,
                DBFReaderSteps.PAYSUM3,
                ORDERID2,
                null,
                DbfOrderDeliveryType.CASH_ON_DELIVERY
            )
        );
        data.add(
            Arrays.asList(
                DBFReaderSteps.RPO5,
                DBFReaderSteps.PAYSUM3,
                ORDERID2,
                SERVICEID2,
                DbfOrderDeliveryType.APPLE_PAY
            )
        );

        List<DbfCode> codes = new ArrayList<>();

        data.forEach(list -> {
            final DbfCode code = new DbfCode();
            code.setCode((String) list.get(0));
            code.setDbfPaySum((Double) list.get(1));
            Optional.ofNullable(list.get(2)).ifPresent(t -> {
                code.setOrderId((String) t);
            });
            Optional.ofNullable(list.get(3)).ifPresent(t -> {
                code.setServiceId((Integer) t);
            });
            code.setDbfOrderDeliveryType((DbfOrderDeliveryType) list.get(4));
            codes.add(code);
        });
        return codes;
    }
}

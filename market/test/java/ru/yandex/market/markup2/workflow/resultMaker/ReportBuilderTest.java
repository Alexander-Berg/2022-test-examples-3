package ru.yandex.market.markup2.workflow.resultMaker;

import com.googlecode.protobuf.format.JsonFormat;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;

/**
 * @author galaev@yandex-team.ru
 * @since 25/10/2017.
 */
public class ReportBuilderTest {

    @Test
    public void testParsingStatus() throws IOException {
        AbstractReportBuilder reportBuilder = Mockito.mock(AbstractReportBuilder.class, Mockito.CALLS_REAL_METHODS);

        ModelStorage.OperationStatus status = ModelStorage.OperationStatus.newBuilder()
            .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
            .setType(ModelStorage.OperationType.UPLOAD_IMAGE)
            .setModelId(1)
            .addValidationError(ModelStorage.ValidationError.newBuilder()
                .setType(ModelStorage.ValidationErrorType.DUPLICATE_NAME)
                .addData("test data")
                .build())
            .build();

        String statusString = JsonFormat.printToString(status);
        ModelStorage.OperationStatus statusCopy = reportBuilder.parseStatus(statusString);

        Assert.assertEquals(status, statusCopy);
    }
}

package ru.yandex.direct.web.entity.excel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.mock.web.MockMultipartFile;

import static com.google.common.base.Preconditions.checkArgument;
import static ru.yandex.direct.web.entity.excel.service.validation.ExcelConstraints.XLS_CONTENT_TYPE;

@ParametersAreNonnullByDefault
public class ExcelTestUtils {

    private ExcelTestUtils() {
    }

    public static MockMultipartFile createMockExcelFile(byte[] bytes) {
        return createMockExcelFile("someFile", new ByteArrayInputStream(bytes));
    }

    public static MockMultipartFile createMockExcelFile(InputStream inputStream) {
        return createMockExcelFile("someFile", inputStream);
    }

    public static MockMultipartFile createMockExcelFile(String fileName, @Nullable InputStream inputStream) {
        checkArgument(inputStream != null, "inputStream must not be null");
        try {
            return new MockMultipartFile(fileName, "", XLS_CONTENT_TYPE, inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

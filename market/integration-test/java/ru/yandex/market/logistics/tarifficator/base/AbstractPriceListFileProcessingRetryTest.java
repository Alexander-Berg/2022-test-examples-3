package ru.yandex.market.logistics.tarifficator.base;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.jobs.producer.ProcessUploadedPriceListProducer;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;
import ru.yandex.market.logistics.tarifficator.util.ValidationUtil;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractPriceListFileProcessingRetryTest extends AbstractContextualTest {
    @Autowired
    private ProcessUploadedPriceListProducer processUploadedPriceListProducer;

    @BeforeEach
    void setUp() {
        doNothing().when(processUploadedPriceListProducer).produceTasks(anyLong());
    }

    @Test
    @DisplayName("Перевыставить файл на процессинг успешно")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-processed-with-critical-errors.xml")
    void retryPriceLitFileSuccess() throws Exception {
        retryPriceListFileProcessing(1)
            .andExpect(status().isOk())
            .andExpect(TestUtils.noContent());

        verify(processUploadedPriceListProducer).produceTasks(1);
    }

    @Test
    @DisplayName("Перевыставить файл на процессинг — файл не существует")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-processed-with-critical-errors.xml")
    void retryPriceLitFileNotFound() throws Exception {
        retryPriceListFileProcessing(2)
            .andExpect(status().isNotFound())
            .andExpect(ValidationUtil.errorMessage("Failed to find [PRICE_LIST_FILE] with ids [[2]]"));

        verifyNoMoreInteractions(processUploadedPriceListProducer);
    }

    @Test
    @DisplayName("Перевыставить файл на процессинг — файл в неправильном статусе")
    @DatabaseSetup("/controller/price-list-files/db/before/price-list-files.xml")
    void retryPriceLitFileInWrongStatus() throws Exception {
        retryPriceListFileProcessing(2)
            .andExpect(status().isBadRequest())
            .andExpect(ValidationUtil.errorMessage(
                "Wrong file processing status SUCCESS. Only [ERROR] allowed for processing retry"
            ));

        verifyNoMoreInteractions(processUploadedPriceListProducer);
    }

    protected abstract ResultActions retryPriceListFileProcessing(long id) throws Exception;
}

package ru.yandex.market.ff.tms;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.service.UploadErrorService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

/**
 * Интеграционный тест для {@link RemoveExpiredUploadErrorsExecutor}.
 *
 * @author avetokhin 16/02/18.
 */
class RemoveExpiredUploadErrorsExecutorTest extends IntegrationTest {

    @Autowired
    private UploadErrorService uploadErrorService;

    @Test
    @DatabaseSetup("classpath:tms/upload-error/before-delete.xml")
    @ExpectedDatabase(value = "classpath:tms/upload-error/after-delete.xml", assertionMode = NON_STRICT)
    void execute() {
        new RemoveExpiredUploadErrorsExecutor(uploadErrorService).doJob(null);
    }

}

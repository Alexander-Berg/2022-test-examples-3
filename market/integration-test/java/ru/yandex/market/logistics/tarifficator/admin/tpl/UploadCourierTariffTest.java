package ru.yandex.market.logistics.tarifficator.admin.tpl;

import javax.annotation.Nonnull;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import ru.yandex.market.logistics.tarifficator.base.tpl.AbstractCourierTariffUploadTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

class UploadCourierTariffTest extends AbstractCourierTariffUploadTest {
    @Nonnull
    @Override
    protected MockMultipartHttpServletRequestBuilder requestBuilder(MockMultipartFile file) {
        return multipart("/admin/tpl-courier-tariffs/upload").file(file);
    }

    @Nonnull
    @Override
    protected ResultMatcher successResult() {
        return TestUtils.noContent();
    }

    @Nonnull
    @Override
    protected String getRequestParamName() {
        return "request";
    }
}

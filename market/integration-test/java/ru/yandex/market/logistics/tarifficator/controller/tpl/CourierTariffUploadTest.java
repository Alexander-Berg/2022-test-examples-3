package ru.yandex.market.logistics.tarifficator.controller.tpl;

import javax.annotation.Nonnull;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import ru.yandex.market.logistics.tarifficator.base.tpl.AbstractCourierTariffUploadTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

public class CourierTariffUploadTest extends AbstractCourierTariffUploadTest {
    @Nonnull
    protected MockMultipartHttpServletRequestBuilder requestBuilder(MockMultipartFile file) {
        return multipart("/tpl-courier-tariff/upload").file(file);
    }

    @Nonnull
    @Override
    protected ResultMatcher successResult() {
        return jsonContent("controller/tpl/tariffs/response/success_upload.json");
    }

    @Nonnull
    @Override
    protected String getRequestParamName() {
        return "file";
    }
}

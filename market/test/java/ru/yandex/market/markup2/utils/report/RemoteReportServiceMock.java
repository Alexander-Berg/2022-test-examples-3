package ru.yandex.market.markup2.utils.report;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.IOException;
import java.net.URL;

class RemoteReportServiceMock {
    private static final String REPORT_GET_SERP_RESPONSE_JSON = "report_get_serp_response.json";
    private static final String REPORT_FIND_MODEL_RESPONSE_JSON = "report_find_model_response.json";
    private static final String REPORT_FIND_MODEL_RESPONSE_NO_PICS_JSON = "report_find_model_response_no_pics.json";
    private static final String REPORT_FIND_PICTURE_URLS_JSON = "report_find_picture_urls_response.json";

    RemoteReportServiceMock() {
    }

    String getSerp() throws IOException {
        return readJson(REPORT_GET_SERP_RESPONSE_JSON);
    }

    String getFindModels() throws IOException {
        return readJson(REPORT_FIND_MODEL_RESPONSE_JSON);
    }

    String getFindModelsNoPics() throws IOException {
        return readJson(REPORT_FIND_MODEL_RESPONSE_NO_PICS_JSON);
    }

    String getFindPicUrls() throws IOException {
        return readJson(REPORT_FIND_PICTURE_URLS_JSON);
    }

    private String readJson(String fileName) throws IOException {
        URL resource = Resources.getResource(fileName);
        return Resources.toString(resource, Charsets.UTF_8);
    }
}

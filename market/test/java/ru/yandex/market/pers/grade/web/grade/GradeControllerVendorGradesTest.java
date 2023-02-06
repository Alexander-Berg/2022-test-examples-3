package ru.yandex.market.pers.grade.web.grade;

import org.junit.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author varvara
 * 21.11.2018
 */
public class GradeControllerVendorGradesTest extends GradeControllerBaseTest {

    @Test
    public void testVendorGradesCountOk() throws Exception {
        invokeAndRetrieveResponse(
            get("/api/grade/public/vendor/count?vendorId=1"),
            status().is2xxSuccessful());
    }

    @Test
    public void testVendorGradesOk() throws Exception {
        invokeAndRetrieveResponse(
            get("/api/grade/public/vendor/pager?vendorId=1"),
            status().is2xxSuccessful());
    }

    @Test
    public void testVendorGradesDateOk() throws Exception {
        invokeAndRetrieveResponse(
            get("/api/grade/public/vendor/pager?vendorId=1&dateFrom=01.01.2017&dateTo=01.01.2017"),
            status().is2xxSuccessful());
    }

    @Test
    public void testVendorGradesBadGr0() throws Exception {
        invokeAndRetrieveResponse(
            get("/api/grade/public/vendor/pager?vendorId=1&gr0=-1"),
            status().is4xxClientError());
    }

    @Test
    public void testVendorGradesBadPageNum() throws Exception {
        invokeAndRetrieveResponse(
            get("/api/grade/public/vendor/pager?vendorId=1&page_num=-1"),
            status().is4xxClientError());
    }

    @Test
    public void testVendorGradesBadPageSize() throws Exception {
        invokeAndRetrieveResponse(
            get("/api/grade/public/vendor/pager?vendorId=1&page_size=-1"),
            status().is4xxClientError());
    }

}

//package ru.yandex.market.robot.server.services;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.mockito.junit.MockitoJUnitRunner;
//import org.springframework.mock.web.MockHttpServletRequest;
//import org.springframework.mock.web.MockHttpServletResponse;
//import ru.yandex.market.ir.http.PartnerContent;
//
//import javax.servlet.ServletException;
//import java.io.IOException;
//
//import static org.junit.Assert.assertEquals;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyBoolean;
//import static org.mockito.ArgumentMatchers.anyInt;
//import static org.mockito.Mockito.doThrow;
//
//class MockHttpServletResponseNothingOnNullHeader extends MockHttpServletResponse {
//
//    @Override
//    public void setHeader(String name, String value) {
//        if (value != null) {
//            super.setHeader(name, value);
//        }
//    }
//
//
//}
//
//
//@RunWith(MockitoJUnitRunner.StrictStubs.class)
//public class ModelsServletTest {
//    ModelsServlet modelsServlet;
//
//    @Mock
//    ExcelModelsConverter excelModelsConverter;
//
//    @Before
//    public void setUp() {
//        MockitoAnnotations.initMocks(this);
//        ExcelGenerationException exception = new ExcelGenerationException("Excel generation exception. Type: "
//            + PartnerContent.GetFileTemplateResponse.GenerationStatus.INTERNAL_ERROR + ". Message: "
//            + "INTERNAL ERROR");
//
//        doThrow(exception).when(excelModelsConverter).writeSimpleExcel(anyInt(), any(), any(), anyBoolean());
//        modelsServlet = new ModelsServlet();
//        modelsServlet.setExcelModelsConverter(excelModelsConverter);
//    }
//
//    @Test
//    public void doGet() throws ServletException, IOException {
//        MockHttpServletRequest req = new MockHttpServletRequest();
//        req.setParameter("category_id", "91491");
//        req.setParameter("mode", "simple");
//        MockHttpServletResponse resp = new MockHttpServletResponseNothingOnNullHeader();
//        modelsServlet.doGet(req, resp);
//        assertEquals("Excel generation exception. Type: INTERNAL_ERROR. Message: INTERNAL ERROR",
//            resp.getContentAsString());
//    }
//}

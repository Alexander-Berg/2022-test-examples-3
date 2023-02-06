package ru.yandex.market.mbo.handlers.vendoroffice;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.vendor.GlobalVendorDBMock;
import ru.yandex.market.mbo.db.vendor.GlobalVendorLoaderService;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.db.vendor.GlobalVendorUtilDB;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.handlers.RequestMarker;

/**
 * @author ayratgdl
 * @date 27.04.18
 */
public class UpdateVendorFromVendorOfficeHandlerTest {
    private static final long VENDOR_OFFICE_UID = 1;
    private static final int FOUNDATION_YEAR = 1950;

    private UpdateVendorFromVendorOfficeHandler handler;
    private GlobalVendorService vendorService;

    @Before
    public void setUp() {
        GlobalVendorLoaderService globalVendorLoaderService
                = new GlobalVendorLoaderService();
        vendorService = new GlobalVendorService();
        globalVendorLoaderService.setVendorDb(new GlobalVendorDBMock());
        globalVendorLoaderService.setVendorDBUtil(Mockito.mock(GlobalVendorUtilDB.class));
        vendorService.setVendorDb(new GlobalVendorDBMock());
        vendorService.setVendorDBUtil(Mockito.mock(GlobalVendorUtilDB.class));
        vendorService.setGlobalVendorLoaderService(globalVendorLoaderService);

        handler = new UpdateVendorFromVendorOfficeHandler();
        handler.setVendorService(vendorService);
        handler.setVendorOfficeUid(VENDOR_OFFICE_UID);
        handler.setRequestMarker(Mockito.mock(RequestMarker.class));
    }

    @Test
    public void minUpdateVendor() throws IOException, ServletException {
        GlobalVendor vendor = new GlobalVendor();
        vendor.addName(Language.RUSSIAN.getId(), "Brand Name");
        long vendorId = vendorService.createVendor(vendor, 1);

        String requestJson = "{\n" +
            "  \"request\": {\n" +
            "    \"brandId\": " + vendorId + ",\n" +
            "    \"name\": \"New Brand Name\",\n" +
            "  }\n" +
            "}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(requestJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.doPost(request, response);

        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        Assert.assertEquals("{}", response.getContentAsString());

        GlobalVendor expectedVendor = new GlobalVendor();
        expectedVendor.setId(vendorId);
        expectedVendor.setNames(Arrays.asList(
            new Word(vendorId, Language.RUSSIAN.getId(), "New Brand Name", false))
        );
        expectedVendor.setAliases(Arrays.asList(
            new Word(vendorId, Language.RUSSIAN.getId(), "Brand Name", false))
        );
        expectedVendor.setPublished(true);
        Assert.assertEquals(expectedVendor, vendorService.loadVendor(vendorId));
    }

    @Test
    public void throwOperationException() throws IOException, ServletException {
        GlobalVendorService throwExceptionService = Mockito.mock(GlobalVendorService.class, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new OperationException("Dummy exception");
            }
        });

        handler.setVendorService(throwExceptionService);

        GlobalVendor vendor = new GlobalVendor();
        vendor.addName(Language.RUSSIAN.getId(), "Brand Name");
        long vendorId = vendorService.createVendor(vendor, 1);

        String requestJson = "{\n" +
            "  \"request\": {\n" +
            "    \"brandId\": " + vendorId + ",\n" +
            "    \"name\": \"New Brand Name\",\n" +
            "    \"description\": \"Description\",\n" +
            "    \"country\": \"Russia\",\n" +
            "    \"foundationYear\": 1950\n" +
            "  }\n" +
            "}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(requestJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.doPost(request, response);

        Assert.assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        Assert.assertEquals("{\"status\": 500, \"type\" : \"INTERNAL_ERROR\", \"message\": \"Dummy exception\"}",
            response.getContentAsString());
    }

    @Test
    public void updateVendor() throws IOException, ServletException {
        GlobalVendor vendor = new GlobalVendor();
        vendor.addName(Language.RUSSIAN.getId(), "Brand Name");
        long vendorId = vendorService.createVendor(vendor, 1);

        String requestJson = "{\n" +
            "  \"request\": {\n" +
            "    \"brandId\": " + vendorId + ",\n" +
            "    \"name\": \"New Brand Name\",\n" +
            "    \"description\": \"Description\",\n" +
            "    \"country\": \"Russia\",\n" +
            "    \"foundationYear\": 1950\n" +
            "  }\n" +
            "}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(requestJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.doPost(request, response);

        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        Assert.assertEquals("{}", response.getContentAsString());

        GlobalVendor expectedVendor = new GlobalVendor();
        expectedVendor.setId(vendorId);
        expectedVendor.setNames(Arrays.asList(
            new Word(vendorId, Language.RUSSIAN.getId(), "New Brand Name", false))
        );
        expectedVendor.setAliases(Arrays.asList(
            new Word(vendorId, Language.RUSSIAN.getId(), "Brand Name", false))
        );
        expectedVendor.setDescription("Description");
        expectedVendor.setCountry("Russia");
        expectedVendor.setFoundationYear(FOUNDATION_YEAR);
        expectedVendor.setPublished(true);
        Assert.assertEquals(expectedVendor, vendorService.loadVendor(vendorId));
    }

    @Test
    public void updateAndAutomaticallyPublishVendor() throws IOException, ServletException {
        GlobalVendor vendor = new GlobalVendor();
        vendor.addName(Language.RUSSIAN.getId(), "Brand Name");
        vendor.setPublished(false);
        long vendorId = vendorService.createVendor(vendor, 1);

        String requestJson = "{\n" +
            "  \"request\": {\n" +
            "    \"brandId\": " + vendorId + ",\n" +
            "    \"name\": \"New Brand Name\",\n" +
            "    \"description\": \"Description\",\n" +
            "    \"country\": \"Russia\",\n" +
            "    \"foundationYear\": 1950\n" +
            "  }\n" +
            "}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(requestJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.doPost(request, response);

        Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        Assert.assertEquals("{}", response.getContentAsString());

        GlobalVendor expectedVendor = new GlobalVendor();
        expectedVendor.setId(vendorId);
        expectedVendor.setNames(Arrays.asList(
            new Word(vendorId, Language.RUSSIAN.getId(), "New Brand Name", false))
        );
        expectedVendor.setAliases(Arrays.asList(
            new Word(vendorId, Language.RUSSIAN.getId(), "Brand Name", false))
        );
        expectedVendor.setDescription("Description");
        expectedVendor.setCountry("Russia");
        expectedVendor.setFoundationYear(FOUNDATION_YEAR);
        expectedVendor.setPublished(true);
        Assert.assertEquals(expectedVendor, vendorService.loadVendor(vendorId));
    }

    @Test
    public void updateNotExistVendor() throws IOException, ServletException {
        String requestJson = "{\n" +
            "  \"request\": {\n" +
            "    \"brandId\": 100,\n" +
            "    \"name\": Brand Name,\n" +
            "  }\n" +
            "}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(requestJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.doPost(request, response);

        String responseJson = "{\"status\": 404, \"type\" : \"BRAND_NOT_FOUND\"," +
            " \"message\": \"brand with id 100 not found\"}";
        Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        Assert.assertEquals(responseJson, response.getContentAsString());
    }

    @Test
    public void badRequest() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("aaa".getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.doPost(request, response);

        String responseJson = "{\"status\": 400, \"type\" : \"BAD_REQUEST\"," +
            " \"message\": \"request body contains bad json\"}";
        Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        Assert.assertEquals(responseJson, response.getContentAsString());
    }

    @Test
    public void absentBrandId() throws IOException, ServletException {
        String requestJson = "{\n" +
            "  \"request\": {\n" +
            "  }\n" +
            "}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(requestJson.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.doPost(request, response);

        String responseJson = "{\"status\": 400, \"type\" : \"BAD_PARAM\"," +
            " \"message\": \"brandId is missing\", \"details\": [{\"brandId\": \"MISSING\"}]}";
        Assert.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        Assert.assertEquals(responseJson, response.getContentAsString());
    }
}

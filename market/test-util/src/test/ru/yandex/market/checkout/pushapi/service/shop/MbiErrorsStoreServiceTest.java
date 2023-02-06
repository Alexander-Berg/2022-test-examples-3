package ru.yandex.market.checkout.pushapi.service.shop;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.jdbc.JdbcTestUtils;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.shop.HttpBodies;
import ru.yandex.market.checkout.pushapi.shop.ShopApiResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.*;

public class MbiErrorsStoreServiceTest {

    private MbiErrorsStoreService mbiErrorsStoreService = new MbiErrorsStoreService();
    private JdbcTemplate jdbcTemplate;
    
    private long shopId = anyLong();
    private String message = anyString();
    private ErrorSubCode errorSubCode = any(ErrorSubCode.class);
    private boolean sandbox = anyBoolean();
    private String request = anyString();
    private ShopApiResponse response = ShopApiResponse.fromException(null)
        .populateBodies(new MockHttpBodies(anyString(), anyString(), anyString(), anyString()))
        .setHost(anyString())
        .setResponseTime(anyLong())
        .setUrl(anyString())
        .setArgs(anyString());

    @Before
    public void setUp() throws Exception {
        jdbcTemplate = createJdbcTemplate();

        mbiErrorsStoreService.setJdbcTemplate(jdbcTemplate);
        mbiErrorsStoreService.setExecutorService(
            new ScheduledThreadPoolExecutor(1) {
                @Override
                public ScheduledFuture<?> scheduleWithFixedDelay(
                    Runnable command, long initialDelay, long delay, TimeUnit unit
                ) {
                    command.run();
                    return null;
                }
            }
        );
    }

    private JdbcTemplate createJdbcTemplate() {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate();
        final BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl("jdbc:h2:mem:");
        jdbcTemplate.setDataSource(dataSource);
        jdbcTemplate.afterPropertiesSet();

        jdbcTemplate.execute("CREATE SCHEMA shops_web;");
        jdbcTemplate.execute(
            "CREATE TABLE shops_web.pushapi_log (\n" +
                "    \"ID\" NUMBER PRIMARY KEY AUTO_INCREMENT NOT NULL,\n" +
                "    \"SHOP_ID\" NUMBER NOT NULL,\n" +
                "    \"SUCCESS\" NUMBER(1,0) NOT NULL,\n" +
                "    \"SANDBOX\" NUMBER(1,0) NOT NULL,\n" +
                "    \"REQUEST\" VARCHAR2(1000) NOT NULL,\n" +
                "    \"URL\" VARCHAR2(1000) NOT NULL,\n" +
                "    \"ARGS\" VARCHAR2(1000),\n" +
                "    \"RESPONSE_TIME\" NUMBER NOT NULL,\n" +
                "    \"EVENTTIME\" timestamp(3) NOT NULL,\n" +
                "    \"TRANTIME\" timestamp(3) default sysdate not null,\n" +
                "    \"HOST\" VARCHAR(1000) NOT NULL,\n" +
                "    \"REQUEST_HEADERS\"    CLOB,\n" +
                "    \"REQUEST_BODY\"       CLOB,\n" +
                "    \"RESPONSE_HEADERS\"   CLOB,\n" +
                "    \"RESPONSE_BODY\"      CLOB,\n" +
                "    \"RESPONSE_ERROR\"     VARCHAR2(1000),\n" +
                "    \"RESPONSE_SUB_ERROR\" VARCHAR2(1000),\n" +
                "    \"ERROR_DESCRIPTION\"  VARCHAR2(1000)\n" +
                ");\n"
        );
        return jdbcTemplate;
    }

    private void storeError() throws Exception {
        mbiErrorsStoreService.storeError(shopId, request, errorSubCode, message, sandbox, response);
        mbiErrorsStoreService.init();
    }

    private void storeSuccess() throws Exception {
        mbiErrorsStoreService.storeSuccess(shopId, request, sandbox, response);
        mbiErrorsStoreService.init();
    }

    private void assertOneRowInserted() {
        assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "shops_web.pushapi_log"));
    }

    private <T> T extractLastRowField(final String fieldName, final Class<T> clazz) {
        return jdbcTemplate.queryForObject(
            "SELECT * FROM shops_web.pushapi_log ORDER BY id DESC LIMIT 1",
            new RowMapper<T>() {
                @Override
                public T mapRow(ResultSet resultSet, int i) throws SQLException {
                    if(clazz.equals(Integer.class)) {
                        return (T) Integer.valueOf(resultSet.getInt(fieldName));
                    } else if(clazz.equals(String.class)) {
                        return (T) resultSet.getString(fieldName);
                    } else if(clazz.equals(Long.class)) {
                        return (T) Long.valueOf(resultSet.getLong(fieldName));
                    } else if(clazz.equals(Timestamp.class)) {
                        return (T) resultSet.getTimestamp(fieldName);
                    }
                    throw new RuntimeException("unknown field type " + clazz);
                }
            }
        );
    }

    @Test
    public void testStoreErrorInsertsOneRow() throws Exception {
        storeError();
        assertOneRowInserted();
    }

    @Test
    public void testStoreSuccessInsertsOneRow() throws Exception {
        storeSuccess();
        assertOneRowInserted();
    }

    @Test
    public void testStoreErrorInsertsZeroInSuccessField() throws Exception {
        storeError();
        assertEquals(0, extractLastRowField("success", Integer.class).intValue());
    }

    @Test
    public void testStoreSuccessInsertsOneToSuccessField() throws Exception {
        storeSuccess();
        assertEquals(1, extractLastRowField("success", Integer.class).intValue());
    }

    @Test
    public void testStoreSuccessDoesntInsertsErrorCodes() throws Exception {
        storeSuccess();
        assertNull(extractLastRowField("response_error", String.class));
        assertNull(extractLastRowField("response_sub_error", String.class));
    }

    @Test
    public void testStoreSuccessInsertsRightShopId() throws Exception {
        shopId = 12345l;
        storeSuccess();
        assertEquals(shopId, extractLastRowField("shop_id", Long.class).longValue());
    }

    @Test
    public void testStoreErrorSuccessInsertsRightShopId() throws Exception {
        shopId = 12345l;
        storeError();
        assertEquals(shopId, extractLastRowField("shop_id", Long.class).longValue());
    }

    @Test
    public void testStoreErrorInsertsTrueSandboxValue() throws Exception {
        sandbox = true;
        storeError();
        assertEquals(1, extractLastRowField("sandbox", Integer.class).intValue());
    }

    @Test
    public void testStoreSuccessInsertsTrueSandboxValue() throws Exception {
        sandbox = true;
        storeSuccess();
        assertEquals(1, extractLastRowField("sandbox", Integer.class).intValue());
    }

    @Test
    public void testStoreErrorInsertsFalseSandboxValue() throws Exception {
        sandbox = false;
        storeError();
        assertEquals(0, extractLastRowField("sandbox", Integer.class).intValue());
    }

    @Test
    public void testStoreSuccessInsertsFalseSandboxValue() throws Exception {
        sandbox = false;
        storeSuccess();
        assertEquals(0, extractLastRowField("sandbox", Integer.class).intValue());
    }

    @Test
    public void testStoreErrorInsertsRightRequestValue() throws Exception {
        request = "/cart";
        storeError();
        assertEquals(request, extractLastRowField("request", String.class));
    }

    @Test
    public void testStoreSuccessInsertsRightRequestValue() throws Exception {
        request = "/cart";
        storeSuccess();
        assertEquals(request, extractLastRowField("request", String.class));
    }

    @Test
    public void testStoreErrorInsertsRightUrl() throws Exception {
        final String url = "http://blah";
        response.setUrl(url);
        storeError();
        assertEquals(url, extractLastRowField("url", String.class));
    }

    @Test
    public void testStoreSuccessInsertsRightUrl() throws Exception {
        final String url = "http://blah";
        response.setUrl(url);
        storeSuccess();
        assertEquals(url, extractLastRowField("url", String.class));
    }

    @Test
    public void testStoreErrorInsertsRightArgs() throws Exception {
        final String args = "arg=val&arg1=val1";
        response.setArgs(args);
        storeError();
        assertEquals(args, extractLastRowField("args", String.class));
    }

    @Test
    public void testStoreSuccessInsertsRightArgs() throws Exception {
        final String args = "arg=val&arg1=val1";
        response.setArgs(args);
        storeSuccess();
        assertEquals(args, extractLastRowField("args", String.class));
    }

    @Test
    public void testStoreErrorInsertsRightResponseTime() throws Exception {
        final long responseTime = 1234l;
        response.setResponseTime(responseTime);
        storeError();
        assertEquals(responseTime, extractLastRowField("response_time", Long.class).longValue());
    }

    @Test
    public void testStoreSuccessInsertsRightResponseTime() throws Exception {
        final long responseTime = 1234l;
        response.setResponseTime(responseTime);
        storeSuccess();
        assertEquals(responseTime, extractLastRowField("response_time", Long.class).longValue());
    }

    @Test
    public void testStoreSuccessInsertsRightBodies() throws Exception {
        final MockHttpBodies bodies = new MockHttpBodies("reqHead", "reqBody", "resHead", "resBody");
        response.populateBodies(bodies);
        storeSuccess();
        assertEquals(bodies.requestHeaders, extractLastRowField("request_headers", String.class));
        assertEquals(bodies.requestBody, extractLastRowField("request_body", String.class));
        assertEquals(bodies.responseHeaders, extractLastRowField("response_headers", String.class));
        assertEquals(bodies.responseBody, extractLastRowField("response_body", String.class));
    }

    @Test
    public void testStoreErrorInsertsRightBodies() throws Exception {
        final MockHttpBodies bodies = new MockHttpBodies("reqHead", "reqBody", "resHead", "resBody");
        response.populateBodies(bodies);
        storeError();
        assertEquals(bodies.requestHeaders, extractLastRowField("request_headers", String.class));
        assertEquals(bodies.requestBody, extractLastRowField("request_body", String.class));
        assertEquals(bodies.responseHeaders, extractLastRowField("response_headers", String.class));
        assertEquals(bodies.responseBody, extractLastRowField("response_body", String.class));
    }

    @Test
    public void testStoreErrorInsertsRightResponseErrorAndSubError() throws Exception {
        errorSubCode = ErrorSubCode.CANT_PARSE_RESPONSE;
        storeError();
        assertEquals(errorSubCode.toString(), extractLastRowField("response_sub_error", String.class));
        assertEquals(errorSubCode.getParent().toString(), extractLastRowField("response_error", String.class));
    }

    @Test
    public void testStoreErrorStoresRightErrorDescription() throws Exception {
        message = "blah blah";
        storeError();
        assertEquals(message, extractLastRowField("error_description", String.class));
    }

    @Test
    public void testStoreErrorInsertsRightHost() throws Exception {
        final String host = "kremlin.ru";
        response.setHost(host);
        storeError();
        assertEquals(host, extractLastRowField("host", String.class));
    }

    @Test
    public void testStoreSuccessInsertsRightHost() throws Exception {
        final String host = "kremlin.ru";
        response.setHost(host);
        storeSuccess();
        assertEquals(host, extractLastRowField("host", String.class));
    }

    @Test
    public void testStoreSuccessInsertsNotNullEventtime() throws Exception {
        storeSuccess();
        assertNotNull(extractLastRowField("eventtime", Timestamp.class));
    }

    @Test
    public void testStoreErrorInsertsNotNullEventtime() throws Exception {
        storeError();
        assertNotNull(extractLastRowField("eventtime", Timestamp.class));
    }

    @Test
    public void testStoreSuccessInsertsNotNullTrantime() throws Exception {
        storeSuccess();
        assertNotNull(extractLastRowField("trantime", Timestamp.class));
    }

    @Test
    public void testStoreErrorInsertsNotNullTrantime() throws Exception {
        storeError();
        assertNotNull(extractLastRowField("trantime", Timestamp.class));
    }

    private class MockHttpBodies extends HttpBodies {

        private String requestHeaders;
        private String requestBody;
        private String responseHeaders;
        private String responseBody;

        private MockHttpBodies(String requestHeaders, String requestBody, String responseHeaders, String responseBody) {
            this.requestHeaders = requestHeaders;
            this.requestBody = requestBody;
            this.responseHeaders = responseHeaders;
            this.responseBody = responseBody;
        }

        private ByteArrayOutputStream baos(String str) {
            try {
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byteArrayOutputStream.write(str.getBytes());
                return byteArrayOutputStream;
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ByteArrayOutputStream getRequestHeaders() {
            return baos(requestHeaders);
        }

        @Override
        public ByteArrayOutputStream getRequestBody() {
            return baos(requestBody);
        }

        @Override
        public ByteArrayOutputStream getResponseHeaders() {
            return baos(responseHeaders);
        }

        @Override
        public ByteArrayOutputStream getResponseBody() {
            return baos(responseBody);
        }
    }
}

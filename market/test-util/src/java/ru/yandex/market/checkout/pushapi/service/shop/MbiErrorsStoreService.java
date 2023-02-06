package ru.yandex.market.checkout.pushapi.service.shop;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.shop.ShopApiResponse;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MbiErrorsStoreService implements ErrorsStoreService {

    private static final Logger log = Logger.getLogger(MbiErrorsStoreService.class);

    private volatile Queue<LogRecord> records = new ConcurrentLinkedQueue<>();
    private NamedParameterJdbcTemplate jdbcTemplate;
    private ScheduledExecutorService executorService;
    public static final long BUFFER_LIMIT = 100000;
    public static final int FLUSH_DELAY = 5;

    @Required
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Required
    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }


    public void init() {
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                final Queue<LogRecord> oldRecords = records;
                records = new ConcurrentLinkedQueue<>();

                try {
                    flush(oldRecords);
                } catch(Exception e) {
                    log.error("can't insert log records to db", e);
                    records.addAll(oldRecords);
                }
            }
        }, FLUSH_DELAY, FLUSH_DELAY, TimeUnit.SECONDS);
    }

    private void flush(Queue<LogRecord> oldRecords) {
        if(oldRecords.size() > 0) {
            log.info("flushing " + oldRecords.size() + " records");
            List<SqlParameterSource> batchArgs = new ArrayList<>();
            for(LogRecord record : oldRecords) {
                batchArgs.add(
                    new MapSqlParameterSource()
                        .addValue("shopId", record.shopId)
                        .addValue("user_id", record.response.getUid())
                        .addValue("success", record.success ? 1 : 0)
                        .addValue("sandbox", record.sandbox ? 1 : 0)
                        .addValue("request", record.request)
                        .addValue("url", record.response.getUrl())
                        .addValue("args", record.response.getArgs())
                        .addValue("responseTime", record.response.getResponseTime())
                        .addValue("eventtime", new Timestamp(record.eventtime.getTime()))
                        .addValue("host", record.response.getHost())
                        .addValue("requestHeaders", record.response.getRequestHeaders())
                        .addValue("requestBody", record.response.getRequestBody())
                        .addValue("responseHeaders", record.response.getResponseHeaders())
                        .addValue("responseBody", record.response.getResponseBody())
                        .addValue(
                            "responseError",
                            record.errorSubCode == null
                                ? null
                                : record.errorSubCode.getParent().toString()
                        )
                        .addValue(
                            "responseSubError",
                            record.errorSubCode == null
                                ? null
                                : record.errorSubCode.toString()
                        )
                        .addValue("errorDescription", record.message)
                        .addValue("requestMethod", record.response.getHttpMethod())
                );
            }

            jdbcTemplate.batchUpdate(
                "INSERT INTO shops_web.pushapi_log (" +
                    "   shop_id, user_id, success, sandbox, request, url, args, response_time, eventtime, host," +
                    "   request_headers, request_body, response_headers, response_body," +
                    "   response_error, response_sub_error, error_description, request_method" +
                    ") VALUES (" +
                    "   :shopId, :user_id, :success, :sandbox, :request, :url, :args, :responseTime, :eventtime, :host," +
                    "   :requestHeaders, :requestBody, :responseHeaders, :responseBody," +
                    "   :responseError, :responseSubError, :errorDescription, :requestMethod" +
                    ")",
                batchArgs.toArray(new SqlParameterSource[batchArgs.size()])
            );

            log.info("inserted " + oldRecords.size() + " records to pushapi_log");
        }
    }

    @Override
    public void storeSuccess(long shopId, String request, boolean sandbox, ShopApiResponse response) {
        final LogRecord record = new LogRecord(shopId, request, sandbox, new Date(), response);

        addRecord(record);
    }

    @Override
    public void storeError(
        long shopId, String request, ErrorSubCode errorSubCode, String message, boolean sandbox,
        ShopApiResponse response
    ) {
        final LogRecord record = new LogRecord(shopId, request, errorSubCode, message, sandbox, new Date(), response);

        addRecord(record);
    }

    private void addRecord(LogRecord record) {
        if(records.size() < BUFFER_LIMIT) {
            records.add(record);
        } else {
            log.info("skipped logRecord: " + record);
        }
    }

    private class LogRecord {
        final long shopId;
        final boolean success;
        final String request;
        final ErrorSubCode errorSubCode;
        final String message;
        final boolean sandbox;
        final ShopApiResponse response;
        final Date eventtime;

        private LogRecord(long shopId, String request, boolean sandbox, Date eventtime, ShopApiResponse response) {
            this.eventtime = eventtime;
            success = true;
            errorSubCode = null;
            message = null;

            this.shopId = shopId;
            this.request = request;
            this.sandbox = sandbox;
            this.response = response != null ? response : ShopApiResponse.fromBody(null);
        }

        private LogRecord(
            long shopId, String request, ErrorSubCode errorSubCode, String message, boolean sandbox,
            Date eventtime, ShopApiResponse response
        ) {
            this.eventtime = eventtime;
            success = false;
            this.shopId = shopId;
            this.request = request;
            this.errorSubCode = errorSubCode;
            this.message = message;
            this.sandbox = sandbox;
            this.response = response != null ? response : ShopApiResponse.fromBody(null);
        }

        @Override
        public String toString() {
            return "LogRecord{" +
                "shopId=" + shopId +
                ", success=" + success +
                ", request='" + request + '\'' +
                ", errorSubCode=" + errorSubCode +
                ", message='" + message + '\'' +
                ", sandbox=" + sandbox +
                ", response=" + response +
                ", eventtime=" + eventtime +
                "} " + super.toString();
        }
    }

}

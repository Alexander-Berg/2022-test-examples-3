package ru.yandex.market.sre.services.other;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.IteratorF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.statface.ReportConfig;
import ru.yandex.inside.statface.StatfaceClient;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.StartrekClientBuilder;
import ru.yandex.startrek.client.model.Field;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.SearchRequest;

@Ignore
public class StafaceClientTest {

    //https://oauth.yandex-team.ru/authorize?response_type=token&client_id=801af94c94e040848ebe206086a7a4e2
    private final String TOKEN = "";
    private final String STARTREK_TOKEN = "";
    private final String REPOR_TNAME = "/Market/IncidentManagement/ServiceAvailability/test_slo_report";
    //private final String REPOR_TNAME = "/Market/IncidentManagement/ServiceAvailability/slo_control_report";
    private final String REPORT_INC_NAME = "/Market/IncidentManagement/ServiceAvailability/test_slo_inc_report";

    @Test
    public void createSloReport() {
        StatfaceClient stafaceClient = new StatfaceClient(TOKEN);
        ReportConfig config = new ReportConfig();
        config.setTitle("Test eventdetecor check results");
        config.addDimension("service", ReportConfig.FieldType.STRING);
        config.addDimension("component", ReportConfig.FieldType.STRING);
        config.addDimension("component_raw", ReportConfig.FieldType.STRING);
        config.addDimension("indicator", ReportConfig.FieldType.STRING);
        config.addDimension("indicator_raw", ReportConfig.FieldType.STRING);
        config.addDimension("time_scale", ReportConfig.FieldType.STRING);

        config.addMeasure("downtime", ReportConfig.FieldType.NUMBER);
        config.addMeasure("ticket", ReportConfig.FieldType.STRING);
        config.addMeasure("issue_ticket", ReportConfig.FieldType.STRING);
        stafaceClient.createOrUpdateReport(REPOR_TNAME, StatfaceClient.Scale.MINUTE, config);
    }

    @Test
    public void deleteSloReport() {
        StatfaceClient stafaceClient = new StatfaceClient(TOKEN);
        stafaceClient.deleteReport(REPOR_TNAME);
    }

    @Test
    public void reloadSloReport() {
        deleteSloReport();
        createSloReport();
        uploadAlerts();
    }

    @Test
    public void experiment2() {
        StatfaceClient stafaceClient = new StatfaceClient(TOKEN);
        List<Map<String, Object>> data = new ArrayList<>();

        Map<String, Object> item = new HashMap<>();
        Date now = DateUtils.truncate(new Date(), Calendar.MINUTE);
        now = DateUtils.addMinutes(now, -10);
        item.put("fielddate", DateFormatUtils.format(now, getDateFormat()));
        item.put("service", "blue");
        item.put("component", "checkouter");
        item.put("indicator", "timings_95");
        item.put("time_scale", "ONE_MIN");
        item.put("duration", 100);
        item.put("ticket", "MARKETINC-111");
        data.add(item);
        stafaceClient.postData(REPOR_TNAME, StatfaceClient.Scale.MINUTE, data);

    }

    protected String getDateFormat() {

        return "yyyy-MM-dd HH:mm:00";

    }

    String getService(Issue issue) {
        if (issue.getTags().exists(tag -> tag.equals("market:service:inner"))) {
            return "Внутренний";
        }
        if (issue.getTags().exists(tag -> tag.equals("белый"))) {
            return "Белый";
        }
        if (issue.getTags().exists(tag -> tag.equals("синий"))) {
            return "Синий";
        }
        throw new RuntimeException("Unknown service " + issue.getKey());
    }

    @Test
    public void uploadAlerts() {
        final Session startrekSession = new StartrekClientBuilder()
                .uri("https://st-api.yandex-team.ru")
                .socketTimeout(30, TimeUnit.SECONDS)
                .customFields(
                        Cf.map(
                                "sreBeginTime", Field.Schema.scalar(Field.Schema.Type.DATETIME, false),
                                "sreEndTime", Field.Schema.scalar(Field.Schema.Type.DATETIME, false),
                                "vdt", Field.Schema.scalar(Field.Schema.Type.INTEGER, false),
                                "crashId", Field.Schema.scalar(Field.Schema.Type.STRING, false)
                        )
                )
                .build(STARTREK_TOKEN);
        IteratorF<Issue> issues = startrekSession.issues().find(prepareSearchRequest());

        StatfaceClient stafaceClient = new StatfaceClient(TOKEN);
        List<Map<String, Object>> data = new ArrayList<>();


        issues.forEachRemaining((issue) -> {
            try {
                String service = getService(issue);
                String component = getComponent(issue);
                String type = getType(issue);
                String scale = getScale(issue);

                String componentFormatted = formatComponent(component, service);

                Option<Instant> from = issue.get("sreBeginTime");
                Map<String, Object> item = new HashMap<>();
                item.put("fielddate", DateTimeFormat.forPattern(getDateFormat()).print(from.get()));
                item.put("service", service);
                item.put("component_raw", component);
                item.put("component", componentFormatted);
                item.put("indicator_raw", type);
                item.put("indicator", getFormattedType(type));
                item.put("time_scale", scale);
                item.put("downtime", ((Option<Integer>) issue.get("vdt")).get());
                item.put("ticket", issue.getKey());
                Option<String> issueTicket = issue.getO("crashId");
                if (issueTicket.isPresent()) {
                    item.put("issue_ticket", issueTicket.get());
                }
                data.add(item);

            } catch (RuntimeException e) {
                System.out.println("Failed to process issue " + issue.getKey() + ". " + e.getMessage() + "|" + issue.getSummary());
            }
        });
        stafaceClient.postData(REPOR_TNAME, StatfaceClient.Scale.MINUTE, data);
        //session.q
    }

    protected SearchRequest prepareIssueSearchRequest() {
        String query = "Queue: MARKETINCIDENTS AND crashId: notEmpty()  \"Sort by\": Key DESC";
        return SearchRequest.builder()
                .query(query)
                //.fields("tags,sreBeginTime,sreEndTime,vdt".split(","))
                .build();
    }

    @Test
    public void createIncidentsReport() {
        StatfaceClient stafaceClient = new StatfaceClient(TOKEN);
        ReportConfig config = new ReportConfig();
        config.setTitle("Test eventdetecor check results");
        config.addDimension("ticket", ReportConfig.FieldType.STRING);

        config.addMeasure("status", ReportConfig.FieldType.STRING);
        config.addMeasure("status_key", ReportConfig.FieldType.STRING);
        config.addMeasure("created_at", ReportConfig.FieldType.STRING);
        config.addMeasure("resolved_at", ReportConfig.FieldType.STRING);
        config.addMeasure("resolution", ReportConfig.FieldType.STRING);
        config.addMeasure("resolution_key", ReportConfig.FieldType.STRING);
        stafaceClient.createOrUpdateReport(REPORT_INC_NAME, StatfaceClient.Scale.YEAR, config);
    }

    @Test
    public void uploadIncidents() {
        final Session startrekSession = new StartrekClientBuilder()
                .uri("https://st-api.yandex-team.ru")
                .socketTimeout(30, TimeUnit.SECONDS)
                .build(STARTREK_TOKEN);
        IteratorF<Issue> issues = startrekSession.issues().find(prepareIssueSearchRequest());

        StatfaceClient stafaceClient = new StatfaceClient(TOKEN);
        List<Map<String, Object>> data = new ArrayList<>();

        issues.forEachRemaining((issue) -> {
            try {
                Map<String, Object> item = new HashMap<>();
                item.put("fielddate", DateTimeFormat.forPattern("yyyy-01-01 00:00:00").print(issue.getCreatedAt()));
                item.put("ticket", issue.getKey());
                item.put("status_key", issue.getStatus().getKey());
                item.put("status", issue.getStatus().getDisplay());
                item.put("created_at", DateTimeFormat.forPattern(getDateFormat()).print(issue.getCreatedAt()));
                if (issue.getResolution().isPresent()) {
                    item.put("resolution_key", issue.getResolution().get().getKey());
                    item.put("resolution", issue.getResolution().get().getDisplay());
                    item.put("resolved_at",
                            DateTimeFormat.forPattern(getDateFormat()).print(issue.getResolvedAt().get()));
                }
                data.add(item);
            } catch (RuntimeException e) {
                System.out.println("Failed to process issue " + issue.getKey() + ". " + e.getMessage() + "|" + issue.getSummary());
            }
        });
        stafaceClient.postData(REPORT_INC_NAME, StatfaceClient.Scale.YEAR, data);
        //session.q
    }

    private String getScale(Issue issue) {
        if (issue.getDescription().get().contains("one_min")) {
            return "one_min";
        }
        if (issue.getDescription().get().contains("five_min")) {
            return "five_min";
        }
        return "other";
    }

    private String getComponent(Issue issue) {
        Option<String> componentTag = issue.getTags().find(tag -> tag.startsWith("component"));
        if (componentTag.isPresent()) {
            return componentTag.get().substring(10);
        }
        throw new RuntimeException("Unknown component " + issue.getKey());
    }

    private String getType(Issue issue) {
        Option<String> componentTag = issue.getTags().find(tag -> tag.startsWith("type"));
        if (componentTag.isPresent()) {
            return componentTag.get().substring(5);
        }

        throw new RuntimeException("Unknown type " + issue.getKey());
    }

    private String getFormattedType(String type) {
        if (type.equals("log-err-rate") || type.equals("500-percent")) {
            return "err-rate";
        }
        return type;
    }

    protected SearchRequest prepareSearchRequest() {
        String query = "Resolution: !falseAlarm Queue: !MARKETINCIDENTS Queue: !TESTMARKETINCID   Tags: " +
                "\"market:sloDash\" \"Sort by\": Key DESC";
        return SearchRequest.builder()
                .query(query)
                .fields("tags,sreBeginTime,sreEndTime,vdt".split(","))
                .build();
    }

    @Test
    public void updateTags() {
        final Session startrekSession = new StartrekClientBuilder()
                .uri("https://st-api.yandex-team.ru")
                .socketTimeout(30, TimeUnit.SECONDS)
                .customFields(
                        Cf.map(
                                "sreBeginTime", Field.Schema.scalar(Field.Schema.Type.DATETIME, false),
                                "sreEndTime", Field.Schema.scalar(Field.Schema.Type.DATETIME, false),
                                "vdt", Field.Schema.scalar(Field.Schema.Type.INTEGER, false),
                                "crashId", Field.Schema.scalar(Field.Schema.Type.STRING, false)
                        )
                )
                .build(STARTREK_TOKEN);

        String query = "Resolution: !falseAlarm Tags: !\"market:sloDash\" Summary: !\"Нет данных\" Summary: " +
                "!\"Аномалия\"  Queue: MARKETALARMS \"Sort by\": " +
                "Key ASC";
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .fields("tags,sreBeginTime,sreEndTime,vdt".split(","))
                .build();
        IteratorF<Issue> issues = startrekSession.issues().find(request);
        issues.forEachRemaining(issue -> {
            try {
                String service = getService(issue);
                String component = getComponent(issue);
                String type = getType(issue);
                String scale = getScale(issue);

                if (component.contains("pipelines") || issue.getSummary().contains("CAPI")) {
                    return;
                }
                String componentFormatted = formatComponent(component, service);

                Option<Instant> from = issue.get("sreBeginTime");
                Map<String, Object> item = new HashMap<>();
                item.put("fielddate", DateTimeFormat.forPattern(getDateFormat()).print(from.get()));
                item.put("service", service);
                item.put("component_raw", component);
                item.put("component", componentFormatted);
                item.put("indicator_raw", type);
                item.put("indicator", getFormattedType(type));
                item.put("time_scale", scale);
                item.put("downtime", ((Option<Integer>) issue.get("vdt")).get());
                item.put("ticket", issue.getKey());
                Option<String> issueTicket = issue.getO("crashId");
                if (issueTicket.isPresent()) {
                    item.put("issue_ticket", issueTicket.get());
                }
                IssueUpdate update = IssueUpdate.builder().tags(Cf.list("market:sloDash"), Cf.list()).build();
                startrekSession.issues().update(issue, update);
                System.out.println(issue.getKey() + "|" + issue.getSummary());

            } catch (RuntimeException e) {
                System.out.println("Failed to process issue " + issue.getKey() + ". " + e.getMessage() + "|" + issue.getSummary());
            }
//            IssueUpdate update = IssueUpdate.builder().tags(Cf.list("market:eventdetector"), Cf.list()).build();
//            startrekSession.issues().update(issue, update);
//            System.out.println(issue.getKey());
        });
    }

    private String formatComponent(String component, String service) {
        String componentFormatted = component;
        if (componentFormatted.startsWith("beru") || componentFormatted.startsWith("blue")) {
            componentFormatted = componentFormatted.substring(5);
        }
        if (service.equalsIgnoreCase("Внутренний")) {
            return "inner-" + componentFormatted;
        }
        return (service.equalsIgnoreCase("Белый") ? "white-" : "blue-") + componentFormatted;
    }

    @Test
    public void updateTags2() {
        final Session startrekSession = new StartrekClientBuilder()
                .uri("https://st-api.yandex-team.ru")
                .socketTimeout(30, TimeUnit.SECONDS)
                .customFields(
                        Cf.map(
                                "sreBeginTime", Field.Schema.scalar(Field.Schema.Type.DATETIME, false),
                                "sreEndTime", Field.Schema.scalar(Field.Schema.Type.DATETIME, false),
                                "vdt", Field.Schema.scalar(Field.Schema.Type.INTEGER, false),
                                "crashId", Field.Schema.scalar(Field.Schema.Type.STRING, false)
                        )
                )
                .build(STARTREK_TOKEN);

        String query = "";
        SearchRequest request = SearchRequest.builder()
                .filterId("Resolution: !falseAlarm Queue: !MARKETINCIDENTS Queue: !TESTMARKETINCID Tags: " +
                        "\"market:sloDash\"")
                .fields("tags,sreBeginTime,sreEndTime,vdt".split(","))
                .build();
        IteratorF<Issue> issues = startrekSession.issues().find(request);

        List<Map<String, Object>> data = new ArrayList<>();
        issues.forEachRemaining(issue -> {
            try {
                String service = getService(issue);
                String component = getComponent(issue);
                String type = getType(issue);
                String scale = getScale(issue);

                if (component.contains("pipelines") || issue.getSummary().contains("CAPI")) {
                    return;
                }
                String componentFormatted = formatComponent(component, service);
                Option<Instant> from = issue.get("sreBeginTime");
                Map<String, Object> item = new HashMap<>();
                item.put("fielddate", DateTimeFormat.forPattern(getDateFormat()).print(from.get()));
                item.put("service", service);
                item.put("component_raw", component);
                item.put("component", componentFormatted);
                item.put("indicator_raw", type);
                item.put("indicator", getFormattedType(type));
                item.put("time_scale", scale);
                item.put("downtime", ((Option<Integer>) issue.get("vdt")).get());
                item.put("ticket", issue.getKey());
                Option<String> issueTicket = issue.getO("crashId");
                if (issueTicket.isPresent()) {
                    item.put("issue_ticket", issueTicket.get());
                }
                data.add(item);

            } catch (RuntimeException e) {
                //System.out.println("Failed to process issue " + issue.getKey() + ". " + e.getMessage() + "|" +
                // issue.getSummary());
            }
        });

        List<Map<String, Object>> data2 = new ArrayList<>();
        issues = startrekSession.issues().find(prepareSearchRequest());


        issues.forEachRemaining((issue) -> {
            try {
                String service = getService(issue);
                String component = getComponent(issue);
                String type = getType(issue);
                String scale = getScale(issue);

                if (component.contains("pipelines") || issue.getSummary().contains("CAPI")) {
                    return;
                }
                String componentFormatted = formatComponent(component, service);
                Option<Instant> from = issue.get("sreBeginTime");
                Map<String, Object> item = new HashMap<>();
                item.put("fielddate", DateTimeFormat.forPattern(getDateFormat()).print(from.get()));
                item.put("service", service);
                item.put("component_raw", component);
                item.put("component", componentFormatted);
                item.put("indicator_raw", type);
                item.put("indicator", getFormattedType(type));
                item.put("time_scale", scale);
                item.put("downtime", ((Option<Integer>) issue.get("vdt")).get());
                item.put("ticket", issue.getKey());
                Option<String> issueTicket = issue.getO("crashId");
                if (issueTicket.isPresent()) {
                    item.put("issue_ticket", issueTicket.get());
                }
                data2.add(item);

            } catch (RuntimeException e) {
                //System.out.println("Failed to process issue " + issue.getKey() + ". " + e.getMessage() + "|" +
                // issue.getSummary());
            }
        });
        Collections.reverse(data);
        Collections.reverse(data2);
        System.out.println("here");
    }

    interface OnIssueFound {
        void onFound (Session session, Issue issue);
    }

    private void iterate(String query, OnIssueFound callback) {
        final Session startrekSession = new StartrekClientBuilder()
                .uri("https://st-api.yandex-team.ru")
                .socketTimeout(30, TimeUnit.SECONDS)
                .customFields(
                        Cf.map(
                                "sreBeginTime", Field.Schema.scalar(Field.Schema.Type.DATETIME, false),
                                "sreEndTime", Field.Schema.scalar(Field.Schema.Type.DATETIME, false),
                                "vdt", Field.Schema.scalar(Field.Schema.Type.INTEGER, false),
                                "crashId", Field.Schema.scalar(Field.Schema.Type.STRING, false)
                        )
                )
                .build(STARTREK_TOKEN);
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .fields("tags,sreBeginTime,sreEndTime,vdt".split(","))
                .build();
        IteratorF<Issue> issues = startrekSession.issues().find(request);
        issues.forEachRemaining(issue -> callback.onFound(startrekSession, issue));
    }

    @Test
    @Ignore
    public void batchUpdate() {
        iterate("Tags: \"простой_склада\" AND \"Warehouse Downtime\": > 0 AND \"Время начала\": empty() \"Sort by\": " +
                "Created DESC", (startrekSession, issue) -> {
                    IssueUpdate update = IssueUpdate.builder()
                            .set("sreBeginTime", issue.getCreatedAt())
                            .build();
            startrekSession.issues().update(issue, update);
        });
    }
}

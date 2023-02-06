package ru.yandex.market.tsum.clients.aqua.testcases;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckTmsJobMessageParser {

    private static final Pattern JOB_STATE_PATTERN = Pattern.compile("(?<=JobState)\\{.*\\}");
    private static final Pattern JOB_NAME_PATTERN = Pattern.compile("(?<=jobName=\\')(.*?)(?=\\')");
    private static final Pattern JOB_STATUS_PATTERN = Pattern.compile("(?<=status=\\')(.*?)(?=\\')");

    private CheckTmsJobMessageParser() {
    }

    public static TmsJobState parseJobState(String message) {

        TmsJobState.Builder builder =
            TmsJobState.newBuilder();

        Matcher jobStateMatcher = JOB_STATE_PATTERN.matcher(message.replace("\n", " "));

        if (jobStateMatcher.find()) {
            String jobState = jobStateMatcher.group();
            jobState = jobState.replace("\\'", "\"");

            Matcher jobNameMatcher = JOB_NAME_PATTERN.matcher(jobState);
            if (jobNameMatcher.find()) {
                builder.withJobName(jobNameMatcher.group());
            }
            Matcher jobStatusMatcher = JOB_STATUS_PATTERN.matcher(jobState);
            if (jobStatusMatcher.find()) {
                builder.withStatus(jobStatusMatcher.group());
            }
        } else {
            builder.withJobName(message);
        }
        return builder.build();
    }
}

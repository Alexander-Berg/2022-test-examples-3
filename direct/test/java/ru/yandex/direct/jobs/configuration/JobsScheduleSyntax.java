package ru.yandex.direct.jobs.configuration;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.scheduler.Hourglass;
import ru.yandex.direct.scheduler.hourglass.HourglassJob;
import ru.yandex.direct.scheduler.support.DirectParameterizedJob;
import ru.yandex.direct.scheduler.support.ParameterizedBy;

@ExtendWith(SpringExtension.class)
@JobsTest
class JobsScheduleSyntax {

    @Autowired
    private List<HourglassJob> jobs;

    @Autowired
    private ApplicationContext ctx;

    private SoftAssertions softly = new SoftAssertions();

    @Test
    void testSchedules() {
        for (HourglassJob job : jobs) {
            checkJob(job.getClass());
        }
        softly.assertAll();
    }

    private boolean isCronExpressionValid(String expression) {
        try {
            new CronSequenceGenerator(expression);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    private void checkJob(Class<? extends HourglassJob> jobClass) {
        for (Hourglass schedule : jobClass.getAnnotationsByType(Hourglass.class)) {
            String cron = schedule.cronExpression();
            int period = schedule.periodInSeconds();

            softly.assertThat(
                    period != Hourglass.PERIOD_NOT_SPECIFIED && !cron.equals(Hourglass.CRON_EXPRESSION_NOT_SPECIFIED))
                    .as("Invalid schedule for class %s: both of period in seconds (%d) and cron " +
                            "expression (%s) are specified", jobClass.getCanonicalName(), period, cron)
                    .isFalse();

            softly.assertThat(
                    period == Hourglass.PERIOD_NOT_SPECIFIED && cron.equals(Hourglass.CRON_EXPRESSION_NOT_SPECIFIED))
                    .as("Invalid schedule for class %s: none of period in seconds or cron expression are specified",
                            jobClass.getName())
                    .isFalse();

            if (!cron.equals(Hourglass.CRON_EXPRESSION_NOT_SPECIFIED)) {
                softly.assertThat(isCronExpressionValid(cron))
                        .as("Cron expression for class %s (%s) should be valid", jobClass.getName(), cron)
                        .isTrue();

                softly.assertThat(cron.trim())
                        .as("Cron expression for class %s (%s) should not starts with *"
                                + " (for frequent jobs add @HourglassDaemon)", jobClass.getName(), cron)
                        .doesNotStartWith("*");
            }

            if (period != Hourglass.PERIOD_NOT_SPECIFIED) {
                softly.assertThat(period)
                        .as("Period in seconds for class %s (%d) should be valid", jobClass.getName(), period)
                        .isGreaterThan(Hourglass.PERIOD_NOT_SPECIFIED);
            }

            Boolean needSync = null;
            String errorDetail = "";
            try {
                needSync = ctx.getBean(schedule.needSchedule()).evaluate();
            } catch (BeansException e) {
                errorDetail = ": " + e.getMessage();
            }

            softly.assertThat(needSync)
                    .as("Invalid schedule for class %s: error evaluating needSync%s", jobClass.getName(), errorDetail)
                    .isNotNull();

            ParameterizedBy parameterizedBy = jobClass.getAnnotation(ParameterizedBy.class);
            if (DirectParameterizedJob.isParameterized(jobClass)) {
                softly.assertThat(jobClass)
                        .as("Job %s must be annotated with @ParameterizedBy", jobClass.getName())
                        .hasAnnotation(ParameterizedBy.class);
            } else {
                softly.assertThat(parameterizedBy)
                        .as("%s annotated with @ParameterizedBy but not parameterized job", jobClass.getName())
                        .isNull();
            }
            if (parameterizedBy != null) {
                softly.assertThatCode(() -> ctx.getBean(parameterizedBy.parametersSource()).getAllParamValues())
                        .as("%s (parameters source for %s) should supply all params values",
                                parameterizedBy.parametersSource().getName(), jobClass.getName())
                        .doesNotThrowAnyException();
            }
        }
    }
}

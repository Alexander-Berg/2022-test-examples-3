package ru.yandex.direct.scheduler.hourglass.implementations;

public class ScheduleInstructionsGetterTest {

/*    private AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

    private JobScheduleInfoFactory jobScheduleInstructionGetter =
            new JobScheduleInfoFactory(applicationContext);

    private List<NextRunCalcStrategy> getScheduleInstructionList(HourglassJob job) {
        return jobScheduleInstructionGetter.getScheduleInfo(job, null).getScheduleModifierParams();
    }

    private List<ScheduleModifierParams> getStartTimeStretchingParams(HourglassJob job) {
        return jobScheduleInstructionGetter.getScheduleInfo(job.getClass()).getScheduleModifierParams();
    }

    @Test
    public void getSchedules_DaemonJobTest() {
        applicationContext.register(AlwaysTrue.class);
        applicationContext.refresh();

        var scheduleInstructionsList = getScheduleInstructionList(new DaemonJob());

        assertThat(scheduleInstructionsList).hasSize(1);

        var gotScheduleInfo = scheduleInstructionsList.get(0);
        assertTrue(gotScheduleInfo instanceof DaemonRunStrategy);

        var expectedScheduleData = new ScheduleDaemonData(1);
        assertThat(gotScheduleInfo.getScheduleData()).isEqualTo(expectedScheduleData);
    }

    @Test
    public void getSchedules_CronJobTest() {
        applicationContext.register(AlwaysTrue.class);
        applicationContext.refresh();

        var scheduleInstructionsList = getScheduleInstructionList(new CronJob());
        assertThat(scheduleInstructionsList).hasSize(1);

        var gotScheduleInfo = scheduleInstructionsList.get(0);
        assertTrue(gotScheduleInfo instanceof ByCronExpressionStrategy);

        var expectedScheduleData = new ScheduleCronData("0 15 * * * ?");
        assertThat(gotScheduleInfo.getScheduleData()).isEqualTo(expectedScheduleData);

        var scheduleModifiers = getStartTimeStretchingParams(new CronJob());
        assertEquals(0, scheduleModifiers.size());
    }

    @Test
    public void getSchedules_CronJobWithStretchingTest() {
        applicationContext.register(AlwaysTrue.class);
        applicationContext.refresh();

        var scheduleInstructionsList = getScheduleInstructionList(new CronJobWithStretching());

        assertThat(scheduleInstructionsList).hasSize(1);

        var gotScheduleInfo = scheduleInstructionsList.get(0);
        assertTrue(gotScheduleInfo instanceof ByCronExpressionStrategy);

        var expectedScheduleData = new ScheduleCronData("0 15 * * * ?");
        assertThat(gotScheduleInfo.getScheduleData()).isEqualTo(expectedScheduleData);

        var scheduleModifiers = getStartTimeStretchingParams(new CronJobWithStretching());

        assertEquals(1, scheduleModifiers.size());
        assertThat(scheduleModifiers.get(0)).isInstanceOf(StartTimeStretchingParams.class);
        StartTimeStretchingParams startTimeStretchingParams = (StartTimeStretchingParams) scheduleModifiers.get(0);

        assertEquals(3100, startTimeStretchingParams.getStretchPeriod());
    }

    @Test
    public void getSchedules_PeriodJobWithoutStretchingTest() {
        applicationContext.register(AlwaysTrue.class);
        applicationContext.refresh();

        var scheduleInstructionsList = getScheduleInstructionList(new PeriodJobWithoutStretching());

        assertThat(scheduleInstructionsList).hasSize(1);

        var gotScheduleInfo = scheduleInstructionsList.get(0);
        assertTrue(gotScheduleInfo instanceof PeriodicStrategy);

        var expectedScheduleData = new SchedulePeriodicData(2000, MILLISECONDS);
        assertThat(gotScheduleInfo.getScheduleData()).isEqualTo(expectedScheduleData);

        var scheduleModifiers = getStartTimeStretchingParams(new PeriodJobWithoutStretching());
        assertEquals(0, scheduleModifiers.size());
    }

    @Test
    public void getSchedules_PeriodJobTest() {
        applicationContext.register(AlwaysTrue.class);
        applicationContext.refresh();

        var scheduleInstructionsList = getScheduleInstructionList(new PeriodJob());
        assertThat(scheduleInstructionsList).hasSize(1);

        var gotScheduleInfo = scheduleInstructionsList.get(0);
        assertTrue(gotScheduleInfo instanceof PeriodicStrategy);

        var expectedScheduleData = new SchedulePeriodicData(2000, MILLISECONDS);
        assertThat(gotScheduleInfo.getScheduleData()).isEqualTo(expectedScheduleData);

        var scheduleModifiers = getStartTimeStretchingParams(new PeriodJob());
        assertEquals(1, scheduleModifiers.size());
        assertThat(scheduleModifiers.get(0)).isInstanceOf(StartTimeStretchingParams.class);
        StartTimeStretchingParams startTimeStretchingParams = (StartTimeStretchingParams) scheduleModifiers.get(0);

        assertEquals(2, startTimeStretchingParams.getStretchPeriod());
    }

    @Test
    public void getSchedules_FalseConditionJobTest() {
        applicationContext.register(FalseCondition.class);
        applicationContext.refresh();

        var scheduleInstructionsList = getScheduleInstructionList(new FalseConditionJob());
        assertThat(scheduleInstructionsList).hasSize(1);

        var gotScheduleInfo = scheduleInstructionsList.get(0);

        assertTrue(gotScheduleInfo instanceof FarFutureStrategy);

        assertThat(gotScheduleInfo.getScheduleData()).isNull();
    }


    @Test
    public void getSchedules_FalseConditionAndPeriodJobTest() {
        applicationContext.register(AlwaysTrue.class);
        applicationContext.register(FalseCondition.class);
        applicationContext.refresh();

        var scheduleInstructionsList = getScheduleInstructionList(new FalseConditionAndPeriodJob());

        assertThat(scheduleInstructionsList).hasSize(1);

        var gotScheduleInfo = scheduleInstructionsList.get(0);
        assertTrue(gotScheduleInfo instanceof PeriodicStrategy);

        var expectedScheduleData = new SchedulePeriodicData(10, MINUTES);
        assertThat(gotScheduleInfo.getScheduleData()).isEqualTo(expectedScheduleData);
    }

    @Test
    public void getSchedules_CronAndPeriodicJobTest() {
        applicationContext.register(AlwaysTrue.class);
        applicationContext.refresh();

        var scheduleInstructionsList = getScheduleInstructionList(new CronAndPeriodicJob());

        assertThat(scheduleInstructionsList).hasSize(2);

        var expectedScheduleInfoList = new NextRunCalcStrategy[]{
                new ByCronExpressionStrategy(new ScheduleCronData("25 27 6 * * ?")),
                new PeriodicStrategy(new SchedulePeriodicData(5, MINUTES))
        };

        assertThat(scheduleInstructionsList).containsExactlyInAnyOrder(expectedScheduleInfoList);
    }

    @Test
    public void getSchedules_NotSpecifiedPeriodJobTest() {
        applicationContext.register(AlwaysTrue.class);
        applicationContext.refresh();

        var scheduleInstructionsList = getScheduleInstructionList(new NotSpecifiedPeriodJob());

        assertThat(scheduleInstructionsList).hasSize(1);
        var gotScheduleInfo = scheduleInstructionsList.get(0);

        assertTrue(gotScheduleInfo instanceof FarFutureStrategy);

        assertThat(gotScheduleInfo.getScheduleData()).isNull();
    }

    private static class HourglassImplementationJob implements HourglassJob {
        @Override
        public void execute(TaskParametersMap parametersMap) {
        }

        @Override
        public void onShutdown() {
        }
    }

    @Hourglass(periodInSeconds = 1)
    @HourglassDaemon
    private class DaemonJob extends HourglassImplementationJob {
    }

    @Hourglass(cronExpression = "0 15 * * * ?")
    private class CronJob extends HourglassImplementationJob {
    }

    @Hourglass(cronExpression = "0 15 * * * ?")
    @HourglassStretchPeriod(3100)
    private class CronJobWithStretching extends HourglassImplementationJob {
    }

    @Hourglass(periodInSeconds = 2)
    @HourglassStretchingDisabled
    private class PeriodJobWithoutStretching extends HourglassImplementationJob {
    }


    @Hourglass(periodInSeconds = 2)
    private class PeriodJob extends HourglassImplementationJob {
    }

    @Hourglass(periodInSeconds = 7, needSchedule = FalseCondition.class)
    private class FalseConditionJob extends HourglassImplementationJob {
    }

    @Hourglass(periodInSeconds = 600)
    @Hourglass(periodInSeconds = 900, needSchedule = FalseCondition.class)
    private class FalseConditionAndPeriodJob extends HourglassImplementationJob {
    }

    @Hourglass(periodInSeconds = 300)
    @Hourglass(cronExpression = "25 27 6 * * ?")
    private class CronAndPeriodicJob extends HourglassImplementationJob {
    }

    @Hourglass()
    private class NotSpecifiedPeriodJob extends HourglassImplementationJob {
    }

    private static class FalseCondition implements Condition {
        @Override
        public boolean evaluate() {
            return false;
        }
    }*/
}

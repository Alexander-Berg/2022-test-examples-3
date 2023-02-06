package ru.yandex.direct.scheduler.hourglass.implementations;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.hourglass.implementations.updateschedule.ScheduleRecord;
import ru.yandex.direct.scheduler.Hourglass;
import ru.yandex.direct.scheduler.hourglass.ParamDescription;
import ru.yandex.direct.scheduler.hourglass.TaskListProvider;
import ru.yandex.direct.scheduler.hourglass.TaskParametersMap;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.ScheduleInfoImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.ModifierDataWithTypeImpl;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.RandomStartTime;
import ru.yandex.direct.scheduler.hourglass.implementations.schedule.modifiers.RandomStartTimeData;
import ru.yandex.direct.scheduler.hourglass.schedule.ScheduleInfoConverter;
import ru.yandex.direct.scheduler.hourglass.schedule.modifiers.ModifierType;
import ru.yandex.direct.scheduler.hourglass.schedule.modifiers.NextRunModifierFactory;
import ru.yandex.direct.scheduler.support.DirectJob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScheduleRecordListProviderImplTest {
    private TaskParameterizer taskParameterizer;
    private JobScheduleInfoFactory jobScheduleInfoFactory;
    private TaskListProvider taskListProvider;
    private ScheduleInfoConverter scheduleInfoConverter;
    private NextRunModifierFactory nextRunModifierFactory;
    private ScheduleRecordListProviderImpl scheduleRecordListProvider;

    @Before
    public void before() {
        taskParameterizer = mock(TaskParameterizer.class);
        jobScheduleInfoFactory = mock(JobScheduleInfoFactory.class);
        taskListProvider = mock(TaskListProviderImpl.class);

        scheduleInfoConverter = mock(ScheduleInfoConverter.class);
        nextRunModifierFactory = mock(NextRunModifierFactory.class);
        scheduleRecordListProvider =
                new ScheduleRecordListProviderImpl(taskListProvider, scheduleInfoConverter, taskParameterizer,
                        jobScheduleInfoFactory);
    }

    @Test
    public void getRecords_severalParams() {
        var param1 = new ParamDescriptionImpl(1, 2, TaskParametersMap.of("param", "1"));
        var param2 = new ParamDescriptionImpl(1, 2, TaskParametersMap.of("param", "2"));
        List<ParamDescription> params = List.of(param1, param2);
        when(taskParameterizer.getAllParameters(any())).thenReturn(params);

        var modifier1 = new ModifierDataWithTypeImpl(ModifierType.RANDOM_START_TIME, new RandomStartTimeData(1));
        var modifier2 = new ModifierDataWithTypeImpl(ModifierType.RANDOM_START_TIME, new RandomStartTimeData(2));
        when(nextRunModifierFactory.getModifier(eq(ModifierType.RANDOM_START_TIME))).thenReturn(new RandomStartTime());
        var scheduleInfo1 = new ScheduleInfoImpl(List.of(), List.of(modifier1));
        var scheduleInfo2 = new ScheduleInfoImpl(List.of(), List.of(modifier2));
        when(jobScheduleInfoFactory.getScheduleInfo(any(), eq(param1))).thenReturn(scheduleInfo1);
        when(jobScheduleInfoFactory.getScheduleInfo(any(), eq(param2))).thenReturn(scheduleInfo2);

        when(scheduleInfoConverter.serializeAndEncodeSchedule(eq(scheduleInfo1))).thenReturn("serializedStr1");
        when(scheduleInfoConverter.serializeSchedule(eq(scheduleInfo1))).thenReturn("prettyStr1");
        when(scheduleInfoConverter.serializeAndEncodeSchedule(eq(scheduleInfo2))).thenReturn("serializedStr2");
        when(scheduleInfoConverter.serializeSchedule(eq(scheduleInfo2))).thenReturn("prettyStr2");

        when(taskListProvider.getTasks()).thenReturn(List.of(
                TaskDescriptionImpl.builder()
                        .setName("TestJob")
                        .setTaskClass(TestJob.class)
                        .build()
        ));
        scheduleRecordListProvider.init();

        var gotRecords = scheduleRecordListProvider.getRecords();

        var expectedRecords = List.of(
                new ScheduleRecord()
                        .setMeta("prettyStr1")
                        .setName("TestJob")
                        .setScheduleHashSum("serializedStr1")
                        .setParam("{\"param\":\"1\"}"),
                new ScheduleRecord()
                        .setMeta("prettyStr2")
                        .setName("TestJob")
                        .setScheduleHashSum("serializedStr2")
                        .setParam("{\"param\":\"2\"}")
        );
        assertThat(gotRecords).hasSize(2);
        assertThat(gotRecords)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedRecords.toArray(ScheduleRecord[]::new));
    }

    @Test
    public void getRecords_emptyParamsMap() {
        when(taskParameterizer.getAllParameters(any())).thenReturn(List.of());

        when(jobScheduleInfoFactory.getScheduleInfo(any(), any())).thenReturn(new ScheduleInfoImpl(List.of(),
                List.of()));

        when(scheduleInfoConverter.serializeAndEncodeSchedule(any())).thenReturn("encodedSerializedStr");
        when(scheduleInfoConverter.serializeSchedule(any())).thenReturn("serializedStr");

        when(taskListProvider.getTasks()).thenReturn(List.of(
                TaskDescriptionImpl.builder()
                        .setName("TestJob")
                        .setTaskClass(TestJob.class)
                        .build()
        ));
        scheduleRecordListProvider.init();

        var gotRecords = scheduleRecordListProvider.getRecords();

        var expectedRecords = List.of(
                new ScheduleRecord()
                        .setMeta("serializedStr")
                        .setName("TestJob")
                        .setScheduleHashSum("encodedSerializedStr")
                        .setParam("{}")
        );
        assertThat(gotRecords).hasSize(1);
        assertThat(gotRecords)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedRecords.toArray(ScheduleRecord[]::new));
    }

    @Hourglass
    private class TestJob extends DirectJob {
        @Override
        public void execute() {

        }
    }
}

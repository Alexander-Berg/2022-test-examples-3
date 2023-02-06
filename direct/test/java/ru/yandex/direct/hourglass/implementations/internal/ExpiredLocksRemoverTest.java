package ru.yandex.direct.hourglass.implementations.internal;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.hourglass.MonitoringWriter;
import ru.yandex.direct.hourglass.implementations.InstanceIdImpl;
import ru.yandex.direct.hourglass.storage.JobStatus;
import ru.yandex.direct.hourglass.storage.implementations.memory.IntegerPrimaryId;
import ru.yandex.direct.hourglass.storage.implementations.memory.MemStorageImpl;
import ru.yandex.direct.hourglass.storage.implementations.memory.MutableJob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExpiredLocksRemoverTest {

    @Test
    void testRemoveExpiredJobs() {
        MemStorageImpl storage = new MemStorageImpl();

        storage.addJob(new MutableJob().setJobStatus(JobStatus.EXPIRED).setPrimaryId(new IntegerPrimaryId(1)));
        storage.addJob(new MutableJob().setJobStatus(JobStatus.EXPIRED).setPrimaryId(new IntegerPrimaryId(2)));
        storage.addJob(new MutableJob().setJobStatus(JobStatus.LOCKED).setPrimaryId(new IntegerPrimaryId(3)));
        storage.addJob(new MutableJob().setJobStatus(JobStatus.READY).setPrimaryId(new IntegerPrimaryId(4)));
        storage.addJob(new MutableJob().setJobStatus(JobStatus.STOPPED).setPrimaryId(new IntegerPrimaryId(5)));
        storage.addJob(new MutableJob().setJobStatus(JobStatus.ARCHIVED).setPrimaryId(new IntegerPrimaryId(6)));

        ExpiredLocksRemover expiredLocksRemover = new ExpiredLocksRemover(storage, new InstanceIdImpl(),
                mock(MonitoringWriter.class));

        expiredLocksRemover.run();

        assertThat(storage.find().whereJobStatus(JobStatus.EXPIRED).findJobs()).isEmpty();
        assertThat(storage.find().wherePrimaryIdIn(List.of(new IntegerPrimaryId(4))).whereJobStatus(JobStatus.READY)
                .findJobs()).hasSize(1);
        assertThat(storage.find().wherePrimaryIdIn(List.of(new IntegerPrimaryId(4))).whereJobStatus(JobStatus.READY)
                .findJobs()).hasSize(1);
        assertThat(storage.find().whereJobStatus(JobStatus.LOCKED).wherePrimaryIdIn(List.of(new IntegerPrimaryId(3)))
                .findJobs()).hasSize(1);
        assertThat(storage.find().whereJobStatus(JobStatus.STOPPED).wherePrimaryIdIn(List.of(new IntegerPrimaryId(5)))
                .findJobs()).hasSize(1);
        assertThat(storage.find().whereJobStatus(JobStatus.ARCHIVED).wherePrimaryIdIn(List.of(new IntegerPrimaryId(6)))
                .findJobs()).hasSize(1);
    }


}


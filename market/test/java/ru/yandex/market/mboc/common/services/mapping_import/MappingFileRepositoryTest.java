package ru.yandex.market.mboc.common.services.mapping_import;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MappingFileRepositoryTest extends BaseDbTestClass {

    @Autowired
    private MappingFileRepository repository;
    @Autowired
    private MappingFileRowRepository rowRepo;

    @Test
    public void findUncompleted() {
        repository.deleteAll();
        repository.insertBatch(
        MappingFile.builder().filename("uncomplete1").uploaded(LocalDateTime.now()).build(),
        MappingFile.builder().filename("uncomplete2").uploaded(LocalDateTime.now()).build(),
        MappingFile.builder().filename("uncomplete3").uploaded(LocalDateTime.now()).build()
        );

        repository.insert(MappingFile.builder().filename("complete").uploaded(LocalDateTime.now())
            .completed(LocalDateTime.now()).build());

        var uncompletedFiles = repository.findUncompletedFiles();
        var names = uncompletedFiles.stream().map(MappingFile::getFilename).filter(name -> name.startsWith(
            "uncomplete")).collect(Collectors.toList());
        assertEquals(3, names.size());
    }

    @Test
    public void testFileCompletion() {
        repository.deleteAll();
        rowRepo.deleteAll();

        var file = repository.insert(
            MappingFile.builder().filename("to_complete").uploaded(LocalDateTime.now()).size(1).build()
            );


        MappingFileRow row = rowRepo.insert(
            MappingFileRow.builder().fileId(file.getId()).processed(LocalDateTime.now())
                .resolution(MappingFileRow.Resolution.DONE).build()
        );

        repository.tryToCompleteFiles(List.of(file));

        var updated = repository.findById(file.getId());
        assertNotNull(updated.getCompleted());

        MappingFileRow rowById = rowRepo.findById(row.getId());
        assertEquals(MappingFileRow.Resolution.DONE, rowById.getResolution());
    }

    @Test
    public void testRepeatedCompletion(){
        repository.deleteAll();
        rowRepo.deleteAll();

        var fixedDate = LocalDateTime.of(2000,1,1,0,0);

        var file = repository.insert(
            MappingFile.builder()
                .filename("to_complete")
                .uploaded(fixedDate)
                .completed(fixedDate)
                .size(1)
                .build()
        );


        MappingFileRow row = rowRepo.insert(
            MappingFileRow.builder().fileId(file.getId()).processed(fixedDate)
                .resolution(MappingFileRow.Resolution.DONE).build()
        );

        repository.tryToCompleteFiles(List.of(file));

        var updated = repository.findById(file.getId());
        assertNotNull(updated.getCompleted());
        assertEquals(fixedDate, updated.getCompleted());

        MappingFileRow rowById = rowRepo.findById(row.getId());
        assertEquals(MappingFileRow.Resolution.DONE, rowById.getResolution());

    }

    @Test
    public void testFileStatistics() {
        repository.deleteAll();
        rowRepo.deleteAll();

        var fileCompleted = repository.insert(
            MappingFile.builder().filename("to_complete").uploaded(LocalDateTime.now()).size(3).build()
        );

        rowRepo.insertBatch(
            MappingFileRow.builder().fileId(fileCompleted.getId()).processed(LocalDateTime.now()).build(),
            MappingFileRow.builder().fileId(fileCompleted.getId()).processed(LocalDateTime.now()).build(),
            MappingFileRow.builder().fileId(fileCompleted.getId()).skipped(LocalDateTime.now()).build()
        );

        var fileSkipped = repository.insert(
            MappingFile.builder().filename("to_complete").uploaded(LocalDateTime.now()).size(3).build()
        );

        rowRepo.insertBatch(
            MappingFileRow.builder().fileId(fileSkipped.getId()).skipped(LocalDateTime.now()).build(),
            MappingFileRow.builder().fileId(fileSkipped.getId()).skipped(LocalDateTime.now()).build(),
            MappingFileRow.builder().fileId(fileSkipped.getId()).skipped(LocalDateTime.now()).build()
        );

        var inProgress = repository.insert(
            MappingFile.builder().filename("to_complete").uploaded(LocalDateTime.now()).size(3).build()
        );

        rowRepo.insertBatch(
            MappingFileRow.builder().fileId(inProgress.getId()).build(),
            MappingFileRow.builder().fileId(inProgress.getId()).build(),
            MappingFileRow.builder().fileId(inProgress.getId()).build()
        );

        repository.tryToCompleteFiles(List.of(inProgress));

        var statuses = repository.collectFileStatuses().stream()
            .collect(Collectors.toMap(MappingFileStatus::getId, Function.identity()));

        var completedStatistics = statuses.get(fileCompleted.getId());
        assertEquals(2, completedStatistics.getProcessed());
        assertEquals(1, completedStatistics.getSkipped());
        assertNotNull(completedStatistics.getCompleted());

        var skippedStatistics = statuses.get(fileSkipped.getId());
        assertEquals(3, skippedStatistics.getSkipped());
        assertNotNull(skippedStatistics.getCompleted());

        var inProgressStatistics = statuses.get(inProgress.getId());
        assertNull(inProgressStatistics.getCompleted());
        assertEquals(0, inProgressStatistics.getProcessed());
        assertEquals(0, inProgressStatistics.getSkipped());
    }

    @Test
    public void testVerificationStatusesInStats() {
        repository.deleteAll();

        var values = MappingFile.VerificationStatus.values();

        Stream.of(values).map(this::basicFileWithVerificationStatus).forEach(repository::insert);

        var expectedStatuses = Stream.of(values)
            .map(MappingFile.VerificationStatus::toText)
            .collect(Collectors.toList());

        var actualStatuses =  repository.collectFileStatuses().stream()
            .map(MappingFileStatus::getVerificationStatus)
            .collect(Collectors.toList());
        Collections.reverse(actualStatuses);
        assertEquals(expectedStatuses, actualStatuses);
    }

    private MappingFile basicFileWithVerificationStatus(MappingFile.VerificationStatus status) {
        return MappingFile.builder()
            .filename("to_complete")
            .uploaded(LocalDateTime.now())
            .size(3)
            .verificationStatus(status)
            .build();
    }


}

package ru.yandex.market.mboc.common.services.mapping_import;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.lightmapper.test.LongGenericMapperRepositoryMock;

public class MappingFileRowRepositoryMock extends LongGenericMapperRepositoryMock<MappingFileRow>
    implements MappingFileRowRepository {

    private final MappingFileRepository fileRepository;

    public MappingFileRowRepositoryMock(MappingFileRepository fileRepository) {
        super(MappingFileRow::setId, MappingFileRow::getId);
        this.fileRepository = fileRepository;
    }

    @Override
    protected void validate(MappingFileRow instance) {
        fileRepository.findById(instance.getFileId());
    }

    @Override
    public List<Long> findRowIdsToProcess(MappingFile files) {
        Predicate<MappingFileRow> isRelated = row -> files.getId() == row.getFileId();
        Predicate<MappingFileRow> toProcess = row -> row.getProcessed() == null && row.getSkipped() == null;

        return findAll().stream()
            .filter(isRelated)
            .filter(toProcess)
            .map(MappingFileRow::getId)
            .collect(Collectors.toList());
    }

    @Override
    public List<MappingFileRow> findRowsToProcess(Collection<MappingFile> files, int chunkSize, int offset) {
        var fileIds = files.stream().map(MappingFile::getId).collect(Collectors.toSet());
        Predicate<MappingFileRow> isRelated = row -> fileIds.contains(row.getFileId());
        Predicate<MappingFileRow> toProcess = row -> row.getProcessed() == null && row.getSkipped() == null;
        return findAll().stream().filter(isRelated).filter(toProcess).collect(Collectors.toList());
    }

    @Override
    public void findFailedRows(Integer fileId, Consumer<MappingFileRow> consumer) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public void markCompleted(Collection<MappingFileRow> rows) {
        rows.forEach(row -> row.setProcessed(LocalDateTime.now()));
        updateBatch(rows);
    }

    @Override
    public void markSkipped(Collection<MappingFileRow> rows) {
        rows.forEach(row -> row.setSkipped(LocalDateTime.now()));
        updateBatch(rows);
    }

    @Override
    public List<MappingFileRow> findRandomRows(Integer fileId) {
        var part = findAll().stream().limit(50).collect(Collectors.toList());
        Collections.shuffle(part);
        return part;
    }
}

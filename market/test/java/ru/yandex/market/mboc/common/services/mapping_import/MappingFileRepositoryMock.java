package ru.yandex.market.mboc.common.services.mapping_import;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.lightmapper.test.LongGenericMapperRepositoryMock;

public class MappingFileRepositoryMock extends LongGenericMapperRepositoryMock<MappingFile>
    implements MappingFileRepository {

    public MappingFileRepositoryMock() {
        super(MappingFile::setId, MappingFile::getId);
    }

    @Override
    public List<MappingFile> findUncompletedFiles() {
        return findAll().stream().filter(Predicate.not(MappingFile::isCompleted)).collect(Collectors.toList());
    }

    @Override
    public void tryToCompleteFiles(Collection<MappingFile> files) {
        files.forEach(file -> file.setCompleted(LocalDateTime.now()));
        updateBatch(files);
    }

    @Override
    public List<MappingFileStatus> collectFileStatuses() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void tryToCompleteVerification(MappingFile file) {
        throw new RuntimeException("not implemented");
    }
}

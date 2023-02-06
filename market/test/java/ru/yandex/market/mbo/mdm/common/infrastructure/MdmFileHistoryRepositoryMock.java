package ru.yandex.market.mbo.mdm.common.infrastructure;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import ru.yandex.market.mbo.lightmapper.test.LongGenericMapperRepositoryMock;

/**
 * @author dmserebr
 * @date 29/06/2020
 */
public class MdmFileHistoryRepositoryMock extends LongGenericMapperRepositoryMock<MdmFileHistoryEntry>
    implements MdmFileHistoryRepository {

    public MdmFileHistoryRepositoryMock() {
        super(MdmFileHistoryEntry::setId, MdmFileHistoryEntry::getId);
    }

    @Override
    public List<MdmFileHistoryEntry> findBy(MdmFileHistoryFilter filter, Integer limit, Integer offset) {
        return findAll().stream().filter(e -> test(e, filter)).collect(Collectors.toList());
    }

    @Override
    public Integer getCount(MdmFileHistoryFilter filter) {
        return Math.toIntExact(findAll().stream().filter(e -> test(e, filter)).count());
    }

    private boolean test(MdmFileHistoryEntry e, MdmFileHistoryFilter filter) {
        return (filter.getFileType() == null || e.getFileType() == filter.getFileType()) &&
            (CollectionUtils.isEmpty(filter.getFileStatuses()) ||
                filter.getFileStatuses().contains(e.getFileStatus())) &&
            (filter.getUserLogin() != null || e.getUserLogin().equals(filter.getUserLogin())) &&
            (filter.getUploadedAtStart() == null || !e.getUploadedAt().isBefore(filter.getUploadedAtStart())) &&
            (filter.getUploadedAtEnd() == null || e.getUploadedAt().isBefore(filter.getUploadedAtEnd()));
    }

    @Override
    public long addNewEntry(String filename, MdmFileType fileType, String userLogin) {
        var entry = new MdmFileHistoryEntry();
        entry.setId(nextId());
        entry.setFilename(filename);
        entry.setFileType(fileType);
        entry.setFileStatus(FileStatus.NEW);
        Instant now = Instant.now();
        entry.setUploadedAt(now);
        entry.setModifiedAt(now);
        entry.setUserLogin(userLogin);
        insert(entry);

        return entry.getId();
    }

    @Override
    public void updateEntry(long id, FileStatus fileStatus, String s3Path, List<String> errors,
                            String s3ErrorsPath, int errorCount) {

        MdmFileHistoryEntry entry = findById(id);
        entry.setFileStatus(fileStatus);
        entry.setS3Path(s3Path);
        entry.setErrors(new ArrayList<>(errors));
        entry.setS3ErrorsPath(s3ErrorsPath);
        entry.setErrorCount(errorCount);
        update(entry);
    }
}

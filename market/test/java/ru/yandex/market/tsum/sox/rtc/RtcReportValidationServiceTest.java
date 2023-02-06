package ru.yandex.market.tsum.sox.rtc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.market.tsum.agent.ConfId;
import ru.yandex.market.tsum.agent.FileChecksum;
import ru.yandex.market.tsum.agent.Resource;
import ru.yandex.market.tsum.agent.ResourceInfo;
import ru.yandex.market.tsum.agent.RtcInstanceReport;
import ru.yandex.market.tsum.core.agent.AgentMongoDao;
import ru.yandex.market.tsum.core.agent.ChecksumMismatch;
import ru.yandex.market.tsum.core.agent.PackageType;
import ru.yandex.market.tsum.core.agent.RtcResourceStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 17.05.2018
 */
public class RtcReportValidationServiceTest {
    private static final ConfId CONF_ID = ConfId.newBuilder().setService("some_service").build();
    private static final ConfId WHITELISTED_CONF_ID = ConfId.newBuilder().setService("whitelisted_service").build();
    private static final String HOST = "some_host";
    private static final long TASK_ID = 123;

    @Test
    public void shouldNotAddNotFoundFile_whenDeployInProgress() {
        assertNoErrors(
            instanceReport(true),
            resourceInfo(fileChecksum("/expected/but/not/found", "some_md5"))
        );
    }

    @Test
    public void shouldNotAddChecksumMismatch_whenDeployInProgress() {
        String path = "some/file";
        assertNoErrors(
            instanceReport(true, fileChecksum(path, "md5_1")),
            resourceInfo(fileChecksum(path, "md5_2"))
        );
    }

    @Test
    public void shouldNotAddNotFoundFile_whenServiceIsWhitelisted() {
        assertNoErrors(
            instanceReport(WHITELISTED_CONF_ID),
            resourceInfo(fileChecksum("/expected/but/not/found", "some_md5"))
        );
    }

    @Test
    public void shouldNotAddChecksumMismatch_whenServiceIsWhitelisted() {
        String path = "some/file";
        assertNoErrors(
            instanceReport(WHITELISTED_CONF_ID, fileChecksum(path, "md5_1")),
            resourceInfo(fileChecksum(path, "md5_2"))
        );
    }

    @Test
    public void shouldAddUnknownConfId_whenResourceInfoIsMissing() {
        assertUnknownConfId(
            instanceReport(),
            missingResourceInfo()
        );
    }

    @Test
    public void shouldNotAddAnyErrors_whenThereAreNoFiles() {
        assertNoErrors(
            instanceReport(),
            resourceInfoWithoutFiles()
        );
    }

    @Test
    public void shouldAddNotFoundFile_whenFileExistsInResourceInfoButDoesNotExistInRtcInstanceReport() {
        String filePath = "/expected/but/not/found";
        assertInstanceMismatch(
            instanceMismatchWithUnknownFile(filePath),
            instanceReport(),
            resourceInfo(fileChecksum(filePath, "some_md5"))
        );
    }

    /**
     * RtcReportValidationService.getChecksum пытается отбрасывать префикс (всё до '/' включительно) если файл из
     * RtcInstanceReport не найден в ResourceInfo. Это кейс, в котором префикс отбросить нельзя.
     */
    @Test
    public void shouldAddNotFoundFile_whenThereIsNoForwardSlashesAndFileIsNotFound() {
        String filePath = "weird_file_name";
        assertInstanceMismatch(
            instanceMismatchWithUnknownFile(filePath),
            instanceReport(),
            resourceInfo(fileChecksum(filePath, "some_md5"))
        );
    }

    @Test
    public void shouldNotAddAnyErrors_whenThereIsOneFileWithMatchingMd5() {
        FileChecksum fileChecksum = fileChecksum("/file/with/correct/md5", "some_md5");
        assertNoErrors(
            instanceReport(fileChecksum),
            resourceInfo(fileChecksum)
        );
    }

    @Test
    public void shouldNotAddAnyErrors_whenThereIsOneFileWithMatchingMd5_alternativePathToMd5Conversion() {
        String filePath = "file/with/correct/md5";
        String fileMd5 = "some_md5";
        assertNoErrors(
            instanceReport(fileChecksum(filePath, fileMd5)),
            resourceInfo(fileChecksum("some_prefix/" + filePath, fileMd5))
        );
    }

    @Test
    public void shouldNotAddAnyErrors_whenThereAreTwoFileWithSomeNamesWithMatchingMd5() {
        FileChecksum fileChecksum = fileChecksum("/file/with/correct/md5", "some_md5");
        FileChecksum fileChecksum2 = fileChecksum("/file/with/correct/md5", "some_md5");
        assertNoErrors(
            instanceReport(fileChecksum, fileChecksum2),
            resourceInfo(fileChecksum)
        );
    }

    @Test
    public void shouldAddInstanceMismatch_whenThereIsOneFileWithWrongMd5() {
        String filePath = "/file/with/correct/md5";
        String expectedMd5 = "expected_md5";
        String actualMd5 = "actual_md5";
        assertInstanceMismatch(
            instanceMismatchWithChecksumMismatch(filePath, expectedMd5, actualMd5),
            instanceReport(fileChecksum(filePath, actualMd5)),
            resourceInfo(fileChecksum(filePath, expectedMd5))
        );
    }

    private static RtcInstanceReport instanceReport(FileChecksum... fileChecksums) {
        return instanceReport(false, CONF_ID, fileChecksums);
    }

    private static RtcInstanceReport instanceReport(boolean deployInProgress, FileChecksum... fileChecksums) {
        return instanceReport(deployInProgress, CONF_ID, fileChecksums);
    }

    private static RtcInstanceReport instanceReport(ConfId confId, FileChecksum... fileChecksums) {
        return instanceReport(false, confId, fileChecksums);
    }

    private static RtcInstanceReport instanceReport(boolean deployInProgress, ConfId confId,
                                                    FileChecksum... fileChecksums) {
        return RtcInstanceReport.newBuilder()
            .setConfId(confId)
            .setHost(HOST)
            .setDeployInProgress(deployInProgress)
            .addAllChecksums(Arrays.asList(fileChecksums))
            .build();
    }

    private static Optional<AgentMongoDao.ResourceInfoWrapper.ResourceInfoEntity> missingResourceInfo() {
        return Optional.empty();
    }

    private static Optional<AgentMongoDao.ResourceInfoWrapper.ResourceInfoEntity> resourceInfoWithoutFiles() {
        return resourceInfo();
    }

    private static Optional<AgentMongoDao.ResourceInfoWrapper.ResourceInfoEntity> resourceInfo(FileChecksum... fileChecksums) {
        return Optional.of(new AgentMongoDao.ResourceInfoWrapper.ResourceInfoEntity(ResourceInfo.newBuilder()
            .addAllChecksums(Arrays.asList(fileChecksums))
            .setResource(Resource.newBuilder()
                .setTaskId(TASK_ID)
                .build())
            .build())
        );
    }

    private static FileChecksum fileChecksum(String path, String md5) {
        return FileChecksum.newBuilder()
            .setPath(path)
            .setMd5(md5)
            .build();
    }

    private static RtcResourceStatus.InstanceMismatch instanceMismatchWithUnknownFile(String filePath) {
        return new RtcResourceStatus.InstanceMismatch(
            getConfIdEntity(),
            HOST,
            new TreeSet<>(),
            new TreeSet<>(Collections.singletonList(filePath))
        );
    }

    private static RtcResourceStatus.InstanceMismatch instanceMismatchWithChecksumMismatch(
        String filePath, String expectedMd5, String actualMd5
    ) {
        return new RtcResourceStatus.InstanceMismatch(
            getConfIdEntity(),
            HOST,
            new TreeSet<>(Collections.singletonList(
                new ChecksumMismatch(filePath, Long.toString(TASK_ID), expectedMd5, actualMd5)
            )),
            new TreeSet<>()
        );
    }

    private static AgentMongoDao.ConfIdEntity getConfIdEntity() {
        return new AgentMongoDao.ConfIdEntity(
            CONF_ID.getService(),
            CONF_ID.getId(),
            CONF_ID.getExtraResourcesList().stream()
                .map(proto -> new AgentMongoDao.RtcInstanceReportWrapper.RtcInstanceReportEntity.ExtraResourceEntity(proto.getResourceType(), proto.getResourceTaskId()))
                .collect(Collectors.toList())
        );
    }

    private static void assertNoErrors(
        RtcInstanceReport rtcInstanceReport,
        Optional<AgentMongoDao.ResourceInfoWrapper.ResourceInfoEntity> resourceInfo
    ) {
        RtcResourceStatus resourceStatus = runValidateReport(rtcInstanceReport, resourceInfo);
        assertTrue(resourceStatus.isValid());
        assertTrue(resourceStatus.getSortedUnknownConfIds().isEmpty());
        assertTrue(resourceStatus.getInstanceMismatches().isEmpty());
    }

    private static void assertUnknownConfId(
        RtcInstanceReport rtcInstanceReport,
        Optional<AgentMongoDao.ResourceInfoWrapper.ResourceInfoEntity> resourceInfo
    ) {
        RtcResourceStatus resourceStatus = runValidateReport(rtcInstanceReport, resourceInfo);
        assertFalse(resourceStatus.isValid());
        assertTrue(resourceStatus.getInstanceMismatches().isEmpty());
        assertEquals(1, resourceStatus.getSortedUnknownConfIds().size());
        assertEquals(getConfIdEntity(), resourceStatus.getSortedUnknownConfIds().get(0));
    }

    private static void assertInstanceMismatch(
        RtcResourceStatus.InstanceMismatch expectedInstanceMismatch,
        RtcInstanceReport rtcInstanceReport,
        Optional<AgentMongoDao.ResourceInfoWrapper.ResourceInfoEntity> resourceInfo
    ) {
        RtcResourceStatus resourceStatus = runValidateReport(rtcInstanceReport, resourceInfo);
        assertFalse(resourceStatus.isValid());
        assertTrue(resourceStatus.getSortedUnknownConfIds().isEmpty());
        assertEquals(1, resourceStatus.getInstanceMismatches().size());
        assertEquals(expectedInstanceMismatch, resourceStatus.getInstanceMismatches().get(0));
    }

    private static RtcResourceStatus runValidateReport(
        RtcInstanceReport rtcInstanceReport,
        Optional<AgentMongoDao.ResourceInfoWrapper.ResourceInfoEntity> resourceInfo
    ) {
        RtcResourceStatus resourceStatus = new RtcResourceStatus("resourceType", PackageType.SOX);
        RtcReportValidationService.validateReport(
            "resourceType",
            new AgentMongoDao.RtcInstanceReportWrapper.RtcInstanceReportEntity(rtcInstanceReport),
            resourceStatus,
            resourceInfo,
            Collections.singleton(WHITELISTED_CONF_ID.getService())
        );
        return resourceStatus;
    }
}

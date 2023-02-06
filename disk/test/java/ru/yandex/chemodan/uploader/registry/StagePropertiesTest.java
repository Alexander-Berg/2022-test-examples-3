package ru.yandex.chemodan.uploader.registry;

import java.util.Properties;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.uploader.ChemodanFile;
import ru.yandex.chemodan.uploader.UidOrSpecial;
import ru.yandex.chemodan.uploader.registry.StagePropertiesTest.StagePropertiesTestContextConfiguration;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequest;
import ru.yandex.chemodan.uploader.registry.record.MpfsRequestRecord;
import ru.yandex.chemodan.uploader.registry.record.status.FileToZipInfo;
import ru.yandex.chemodan.uploader.registry.record.status.MpfsRequestStatus;
import ru.yandex.chemodan.uploader.registry.record.status.ZipResourceInfo;
import ru.yandex.commune.uploader.registry.CallbackResponseOption;
import ru.yandex.commune.uploader.registry.MutableState;
import ru.yandex.commune.uploader.registry.RequestMeta;
import ru.yandex.commune.uploader.registry.RequestRecord;
import ru.yandex.commune.uploader.registry.RequestRevision;
import ru.yandex.commune.uploader.registry.StageProperties;
import ru.yandex.commune.uploader.registry.StageProperties.Bundle;
import ru.yandex.commune.uploader.registry.State;
import ru.yandex.commune.uploader.registry.UploadRequestId;
import ru.yandex.commune.uploader.registry.UploadRequestStatus;
import ru.yandex.commune.uploader.util.HostInstant;
import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.dataSize.DataSize;
import ru.yandex.misc.net.HostnameUtils;
import ru.yandex.misc.property.PropertiesHolder;
import ru.yandex.misc.spring.context.EnvironmentTypeTestsContextConfiguration;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        ChemodanInitContextConfiguration.class,
        EnvironmentTypeTestsContextConfiguration.class,
        StagePropertiesTestContextConfiguration.class},
        loader = AnnotationConfigContextLoader.class)
public class StagePropertiesTest {

    @BeforeClass
    public static void beforeClass() {
        MpfsRequestStatus.registerStatuses();
    }

    @Configuration
    public static class StagePropertiesTestContextConfiguration {
        @Bean
        public static StageProperties stageProperties() {
            return new StageProperties();
        }
    }

    @Autowired
    private StageProperties stageProperties;


    @BeforeClass
    public static void setupProperties() {
        Properties properties = new Properties();

        properties.setProperty("uploader.director.stage.default.retry.delay", "10s");
        properties.setProperty("uploader.director.user.data.wait.start.timeout", "1m");
        properties.setProperty("uploader.director.user.data.max.upload.pause", "1m");

        properties.setProperty("uploader.director.stage.retry.policy..internalError", "illegal_policy");

        properties.setProperty("uploader.director.stage.retry.policy..notCancelledByAdmin", "linear");
        properties.setProperty("uploader.director.stage.retry.policy.linear.delay.base..notCancelledByAdmin", "3s");
        properties.setProperty("uploader.director.stage.retry.policy.linear.max.delay..notCancelledByAdmin", "25s");

        properties.setProperty("uploader.director.stage.max.concurrent..pp.antivirusResult2", "7");
        properties.setProperty("uploader.director.stage.max.concurrent..pp.pi.generatePreview2", "6");

        properties.setProperty("uploader.director.stage.max.concurrent.ZipFolder.ms.s.streamed", "15");

        properties.setProperty("uploader.director.stage.retry.policy.ZipFolder.ms.s.streamed", "linear");
        properties.setProperty("uploader.director.stage.retry.policy.linear.delay.base.ZipFolder.ms.s.streamed", "1s");
        properties.setProperty("uploader.director.stage.retry.policy.linear.max.delay.ZipFolder.ms.s.streamed", "1s");
        properties.setProperty("uploader.director.stage.max.retries.ZipFolder.ms.s.streamed", "3");

        PropertiesHolder.set(properties);
    }

    private RequestRecord consRecord() {
        Instant now = new Instant();
        HostInstant localNow = new HostInstant(HostnameUtils.localhost(), now);
        MpfsRequestRecord.UploadToDefault record = new MpfsRequestRecord.UploadToDefault(
                new RequestMeta(UploadRequestId.valueOf("requestid"), now),
                localNow,
                RequestRevision.initial(localNow),
                new MpfsRequest.UploadToDefault(
                        new ApiVersion("1.0"),
                        ChemodanFile.cons(UidOrSpecial.uid(PassportUid.cons(5181427L)), "uniquefield", "/path"),
                        Option.empty(), Option.empty(), Option.empty())
                );
        return record;
    }

    private RequestRecord consDynamicRecord() {
        Instant now = new Instant();
        HostInstant localNow = new HostInstant(HostnameUtils.localhost(), now);
        MpfsRequestRecord.ZipFolder record = new MpfsRequestRecord.ZipFolder(
                new RequestMeta(UploadRequestId.valueOf("requestid"), now),
                localNow,
                RequestRevision.initial(localNow),
                new MpfsRequest.ZipFolder(
                        new ApiVersion("1.0"), ChemodanFile.FAKE, Option.of("hash1234"), Option.empty(), Option.empty(), Option.empty())
                );
        return record;
    }


    @Test
    public void defaultRetryDelay() {
        RequestRecord record = consRecord();
        MutableState<CallbackResponseOption> state = ((UploadRequestStatus) record.getStatus()).commitFinal;

        Assert.equals(Duration.standardSeconds(10), stageProperties.getRetryDelay(record, state));
    }

    @Test
    public void illegalRetryDelayPolicy() {
        RequestRecord record = consRecord();
        MutableState<Boolean> state = record.getStatus().internalError;

        try {
            stageProperties.getRetryDelay(record, state);
            Assert.fail("there is an illegal policy used for internalError stage");
        } catch (Exception ok) {
        }
    }

    @Test
    public void linearRetryDelayPolicy() {
        RequestRecord record = consRecord();
        MutableState<Boolean> state = record.getStatus().notCancelledByAdmin;

        Assert.equals(Duration.standardSeconds(3), stageProperties.getRetryDelay(record, state));

        state.set(new State.Initial<Boolean>(4));
        Assert.equals(Duration.standardSeconds(15), stageProperties.getRetryDelay(record, state));

        state.set(new State.Initial<Boolean>(17));
        Assert.equals(Duration.standardSeconds(25), stageProperties.getRetryDelay(record, state));
    }

    @Test
    public void postProcessPropertyValue() {
        RequestRecord record = consRecord();
        MpfsRequestStatus.UploadToDefault status = (MpfsRequestStatus.UploadToDefault) record.getStatus();

        Bundle ppAv2 = stageProperties.getBundle(record, status.postProcess.antivirusResult2);
        Assert.equals(7, ppAv2.slots.get().permits);

        Bundle ppCfu = stageProperties.getBundle(record, status.postProcess.commitFileUpload);
        Assert.none(ppCfu.slots);
    }

    @Test
    public void dynamicFields() {
        RequestRecord record = consDynamicRecord();
        MpfsRequestStatus.ZipFolder status = (MpfsRequestStatus.ZipFolder) record.getStatus();

        FileToZipInfo info = new FileToZipInfo(
                "123", MulcaId.fromSerializedString("fileMulcaId"), DataSize.fromBytes(1), "", "");
        ZipResourceInfo fileZipResourceInfo = new ZipResourceInfo("/normalizedPath", Option.of(info));
        status.multipleStream.fill(Cf.list(fileZipResourceInfo));

        Bundle dynamicFile = stageProperties.getBundle(record, status.multipleStream.queue.first().streamed);
        Assert.equals(15, dynamicFile.slots.get().permits);

        Assert.equals(Duration.standardSeconds(1L), dynamicFile.retryDelayPolicy.getRetryDelay(10));

    }

}

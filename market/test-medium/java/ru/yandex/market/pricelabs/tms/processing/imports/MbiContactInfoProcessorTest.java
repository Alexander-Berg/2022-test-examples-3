package ru.yandex.market.pricelabs.tms.processing.imports;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.MbiContactInfo;
import ru.yandex.market.pricelabs.model.SourceMbiContactInfo;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.jobs.MbiContactInfoProcessingJob;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessor;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;
import ru.yandex.market.tms.quartz2.model.Executor;

public class MbiContactInfoProcessorTest extends
        AbstractYTImportingProcessorTest<SourceMbiContactInfo, MbiContactInfo> {

    @Autowired
    private MbiContactInfoProcessor processor;

    @Autowired
    private MbiContactInfoProcessingJob job;

    @Override
    protected AbstractYTImportingProcessor<SourceMbiContactInfo, MbiContactInfo> getProcessor() {
        return processor;
    }

    @Override
    protected Executor getJob() {
        return job;
    }

    @Override
    protected JobType getJobType() {
        return JobType.SYNC_MBI_CONTACT_INFO_PRIORITY;
    }

    @Override
    protected YtSourceTargetScenarioExecutor<SourceMbiContactInfo, MbiContactInfo> newExecutor() {
        return executors.mbiContactInfoExecutor();
    }

    @Override
    protected List<MbiContactInfo> updateRows(List<MbiContactInfo> oldRows, List<MbiContactInfo> newRows) {

        var updatedRowsMap = oldRows.stream()
                .peek(row -> row.setStatus(Status.DELETED))
                .collect(Collectors.toMap(
                        mbiContactInfo -> new MbiContactKey(mbiContactInfo.getPartner_id(),
                                mbiContactInfo.getContact_id(), mbiContactInfo.getUser_type()
                        ),
                        Function.identity()
                ));

        newRows.forEach(
                row -> updatedRowsMap.put(
                        new MbiContactKey(row.getPartner_id(), row.getContact_id(), row.getUser_type()),
                        row
                )
        );

        return updatedRowsMap.values()
                .stream()
                .sorted(Comparator.comparing(MbiContactInfo::getContact_id)
                        .thenComparing(MbiContactInfo::getPartner_id)
                        .thenComparing(MbiContactInfo::getUser_type)
                )
                .collect(Collectors.toList());
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/imports/mbi_contact_info_target.csv";
    }

    @Override
    protected String getSourceCsv() {
        return "tms/processing/imports/mbi_contact_info_source.csv";
    }

    @Override
    protected String getSourceCsv2() {
        return "tms/processing/imports/mbi_contact_info_source2.csv";
    }

    @Override
    protected String getTargetCsv2() {
        return "tms/processing/imports/mbi_contact_info_target2.csv";
    }

    @Override
    protected Class<MbiContactInfo> getTargetClass() {
        return MbiContactInfo.class;
    }

    @Override
    protected Class<SourceMbiContactInfo> getSourceClass() {
        return SourceMbiContactInfo.class;
    }

    @Data
    @AllArgsConstructor
    private static class MbiContactKey {
        private long partnerId;
        private long contactId;
        private String userType;
    }
}

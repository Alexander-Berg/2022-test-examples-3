package ru.yandex.market.crm.triggers.test;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.crm.core.services.bpm.CrmProcessEngineConfiguration;
import ru.yandex.market.crm.triggers.BpmConfiguration;
import ru.yandex.market.crm.triggers.services.bpm.TriggersDAO;
import ru.yandex.market.crm.triggers.services.bpm.correlation.CorrelationKeyProvider;
import ru.yandex.market.crm.triggers.services.bpm.jobs.CrmJobExecutor;
import ru.yandex.market.crm.triggers.services.bpm.meta.TriggerMessageTypesRepository;
import ru.yandex.market.crm.triggers.services.parse.CrmBpmParseFactory;
import ru.yandex.market.crm.triggers.services.segments.TriggerSegmentService;
import ru.yandex.market.crm.triggers.test.helpers.BpmProcessSpy;
import ru.yandex.market.crm.triggers.test.helpers.BpmProcessSpyParseListener;
import ru.yandex.market.mcrm.db.Constants;

@Configuration
public class TestBpmConfig extends BpmConfiguration {

    private final BpmProcessSpy processSpy;

    public TestBpmConfig(TriggersDAO triggersDAO,
                         TriggerSegmentService segmentService,
                         @Named(Constants.DEFAULT_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate,
                         BpmProcessSpy processSpy,
                         CrmBpmParseFactory crmBpmParseFactory,
                         RuntimeService runtimeService,
                         TriggerMessageTypesRepository messageTypesRepository,
                         CorrelationKeyProvider correlationKeyProvider,
                         CrmJobExecutor jobExecutor) {
        super(
                triggersDAO,
                segmentService,
                jdbcTemplate,
                crmBpmParseFactory,
                runtimeService,
                messageTypesRepository,
                correlationKeyProvider,
                jobExecutor
        );
        this.processSpy = processSpy;
    }

    @Override
    protected void configure(CrmProcessEngineConfiguration config) {
        super.configure(config);
        List<BpmnParseListener> bpmnParseListeners = config.getCustomPostBPMNParseListeners();
        if (null == bpmnParseListeners) {
            bpmnParseListeners = new ArrayList<>();
            config.setCustomPostBPMNParseListeners(bpmnParseListeners);
        }
        bpmnParseListeners.add(new BpmProcessSpyParseListener(processSpy));
    }
}

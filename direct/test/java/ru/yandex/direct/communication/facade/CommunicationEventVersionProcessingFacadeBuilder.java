package ru.yandex.direct.communication.facade;

import java.util.Collections;
import java.util.List;

import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;

import static java.util.Collections.emptyList;

public class CommunicationEventVersionProcessingFacadeBuilder {

    private List<CommunicationEventVersionsProcessor> processors = emptyList();
    private List<ActualChecker> actualCheckers = emptyList();
    private List<StatusChecker> statusCheckers = emptyList();
    private List<CommunicationMessageFormatter> formatters = emptyList();
    private List<ActionHandler> actionHandlers = emptyList();
    private List<MessageSender> senders = emptyList();
    private CampaignRepository campaignRepository;
    private List<AutoUpdatedSettingsEventFormatter> eventFormatters = Collections.emptyList();

    public CommunicationEventVersionProcessingFacadeBuilder withProcessors(List<CommunicationEventVersionsProcessor> processors) {
        this.processors = processors;
        return this;
    }

    public CommunicationEventVersionProcessingFacadeBuilder withActualCheckers(List<ActualChecker> actualCheckers) {
        this.actualCheckers = actualCheckers;
        return this;
    }

    public CommunicationEventVersionProcessingFacadeBuilder withStatusCheckers(List<StatusChecker> statusCheckers) {
        this.statusCheckers = statusCheckers;
        return this;
    }

    public CommunicationEventVersionProcessingFacadeBuilder withFormatters(List<CommunicationMessageFormatter> formatters) {
        this.formatters = formatters;
        return this;
    }

    public CommunicationEventVersionProcessingFacadeBuilder withEventFormatters(List<AutoUpdatedSettingsEventFormatter> eventFormatters) {
        this.eventFormatters = eventFormatters;
        return this;
    }

    public CommunicationEventVersionProcessingFacadeBuilder withActionHandlers(List<ActionHandler> actionHandlers) {
        this.actionHandlers = actionHandlers;
        return this;
    }

    public CommunicationEventVersionProcessingFacadeBuilder withSenders(List<MessageSender> senders) {
        this.senders = senders;
        return this;
    }

    public CommunicationEventVersionProcessingFacadeBuilder withCampaignRepository(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
        return this;
    }

    public CommunicationEventVersionProcessingFacade build() {
        return new CommunicationEventVersionProcessingFacade(
                processors, actualCheckers, statusCheckers, formatters, emptyList(),
                actionHandlers, senders, emptyList(), campaignRepository, eventFormatters);
    }
}

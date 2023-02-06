package ru.yandex.market.crm.campaign.dto.sending;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.market.crm.campaign.domain.sending.TestEmailsGroup;

public class TestSendEmailPromoRequest {
    @JsonProperty("groups")
    private List<TestEmailsGroup> emailGroups;
    @JsonProperty("messagesCount")
    private Integer messagesCount;

    public List<TestEmailsGroup> getEmailGroups() {
        return emailGroups;
    }

    public TestSendEmailPromoRequest setEmailGroups(List<TestEmailsGroup> emailGroups) {
        this.emailGroups = emailGroups;
        return this;
    }

    public Integer getMessagesCount() {
        return messagesCount;
    }

    public TestSendEmailPromoRequest setMessagesCount(int messagesCount) {
        this.messagesCount = messagesCount;
        return this;
    }
}

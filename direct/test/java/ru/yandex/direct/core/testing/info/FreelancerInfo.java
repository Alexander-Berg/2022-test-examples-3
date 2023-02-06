package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.dbutil.model.ClientId;

public class FreelancerInfo {

    private ClientInfo clientInfo = new ClientInfo();
    private Freelancer freelancer;

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public FreelancerInfo withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public Freelancer getFreelancer() {
        return freelancer;
    }

    public FreelancerInfo withFreelancer(Freelancer freelancer) {
        this.freelancer = freelancer;
        return this;
    }

    public Long getFreelancerId() {
        return getFreelancer().getId();
    }

    public ClientId getClientId() {
        return getClientInfo().getClientId();
    }

    public Integer getShard() {
        return getClientInfo().getShard();
    }
}

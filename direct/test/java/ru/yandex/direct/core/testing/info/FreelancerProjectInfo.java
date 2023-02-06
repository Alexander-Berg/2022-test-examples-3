package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.dbutil.model.ClientId;

public class FreelancerProjectInfo {
    private ClientInfo clientInfo = new ClientInfo();
    private FreelancerInfo freelancerInfo;
    private FreelancerProject project;

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public FreelancerProjectInfo withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public FreelancerInfo getFreelancerInfo() {
        return freelancerInfo;
    }

    public FreelancerProjectInfo withFreelancerInfo(FreelancerInfo freelancerInfo) {
        this.freelancerInfo = freelancerInfo;
        return this;
    }

    public FreelancerProject getProject() {
        return project;
    }

    public Long getProjectId() {
        return getProject().getId();
    }

    public FreelancerProjectInfo withProject(FreelancerProject project) {
        this.project = project;
        return this;
    }

    public Integer getShard() {
        return getClientInfo().getShard();
    }

    public ClientId getClientId() {
        return getClientInfo().getClientId();
    }

    public ClientId getFreelancerId() {
        return getFreelancerInfo().getClientId();
    }
}

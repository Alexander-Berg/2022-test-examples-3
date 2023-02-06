package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.freelancer.model.ClientAvatar;

public class ClientAvatarInfo {

    private FreelancerInfo freelancerInfo;
    private ClientAvatar clientAvatar;

    public FreelancerInfo getFreelancerInfo() {
        return freelancerInfo;
    }

    public ClientAvatarInfo withFreelancerInfo(FreelancerInfo freelancerInfo) {
        this.freelancerInfo = freelancerInfo;
        return this;
    }

    public ClientAvatar getClientAvatar() {
        return clientAvatar;
    }

    public ClientAvatarInfo withClientAvatar(ClientAvatar clientAvatar) {
        this.clientAvatar = clientAvatar;
        return this;
    }

    public Long getFreelancerId() {
        return getFreelancerInfo().getFreelancer().getId();
    }
}

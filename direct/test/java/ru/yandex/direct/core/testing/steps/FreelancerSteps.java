package ru.yandex.direct.core.testing.steps;

import java.util.List;

import javax.annotation.Nullable;

import one.util.streamex.StreamEx;

import ru.yandex.direct.core.entity.freelancer.container.FreelancerProjectQueryContainer;
import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerProjectRepository;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.info.FreelancerProjectInfo;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancer;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancerProject;

public class FreelancerSteps {
    private final ClientSteps clientSteps;
    private final FreelancerRepository freelancerRepository;
    private final FreelancerProjectRepository freelancerProjectRepository;

    public FreelancerSteps(ClientSteps clientSteps,
                           FreelancerRepository freelancerRepository,
                           FreelancerProjectRepository freelancerProjectRepository) {
        this.clientSteps = clientSteps;
        this.freelancerRepository = freelancerRepository;
        this.freelancerProjectRepository = freelancerProjectRepository;
    }

    public FreelancerInfo addDefaultFreelancer() {
        return createFreelancer(new FreelancerInfo());
    }

    public FreelancerInfo createFreelancer(FreelancerInfo freelancerInfo) {
        if (freelancerInfo.getFreelancer() == null) {
            if (freelancerInfo.getClientInfo().getClient() == null) {
                ClientInfo clientInfo = clientSteps.createClient(freelancerInfo.getClientInfo());
                freelancerInfo.withClientInfo(clientInfo);
            }
            long clientId = freelancerInfo.getClientId().asLong();
            Freelancer defaultFreelancer = defaultFreelancer(clientId);
            freelancerInfo.withFreelancer(defaultFreelancer);
        }
        Freelancer freelancer = freelancerInfo.getFreelancer();
        freelancerRepository.addFreelancers(freelancerInfo.getShard(), singletonList(freelancer));
        return freelancerInfo;
    }

    public FreelancerProjectInfo createDefaultProject() {
        return createProject(new FreelancerProjectInfo());
    }

    public FreelancerProjectInfo createProjectByClient(ClientInfo clientInfo) {
        return createProject(new FreelancerProjectInfo().withClientInfo(clientInfo));
    }

    public FreelancerProjectInfo createProject(FreelancerProjectInfo projectInfo) {
        if (projectInfo.getFreelancerInfo() == null) {
            projectInfo.withFreelancerInfo(addDefaultFreelancer());
        }

        if (projectInfo.getClientInfo() == null) {
            projectInfo.withClientInfo(clientSteps.createDefaultClient());
        }

        if (projectInfo.getProject() == null) {
            projectInfo.withProject(defaultFreelancerProject(projectInfo.getClientInfo().getClientId().asLong(),
                    projectInfo.getFreelancerInfo().getClientId().asLong()));
        }
        freelancerProjectRepository.add(projectInfo.getShard(), singletonList(projectInfo.getProject()));

        return projectInfo;
    }

    public List<FreelancerProject> getProjects(int shard, FreelancerProjectQueryContainer container) {
        return freelancerProjectRepository.get(shard, container, LimitOffset.maxLimited());
    }

    @Nullable
    public FreelancerProject getProject(int shard, Long id) {
        return StreamEx.of(getProjects(shard,
                FreelancerProjectQueryContainer.builder().withProjectIds(id).build()))
                .findFirst()
                .orElse(null);
    }
}

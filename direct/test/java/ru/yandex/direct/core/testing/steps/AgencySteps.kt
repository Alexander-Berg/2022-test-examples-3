package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.yandex.direct.core.testing.repository.TestAgencyRepository
import ru.yandex.direct.dbutil.model.ClientId

@Component
class AgencySteps {

    @Autowired
    private lateinit var testAgencyRepository: TestAgencyRepository;

    fun linkLimRepToClient(shard: Int, limRepUid: Long, clientId: ClientId) {
        return linkLimRepsToClient(shard, setOf(limRepUid), clientId);
    }

    fun linkLimRepsToClient(shard: Int, limRepUids: Collection<Long>, clientId: ClientId) {
        return testAgencyRepository.linkLimRepToClient(shard, limRepUids, clientId.asLong())
    }

    fun unlinkAllLimRepsFromClient(shard: Int, clientId: ClientId) {
        return testAgencyRepository.unlinkLimRepToClient(shard, clientId.asLong());
    }
}

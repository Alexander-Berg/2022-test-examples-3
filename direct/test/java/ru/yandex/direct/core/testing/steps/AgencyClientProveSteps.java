package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.client.model.AgencyClientProve;
import ru.yandex.direct.core.entity.client.repository.AgencyClientProveRepository;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.jooq.impl.DSL.max;
import static ru.yandex.direct.dbschema.ppc.tables.AgencyClientProve.AGENCY_CLIENT_PROVE;

@QueryWithoutIndex("Тестовые степы")
public class AgencyClientProveSteps {

    private static final Long DEFAULT_AGENCY_UID = 1L;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private AgencyClientProveRepository agencyClientProveRepository;

    public AgencyClientProve createDefaultAgencyClientProve(int shard) {
        return createAgencyClientProveWithDateTimeAndIsConfirmed(shard, LocalDateTime.now(), false);
    }

    public AgencyClientProve createAgencyClientProveWithDateTimeAndIsConfirmed(int shard, LocalDateTime dateTime,
                                                                               Boolean isConfirmed) {
        return createAgencyClientProve(shard,
                getNextAgencyClientId(shard),
                DEFAULT_AGENCY_UID,
                getNextClientUid(shard),
                isConfirmed,
                dateTime
        );
    }

    public AgencyClientProve createAgencyClientProve(int shard, Long agencyClientId, Long agencyUid, Long clientUid,
                                                     Boolean isConfirmed, LocalDateTime requestTime) {
        AgencyClientProve requestToInsert = new AgencyClientProve()
                .withAgencyClientId(agencyClientId)
                .withAgencyUid(agencyUid)
                .withClientUid(clientUid)
                .withIsConfirmed(isConfirmed)
                .withRequestTime(requestTime);
        agencyClientProveRepository.addNewRequest(shard, requestToInsert);
        return requestToInsert;
    }

    public List<AgencyClientProve> getAllClientAgencyProve(int shard) {
        return agencyClientProveRepository.getAllRequests(shard);
    }

    // не хочется такой запрос переносить в репозиторий
    // возможно в будущем стоит поменять на вызов степа создания нового клиента
    private Long getNextAgencyClientId(int shard) {
        Long maxAgencyClientId = dslContextProvider.ppc(shard)
                .select(max(AGENCY_CLIENT_PROVE.AGENCY_CLIENT_ID))
                .from(AGENCY_CLIENT_PROVE)
                .fetchOne(0, Long.class);
        return maxAgencyClientId != null ? maxAgencyClientId + 1 : 1;
    }

    // не хочется такой запрос переносить в репозиторий
    // возможно в будущем стоит поменять на вызов степа создания нового пользователя (когда появится)
    private Long getNextClientUid(int shard) {
        Long maxClientUid = dslContextProvider.ppc(shard)
                .select(max(AGENCY_CLIENT_PROVE.CLIENT_UID))
                .from(AGENCY_CLIENT_PROVE)
                .fetchOne(0, Long.class);
        return maxClientUid != null ? maxClientUid + 1 : 1;
    }

}

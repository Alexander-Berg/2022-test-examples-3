package ru.yandex.direct.core.testing.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.ClientFlags;
import ru.yandex.direct.core.entity.client.model.TinType;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsMapping;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.dbschema.ppc.enums.ClientsAgencyStatus;
import ru.yandex.direct.dbschema.ppc.enums.ClientsAllowCreateScampBySubclient;
import ru.yandex.direct.dbschema.ppc.enums.ClientsOptionsStatusbalancebanned;
import ru.yandex.direct.dbschema.ppc.enums.ClientsWorkCurrency;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacSubrole;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.common.util.RepositoryUtils.FALSE;
import static ru.yandex.direct.common.util.RepositoryUtils.TRUE;
import static ru.yandex.direct.dbschema.ppc.tables.AgencyManagers.AGENCY_MANAGERS;
import static ru.yandex.direct.dbschema.ppc.tables.ClientManagers.CLIENT_MANAGERS;
import static ru.yandex.direct.dbschema.ppc.tables.Clients.CLIENTS;
import static ru.yandex.direct.dbschema.ppc.tables.ClientsOptions.CLIENTS_OPTIONS;
import static ru.yandex.direct.dbschema.ppc.tables.ManagerHierarchy.MANAGER_HIERARCHY;
import static ru.yandex.direct.dbschema.ppcdict.tables.ShardClientId.SHARD_CLIENT_ID;
import static ru.yandex.direct.dbschema.ppcdict.tables.ShardUid.SHARD_UID;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class TestClientRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ClientRepository clientRepository;

    /**
     * Добавляет клиента в таблицу clients и создает связку клиент-шард в ppcdict.
     * Объединяет {@link TestClientRepository#addClientIdToPpcdict(int, long)} и
     * {@link TestClientRepository#addClientToClientsTable(int, Client)}
     *
     * @param shard  Шард
     * @param client Клиент
     */
    public void addClient(int shard, Client client) {
        addClientIdToPpcdict(shard, client.getId());
        addClientToClientsTable(shard, client);
        addClientToClientsOptionsTable(shard, client);
    }

    /**
     * Удаляет клиента из базы. Аналогично {@link TestClientRepository#deleteClientFromClientsTable(int, ClientId)}
     *
     * @param shard    Шард
     * @param clientId ID клиента
     */
    public void deleteClient(int shard, ClientId clientId) {
        deleteClientFromClientsTable(shard, clientId);
    }

    /**
     * Добавляет связку клиент-шард в ppcdict
     *
     * @param shard    Шард
     * @param clientId ID клиента
     */
    private void addClientIdToPpcdict(int shard, long clientId) {
        dslContextProvider.ppcdict()
                .insertInto(SHARD_CLIENT_ID,
                        SHARD_CLIENT_ID.CLIENT_ID,
                        SHARD_CLIENT_ID.SHARD)
                .values(clientId, (long) shard)
                .execute();
    }

    /**
     * Добавляет нового клиента в таблицу clients
     *
     * @param shard  Шард
     * @param client Заполненная модель {@link Client}
     */
    private void addClientToClientsTable(int shard, Client client) {
        boolean faviconBlocked = client.getFaviconBlocked() != null ? client.getFaviconBlocked() : false;
        dslContextProvider.ppc(shard)
                .insertInto(CLIENTS,
                        CLIENTS.CLIENT_ID,
                        CLIENTS.CHIEF_UID,
                        CLIENTS.ROLE,
                        CLIENTS.SUBROLE,
                        CLIENTS.NAME,
                        CLIENTS.WORK_CURRENCY,
                        CLIENTS.CREATE_DATE,
                        CLIENTS.DELETED_REPS,
                        CLIENTS.AGENCY_URL,
                        CLIENTS.AGENCY_STATUS,
                        CLIENTS.PRIMARY_MANAGER_UID,
                        CLIENTS.PRIMARY_BAYAN_MANAGER_UID,
                        CLIENTS.PRIMARY_GEO_MANAGER_UID,
                        CLIENTS.ALLOW_CREATE_SCAMP_BY_SUBCLIENT,
                        CLIENTS.COUNTRY_REGION_ID,
                        CLIENTS.IS_FAVICON_BLOCKED,
                        CLIENTS.AGENCY_CLIENT_ID,
                        CLIENTS.AGENCY_UID,
                        CLIENTS.CONNECT_ORG_ID)
                .values(
                        client.getId(),
                        client.getChiefUid(),
                        RbacRole.toSource(client.getRole()),
                        RbacSubrole.toSource(client.getSubRole()),
                        client.getName(),
                        client.getWorkCurrency() != null ?
                                ClientsWorkCurrency.valueOf(client.getWorkCurrency().name()) : null,
                        client.getCreateDate(),
                        client.getDeletedReps(),
                        client.getAgencyUrl(),
                        client.getAgencyStatus() == null ? null :
                                ClientsAgencyStatus.valueOf(client.getAgencyStatus().name()),
                        client.getPrimaryManagerUid(),
                        client.getPrimaryBayanManagerUid(),
                        client.getPrimaryGeoManagerUid(),
                        client.getAllowCreateScampBySubclient() ?
                                ClientsAllowCreateScampBySubclient.Yes : ClientsAllowCreateScampBySubclient.No,
                        client.getCountryRegionId(),
                        faviconBlocked ? TRUE : FALSE,
                        client.getAgencyClientId(),
                        client.getAgencyUserId(),
                        client.getConnectOrgId())
                .execute();
    }

    /**
     * Добавляет нового клиента в таблицу clients_options
     *
     * @param shard  Шард
     * @param client Заполненная модель {@link Client}
     */
    private void addClientToClientsOptionsTable(int shard, Client client) {
        var nonResident = client.getNonResident() != null && client.getNonResident();
        var hideMarketRating = client.getHideMarketRating() != null && client.getHideMarketRating();
        var isBusinessUnit = client.getIsBusinessUnit() != null && client.getIsBusinessUnit();
        var socialAdvertising = client.getSocialAdvertising() != null && client.getSocialAdvertising();
        var clientFlags = new HashSet<ClientFlags>();
        var asSoonAsPossible = client.getAsSoonAsPossible() != null && client.getAsSoonAsPossible();
        if (asSoonAsPossible) {
            clientFlags.add(ClientFlags.AS_SOON_AS_POSSIBLE);
        }
        dslContextProvider.ppc(shard)
                .insertInto(CLIENTS_OPTIONS)
                .set(CLIENTS_OPTIONS.CLIENT_ID, client.getId())
                .set(CLIENTS_OPTIONS.BALANCE_TID, 0L)
                .set(CLIENTS_OPTIONS.OVERDRAFT_LIM, client.getOverdraftLimit() == null ? BigDecimal.ZERO :
                        client.getOverdraftLimit())
                .set(CLIENTS_OPTIONS.DEBT, client.getDebt() == null ? BigDecimal.ZERO :
                        client.getDebt())
                .set(CLIENTS_OPTIONS.NEXT_PAY_DATE, LocalDate.now())
                .set(CLIENTS_OPTIONS.STATUS_BALANCE_BANNED,
                        client.getStatusBalanceBanned() == Boolean.TRUE ?
                                ClientsOptionsStatusbalancebanned.Yes : ClientsOptionsStatusbalancebanned.No)
                .set(CLIENTS_OPTIONS.AUTO_OVERDRAFT_LIM,
                        client.getAutoOverdraftLimit() == null ? BigDecimal.ZERO : client.getAutoOverdraftLimit())
                .set(CLIENTS_OPTIONS.NON_RESIDENT, nonResident ? TRUE : FALSE)
                .set(CLIENTS_OPTIONS.HIDE_MARKET_RATING, hideMarketRating ? TRUE : FALSE)
                .set(CLIENTS_OPTIONS.IS_BUSINESS_UNIT, isBusinessUnit ? TRUE : FALSE)
                .set(CLIENTS_OPTIONS.SOCIAL_ADVERTISING, socialAdvertising ? TRUE : FALSE)
                .set(CLIENTS_OPTIONS.TIN, client.getTin())
                .set(CLIENTS_OPTIONS.TIN_TYPE, TinType.toSource(client.getTinType()))
                .set(CLIENTS_OPTIONS.CLIENT_FLAGS,
                        clientFlags.stream().map(ClientFlags::getTypedValue).collect(Collectors.joining(",")))
                .execute();
    }

    public ClientId getClientIdByUid(Long uid) {
        if (uid == null) {
            return null;
        }
        return dslContextProvider.ppcdict()
                .select(SHARD_UID.CLIENT_ID)
                .from(SHARD_UID)
                .where(SHARD_UID.UID.eq(uid))
                .fetchOne(r -> ClientId.fromNullableLong(r.value1()));
    }

    public void addCommonMetrikaCounters(int shard, ClientId clientId, @Nullable List<Long> counters) {
        String commonMetrikaCounters = ClientOptionsMapping.commonMetrikaCountersToDb(counters);
        dslContextProvider.ppc(shard)
                .update(CLIENTS_OPTIONS)
                .set(CLIENTS_OPTIONS.COMMON_METRIKA_COUNTERS, commonMetrikaCounters)
                .where(CLIENTS_OPTIONS.CLIENT_ID.eq(clientId.asLong()))
                .execute();
    }

    /**
     * Обновить регион клиента
     *
     * @param shard    шард
     * @param clientId ID клиента
     * @param regionId ID региона
     */
    public int setClientRegionId(int shard, ClientId clientId, Long regionId) {
        return dslContextProvider.ppc(shard).update(CLIENTS)
                .set(CLIENTS.COUNTRY_REGION_ID, regionId)
                .where(CLIENTS.CLIENT_ID.eq(clientId.asLong()))
                .execute();
    }

    /**
     * Удаляет клиента из таблицы clients.
     *
     * @param shard    Шард
     * @param clientId ID клиента
     */
    private void deleteClientFromClientsTable(int shard, ClientId clientId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(CLIENTS)
                .where(CLIENTS.CLIENT_ID.eq(clientId.asLong()))
                .execute();
    }

    private void deleteClientsOptionsFromClientsOptionsTable(int shard, ClientId clientId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(CLIENTS_OPTIONS)
                .where(CLIENTS_OPTIONS.CLIENT_ID.eq(clientId.asLong()))
                .execute();
    }

    public void setManagerHierarchy(ClientInfo leader, Collection<ClientInfo> subordinates)
            throws JsonProcessingException {
        TypeReference<List<Long>> type = new TypeReference<>() {
        };
        ObjectWriter mapper = MAPPER.writerFor(type);
        String subordinatesClientIds = mapper.writeValueAsString(mapList(subordinates, s -> s.getClientId().asLong()));
        String subordinatedUids = mapper.writeValueAsString(mapList(subordinates, s -> s.getUid()));
        dslContextProvider.ppc(leader.getShard())
                .insertInto(MANAGER_HIERARCHY)
                .set(MANAGER_HIERARCHY.MANAGER_UID, leader.getUid())
                .set(MANAGER_HIERARCHY.MANAGER_CLIENT_ID, leader.getClientId().asLong())
                .set(MANAGER_HIERARCHY.SUPERVISOR_UID, 0L)
                .set(MANAGER_HIERARCHY.SUPERVISOR_CLIENT_ID, 0L)
                .set(MANAGER_HIERARCHY.SUBORDINATES_CLIENT_ID, subordinatesClientIds)
                .set(MANAGER_HIERARCHY.SUBORDINATES_UID, subordinatedUids)
                .onDuplicateKeyUpdate()
                .set(MANAGER_HIERARCHY.SUBORDINATES_CLIENT_ID, subordinatesClientIds)
                .set(MANAGER_HIERARCHY.SUBORDINATES_UID, subordinatedUids)
                .execute();
        for (ClientInfo subordinate : subordinates) {
            String chiefsClientIds = mapper.writeValueAsString(singletonList(leader.getClientId().asLong()));
            String chiefUids = mapper.writeValueAsString(singletonList(leader.getUid()));
            dslContextProvider.ppc(subordinate.getShard())
                    .insertInto(MANAGER_HIERARCHY)
                    .set(MANAGER_HIERARCHY.MANAGER_UID, subordinate.getUid())
                    .set(MANAGER_HIERARCHY.MANAGER_CLIENT_ID, subordinate.getClientId().asLong())
                    .set(MANAGER_HIERARCHY.SUPERVISOR_UID, leader.getUid())
                    .set(MANAGER_HIERARCHY.SUPERVISOR_CLIENT_ID, leader.getClientId().asLong())
                    .set(MANAGER_HIERARCHY.CHIEFS_CLIENT_ID, chiefsClientIds)
                    .set(MANAGER_HIERARCHY.CHIEFS_UID, chiefUids)
                    .onDuplicateKeyUpdate()
                    .set(MANAGER_HIERARCHY.SUPERVISOR_UID, leader.getUid())
                    .set(MANAGER_HIERARCHY.SUPERVISOR_CLIENT_ID, leader.getClientId().asLong())
                    .set(MANAGER_HIERARCHY.CHIEFS_CLIENT_ID, chiefsClientIds)
                    .set(MANAGER_HIERARCHY.CHIEFS_UID, chiefUids)
                    .execute();
        }
    }

    public void bindManagerToClient(int shard, ClientId clientId, Long managerUid) {
        dslContextProvider.ppc(shard)
                .insertInto(CLIENT_MANAGERS,
                        CLIENT_MANAGERS.CLIENT_ID,
                        CLIENT_MANAGERS.MANAGER_UID)
                .values(clientId.asLong(), managerUid)
                .onDuplicateKeyIgnore()
                .execute();
    }

    public void setManagerToClient(int shard, ClientId clientId, Long managerUid) {
        dslContextProvider.ppc(shard)
                .update(CLIENTS)
                .set(CLIENTS.PRIMARY_MANAGER_UID, managerUid)
                .where(CLIENTS.CLIENT_ID.eq(clientId.asLong()))
                .execute();
    }

    /**
     * Задать клиенту ClientID и UID агентства
     */
    public ClientId setAgencyToClient(int shard, ClientId agencyClientId, Long agencyUid, ClientId clientId) {
        return setAgencyToClient(shard, UidAndClientId.of(agencyUid, agencyClientId), singleton(clientId)).get(0);

    }

    /**
     * Привязать заданных клиентов к заданному агентству в RBAC-ке
     *
     * @return Множество Id-ков успешно привязанных клиентов
     */
    public List<ClientId> setAgencyToClient(int shard, UidAndClientId agency, Set<ClientId> clientIds) {
        if (clientIds.isEmpty()) {
            return emptyList();
        }

        List<ClientId> result = new ArrayList<>(clientIds.size());

        for (ClientId clientId : clientIds) {
            dslContextProvider.ppc(shard)
                    .update(CLIENTS)
                    .set(CLIENTS.AGENCY_CLIENT_ID, agency.getClientId().asLong())
                    .set(CLIENTS.AGENCY_UID, agency.getUid())
                    .where(CLIENTS.CLIENT_ID.eq(clientId.asLong()))
                    .execute();
            result.add(clientId);
        }

        return result;
    }

    public List<Long> getBindedClientsToManager(int shard, Long managerUid) {
        return dslContextProvider.ppc(shard)
                .select(CLIENT_MANAGERS.CLIENT_ID)
                .from(CLIENT_MANAGERS)
                .where(CLIENT_MANAGERS.MANAGER_UID.eq(managerUid))
                .fetch(CLIENT_MANAGERS.CLIENT_ID);
    }

    public void bindManagerToAgency(int shard, ClientId agencyClientId, User manager) {
        dslContextProvider.ppc(shard)
                .insertInto(AGENCY_MANAGERS,
                        AGENCY_MANAGERS.AGENCY_CLIENT_ID,
                        AGENCY_MANAGERS.MANAGER_CLIENT_ID,
                        AGENCY_MANAGERS.MANAGER_UID)
                .values(agencyClientId.asLong(), manager.getClientId().asLong(), manager.getUid())
                .onDuplicateKeyIgnore()
                .execute();
    }
}

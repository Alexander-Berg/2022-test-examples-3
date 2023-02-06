package ru.yandex.direct.core.entity.user.repository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.model.UsersBlockReasonType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.ClientsAutobanIsAutobanned;
import ru.yandex.direct.dbschema.ppc.enums.InternalUsersIsDeveloper;
import ru.yandex.direct.dbschema.ppc.enums.UsersOptionsStatuseasy;
import ru.yandex.direct.dbschema.ppc.enums.UsersSendaccnews;
import ru.yandex.direct.dbschema.ppc.enums.UsersSendnews;
import ru.yandex.direct.dbschema.ppc.enums.UsersSendwarn;
import ru.yandex.direct.dbschema.ppc.enums.UsersStatusarch;
import ru.yandex.direct.dbschema.ppc.enums.UsersStatusblocked;
import ru.yandex.direct.dbschema.ppc.tables.records.ClientsAutobanRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.InternalUsersRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.UsersOptionsRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.UsersRecord;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.rbac.ClientPerm;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacUserLookupService;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.user.UserLangUtils.INT_UKRAINIAN_LANG_SUBTAG;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.dbschema.ppc.tables.ClientsAutoban.CLIENTS_AUTOBAN;
import static ru.yandex.direct.dbschema.ppc.tables.InternalUsers.INTERNAL_USERS;
import static ru.yandex.direct.dbschema.ppc.tables.Users.USERS;
import static ru.yandex.direct.dbschema.ppc.tables.UsersOptions.USERS_OPTIONS;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UserRepositoryFetchByUidsTest {

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private Steps steps;

    private static final Random RANDOM = new Random();

    private UserRepository userRepository;
    private UserInfo userInfo1;
    private UserInfo userInfo2;
    private User user1;
    private User user2;
    private Integer shard;
    private Map<ClientId, Long> clientIdToChiefMap;
    private Map<Long, RbacRole> uidToRbacRoleMap;

    @Before
    public void setUp() throws Exception {
        long chief1 = Math.abs(RANDOM.nextLong());
        user1 = generateNewUserWithAppropriateFields();
        userInfo1 = steps.userSteps().createUser(user1);

        long chief2 = Math.abs(RANDOM.nextLong());
        user2 = generateNewUserWithAppropriateFields();
        userInfo2 = steps.userSteps().createUser(user2);

        if (userInfo1.getShard() != userInfo2.getShard()) {
            throw new IllegalStateException("users not created on the same shard");
        }

        shard = userInfo1.getShard();

        clientIdToChiefMap = new HashMap<>(ImmutableMap.<ClientId, Long>builder()
                .put(userInfo1.getClientInfo().getClientId(), chief1)
                .put(userInfo2.getClientInfo().getClientId(), chief2)
                .build());

        uidToRbacRoleMap = new HashMap<>(ImmutableMap.<Long, RbacRole>builder()
                .put(userInfo1.getUid(), RbacRole.CLIENT)
                .put(userInfo2.getUid(), RbacRole.AGENCY)
                .build());

        user1
                .withPassportKarma(0L)
                .withChiefUid(clientIdToChiefMap.get(user1.getClientId()))
                .withRole(uidToRbacRoleMap.get(user1.getUid()))
                .withSuperManager(false)
                .withMetrikaCountersNum(0)
                .withClientCreateDate(clientRepository.get(shard,
                        singletonList(user1.getClientId())).get(0).getCreateDate());

        user2
                .withPassportKarma(0L)
                .withChiefUid(clientIdToChiefMap.get(user2.getClientId()))
                .withPassportKarma(0L)
                .withRole(uidToRbacRoleMap.get(user2.getUid()))
                .withSuperManager(false)
                .withMetrikaCountersNum(0)
                .withClientCreateDate(clientRepository.get(shard,
                        singletonList(user2.getClientId())).get(0).getCreateDate());

        DSLContext dslContext = dslContextProvider.ppc(shard);

        saveUser(dslContext, user1);
        saveUser(dslContext, user2);

        userRepository = new UserRepository(dslContextProvider, new RbacUserLookupService() {
            @Override
            public Map<ClientId, Long> getChiefsByClientIds(Collection<ClientId> clientIds) {
                return clientIds.stream().collect(toMap(identity(), clientIdToChiefMap::get));
            }

            @Override
            @Nonnull
            public Map<Long, RbacRole> getUidsRoles(Collection<Long> uids) {
                return uids.stream().collect(toMap(identity(), uidToRbacRoleMap::get));
            }
        });
    }

    private User generateNewUserWithAppropriateFields() {
        return generateNewUser()
                .withAllowedIps("")
                .withStatusBlocked(false)
                .withBlockReasonType(UsersBlockReasonType.NOT_SET)
                .withStatusEasy(false)
                .withStatusArch(false)
                .withCreateTime(LocalDateTime.now().withNano(0))
                .withOpts("")
                .withGeoId(0L)
                .withAutobanned(false)
                .withDeveloper(false)
                .withCaptchaFreq(0L)
                .withPerms(EnumSet.noneOf(ClientPerm.class))
                .withSendClientSms(true)
                .withSendClientLetters(true)
                .withUseCampDescription(false)
                .withIsOfferAccepted(false)

                // это репозиторий сейчас из базы не извлекает
                .withRepType(null);
    }

    private void saveUser(DSLContext dslContext, User user) {
        dslContext.update(USERS)
                .set(new UsersRecord()
                        .with(USERS.UID, user.getUid())
                        .with(USERS.LOGIN, user.getLogin())
                        .with(USERS.EMAIL, user.getEmail())
                        .with(USERS.PHONE, user.getPhone())
                        .with(USERS.FIO, user.getFio())
                        .with(USERS.SEND_NEWS, user.getSendNews() ? UsersSendnews.Yes : UsersSendnews.No)
                        .with(USERS.SEND_ACC_NEWS, user.getSendAccNews() ? UsersSendaccnews.Yes : UsersSendaccnews.No)
                        .with(USERS.SEND_WARN, user.getSendWarn() ? UsersSendwarn.Yes : UsersSendwarn.No)
                        .with(USERS.LANG, user.getLang().getLangString())
                        .with(USERS.ALLOWED_IPS, user.getAllowedIps())
                        .with(USERS.STATUS_BLOCKED,
                                user.getStatusBlocked() ? UsersStatusblocked.Yes : UsersStatusblocked.No)
                        .with(USERS.BLOCK_REASON_TYPE, UsersBlockReasonType.toSource(user.getBlockReasonType()))
                        .with(USERS.STATUS_ARCH, user.getStatusArch() ? UsersStatusarch.Yes : UsersStatusarch.No)
                        .with(USERS.CREATETIME, Objects.requireNonNull(user.getCreateTime())
                                .toInstant(OffsetDateTime.now().getOffset()).toEpochMilli()))
                .where(USERS.UID.eq(user.getUid()))
                .execute();

        UsersOptionsRecord usersOptionsRecord = new UsersOptionsRecord()
                .with(USERS_OPTIONS.UID, user.getUid())
                .with(USERS_OPTIONS.STATUS_EASY,
                        user.getStatusEasy() ? UsersOptionsStatuseasy.Yes : UsersOptionsStatuseasy.No)
                .with(USERS_OPTIONS.OPTS, user.getOpts())
                .with(USERS_OPTIONS.YA_COUNTERS, 0L)
                .with(USERS_OPTIONS.GEO_ID, 0L);

        dslContext.insertInto(USERS_OPTIONS)
                .set(usersOptionsRecord)
                .onDuplicateKeyUpdate()
                .set(usersOptionsRecord)
                .execute();

        ClientsAutobanRecord clientsAutobanRecord = new ClientsAutobanRecord()
                .with(CLIENTS_AUTOBAN.CLIENT_ID, user.getClientId().asLong())
                .with(CLIENTS_AUTOBAN.IS_AUTOBANNED, user.getAutobanned() ?
                        ClientsAutobanIsAutobanned.Yes : ClientsAutobanIsAutobanned.No);

        dslContext.insertInto(CLIENTS_AUTOBAN)
                .set(clientsAutobanRecord)
                .onDuplicateKeyUpdate()
                .set(clientsAutobanRecord)
                .execute();

        InternalUsersRecord internalUsersRecord = new InternalUsersRecord()
                .with(INTERNAL_USERS.UID, user.getUid())
                .with(INTERNAL_USERS.IS_DEVELOPER, user.getDeveloper() ?
                        InternalUsersIsDeveloper.Yes : InternalUsersIsDeveloper.No);

        dslContext.insertInto(INTERNAL_USERS)
                .set(internalUsersRecord)
                .onDuplicateKeyUpdate()
                .set(internalUsersRecord)
                .execute();
    }

    @Test
    public void fetchByUids() {
        List<Long> uids = asList(userInfo1.getUid(), userInfo2.getUid());
        List<User> actualUsers = userRepository.fetchByUids(shard, uids);
        assertThat(actualUsers).contains(user1, user2);
    }

    @Test
    public void fetchUkrainianLanguage() {
        Long uid = userInfo1.getUid();
        dslContextProvider.ppc(shard)
                .update(USERS)
                .set(USERS.LANG, INT_UKRAINIAN_LANG_SUBTAG)
                .where(USERS.UID.eq(uid))
                .execute();

        Language actualLang = userRepository.fetchByUids(shard, singleton(uid))
                .get(0)
                .getLang();
        assertThat(actualLang).isEqualTo(Language.UK);
    }

}

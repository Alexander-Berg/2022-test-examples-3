package ru.yandex.market.global.partner.domain.business;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.db.jooq.enums.EPermission;
import ru.yandex.market.global.db.jooq.enums.EPermissionTargetType;
import ru.yandex.market.global.db.jooq.tables.pojos.Business;
import ru.yandex.market.global.db.jooq.tables.pojos.BusinessToken;
import ru.yandex.market.global.db.jooq.tables.pojos.Contacts;
import ru.yandex.market.global.partner.BaseFunctionalTest;
import ru.yandex.market.global.partner.domain.clients.BlackboxService;
import ru.yandex.market.global.partner.domain.clients.BlackboxUserData;
import ru.yandex.market.global.partner.domain.contacts.ContactsCommandService;
import ru.yandex.market.global.partner.domain.contacts.ContactsQueryService;
import ru.yandex.market.global.partner.domain.permission.PermissionService;
import ru.yandex.market.global.partner.domain.permission.model.PermissionTarget;
import ru.yandex.market.global.partner.mapper.EntityMapper;
import ru.yandex.market.global.partner.util.RandomDataGenerator;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BusinessAccessServiceTest extends BaseFunctionalTest {

    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(BusinessAccessServiceTest.class)
            .build();

    private static final String TOKEN = "TOKEN1234567890TOKEN1234567890TOKEN1234567890TOKEN1234567890TOKEN1234567890";
    private static final String TVM_USER_TICKET = "3:user:COWpARCIlLOUBhpICgYI45OTmw8Q45OTmw8aEm9hdXR";

    private final BusinessAccessService businessAccessService;
    private final BusinessCommandServiceIndexingImpl businessCommandService;
    private final Clock clock;
    private final BusinessCreateService businessCreateService;
    private final BlackboxService blackboxService;
    private final PermissionService permissionService;
    private final ContactsQueryService contactsQueryService;
    private final ContactsCommandService contactsCommandService;

    @Test
    public void testJoinUserSuccess() {
        long uid = 11111000000555343L;
        Business business = RANDOM.nextObject(Business.class);
        businessCreateService.create(business);

        businessCommandService.createToken(new BusinessToken()
                .setBusinessId(business.getId())
                .setCreatedAt(OffsetDateTime.now(clock))
                .setExpiresAt(OffsetDateTime.now(clock).plusHours(1))
                .setToken(TOKEN));

        BlackboxUserData userData = new BlackboxUserData(uid, "Alexey", "Alexeev", "+71234567890");
        Mockito.when(blackboxService.loadUserData(uid, TVM_USER_TICKET))
                .thenReturn(userData);

        businessAccessService.joinUser(TOKEN, uid, TVM_USER_TICKET);

        assertThat(permissionService.getUserPermissions(uid)).containsExactlyInAnyOrderEntriesOf(Map.of(
                new PermissionTarget()
                        .setTargetId(business.getId())
                        .setTargetType(EPermissionTargetType.BUSINESS),
                EPermission.ADMINISTRATE
        ));

        assertThat(contactsQueryService.fetchByUID(uid)).usingRecursiveComparison().ignoringExpectedNullFields()
                .isEqualTo(new Contacts(null, uid, userData.getPhoneNumber(), userData.getFirstName(),
                        userData.getLastName(), null, null, null, true));
    }

    @Test
    public void testJoinDuplicatedUserSuccess() {
        long uid = 11111000000555344L;
        Business business = RANDOM.nextObject(Business.class);
        businessCreateService.create(business);

        businessCommandService.createToken(new BusinessToken()
                .setBusinessId(business.getId())
                .setCreatedAt(OffsetDateTime.now(clock))
                .setExpiresAt(OffsetDateTime.now(clock).plusHours(1))
                .setToken(TOKEN));

        BlackboxUserData userData = new BlackboxUserData(uid, "Alexey", "Alexeev", "+71234567890");
        Contacts contact = EntityMapper.MAPPER.toContact(userData);
        contactsCommandService.createContact(contact);

        assertThat(contactsQueryService.fetchByUID(uid)).usingRecursiveComparison().ignoringExpectedNullFields()
                .isEqualTo(contact.setModifiedAt(null).setCreatedAt(null));

        Mockito.when(blackboxService.loadUserData(uid, TVM_USER_TICKET))
                .thenReturn(userData);

        businessAccessService.joinUser(TOKEN, uid, TVM_USER_TICKET);

        assertThat(permissionService.getUserPermissions(uid)).containsExactlyInAnyOrderEntriesOf(Map.of(
                new PermissionTarget()
                        .setTargetId(business.getId())
                        .setTargetType(EPermissionTargetType.BUSINESS),
                EPermission.ADMINISTRATE
        ));
    }

    @Test
    public void testJoinUserWithoutCredentialsSuccess() {
        long uid = 11111000000555345L;
        Business business = RANDOM.nextObject(Business.class);
        businessCreateService.create(business);

        businessCommandService.createToken(new BusinessToken()
                .setBusinessId(business.getId())
                .setCreatedAt(OffsetDateTime.now(clock))
                .setExpiresAt(OffsetDateTime.now(clock).plusHours(1))
                .setToken(TOKEN));

        BlackboxUserData userData = new BlackboxUserData(uid, null, null, null);

        Mockito.when(blackboxService.loadUserData(uid, TVM_USER_TICKET))
                .thenReturn(userData);

        businessAccessService.joinUser(TOKEN, uid, TVM_USER_TICKET);

        Contacts contacts = contactsQueryService.fetchByUID(uid);
        assertThat(contacts.getFirstName()).isNull();
        assertThat(contacts.getLastName()).isNull();
        assertThat(contacts.getPhoneNumber()).isNull();
    }

    @Test
    public void testJoinWithBlackboxExceptionSuccess() {
        long uid = 11111000000555346L;
        Business business = RANDOM.nextObject(Business.class);
        businessCreateService.create(business);

        businessCommandService.createToken(new BusinessToken()
                .setBusinessId(business.getId())
                .setCreatedAt(OffsetDateTime.now(clock))
                .setExpiresAt(OffsetDateTime.now(clock).plusHours(1))
                .setToken(TOKEN));

        Mockito.when(blackboxService.loadUserData(uid, TVM_USER_TICKET)).thenReturn(null);

        businessAccessService.joinUser(TOKEN, uid, TVM_USER_TICKET);

        assertThat(contactsQueryService.fetchByUID(uid)).isNull();
    }

}

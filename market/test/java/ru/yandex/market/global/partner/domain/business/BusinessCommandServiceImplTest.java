package ru.yandex.market.global.partner.domain.business;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.db.jooq.enums.EPermission;
import ru.yandex.market.global.db.jooq.enums.EPermissionTargetType;
import ru.yandex.market.global.db.jooq.tables.pojos.Business;
import ru.yandex.market.global.db.jooq.tables.pojos.BusinessToken;
import ru.yandex.market.global.db.jooq.tables.pojos.Permission;
import ru.yandex.market.global.partner.BaseFunctionalTest;
import ru.yandex.market.global.partner.domain.permission.PermissionService;
import ru.yandex.market.global.partner.domain.permission.model.PermissionTarget;
import ru.yandex.market.global.partner.util.RandomDataGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.global.db.jooq.enums.EPermission.ADMINISTRATE;
import static ru.yandex.market.global.db.jooq.enums.EPermissionTargetType.BUSINESS;

@Slf4j
public class BusinessCommandServiceImplTest extends BaseFunctionalTest {

    private static final EnhancedRandom RANDOM =
            RandomDataGenerator.dataRandom(BusinessCommandServiceImplTest.class).build();

    private static final String TOKEN = "TOKEN";
    private static final long UID = 123000321;

    @Autowired
    private BusinessCommandServiceIndexingImpl businessCommandService;

    @Autowired
    private BusinessQueryService businessQueryService;

    @Autowired
    private BusinessTokenRepository businessTokenRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private Clock clock;

    @Autowired
    private BusinessCreateService businessCreateService;

    @Test
    public void testCreateBusiness() {
        Business toCreate = RANDOM.nextObject(Business.class);
        businessCreateService.create(toCreate);

        assertThat(businessQueryService.get(toCreate.getId()))
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withComparatorForType(Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class)
                        .build()
                )
                .isEqualTo(toCreate);
    }

    @Test
    public void testTokenCrated() {
        Business business = RANDOM.nextObject(Business.class);
        businessCreateService.create(business);
        List<BusinessToken> businessTokens = businessTokenRepository.fetchByBusinessId(business.getId());

        Assertions.assertThat(businessTokens).hasSize(1);
        assertThat(businessTokens.get(0).getExpiresAt()).isAfter(OffsetDateTime.now(clock));
    }

    @Test
    public void testJoinUserSuccess() {
        Business business = RANDOM.nextObject(Business.class);
        businessCreateService.create(business);

        businessCommandService.createToken(new BusinessToken()
                .setBusinessId(business.getId())
                .setCreatedAt(OffsetDateTime.now(clock))
                .setExpiresAt(OffsetDateTime.now(clock).plusHours(1))
                .setToken(TOKEN));

        businessCommandService.joinUser(TOKEN, UID);

        assertThat(permissionService.getUserPermissions(UID)).containsExactlyInAnyOrderEntriesOf(Map.of(
                new PermissionTarget()
                        .setTargetId(business.getId())
                        .setTargetType(EPermissionTargetType.BUSINESS),
                EPermission.ADMINISTRATE
        ));
    }

    @Test
    public void testJoinUserNonExistingToken() {
        Business business = RANDOM.nextObject(Business.class);
        businessCreateService.create(business);

        assertThatThrownBy(() -> businessCommandService.joinUser(TOKEN, UID))
                .isExactlyInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void testJoinUserExpiredToken() {
        Business business = RANDOM.nextObject(Business.class);
        businessCreateService.create(business);

        businessCommandService.createToken(new BusinessToken()
                .setBusinessId(business.getId())
                .setCreatedAt(OffsetDateTime.now(clock).minusHours(2))
                .setExpiresAt(OffsetDateTime.now(clock).minusHours(1))
                .setToken(TOKEN));

        assertThatThrownBy(() -> businessCommandService.joinUser(TOKEN, UID))
                .isExactlyInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void testJoinUserWithExistingBusiness() {
        Business business = RANDOM.nextObject(Business.class);
        businessCreateService.create(business);

        businessCommandService.createToken(new BusinessToken()
                .setBusinessId(business.getId())
                .setCreatedAt(OffsetDateTime.now(clock))
                .setExpiresAt(OffsetDateTime.now(clock).plusHours(1))
                .setToken(TOKEN));

        long secondBusinessId = business.getId() + 1L;

        permissionService.createPermission(new Permission()
                .setUid(UID)
                .setPermission(ADMINISTRATE)
                .setTargetType(BUSINESS)
                .setTargetId(secondBusinessId));

        businessCommandService.joinUser(TOKEN, UID);

        assertThat(permissionService.getUserPermissions(UID)).containsExactlyInAnyOrderEntriesOf(Map.of(
                new PermissionTarget()
                        .setTargetId(business.getId())
                        .setTargetType(EPermissionTargetType.BUSINESS),
                EPermission.ADMINISTRATE,
                new PermissionTarget()
                        .setTargetId(secondBusinessId)
                        .setTargetType(EPermissionTargetType.BUSINESS),
                EPermission.ADMINISTRATE
        ));
    }

    @Test
    public void testJoinUserWithSameBusiness() {
        Business business = RANDOM.nextObject(Business.class);
        businessCreateService.create(business);

        businessCommandService.createToken(new BusinessToken()
                .setBusinessId(business.getId())
                .setCreatedAt(OffsetDateTime.now(clock))
                .setExpiresAt(OffsetDateTime.now(clock).plusHours(1))
                .setToken(TOKEN));

        businessCommandService.joinUser(TOKEN, UID);
        businessCommandService.joinUser(TOKEN, UID);

        assertThat(permissionService.getUserPermissions(UID)).containsExactlyInAnyOrderEntriesOf(Map.of(
                new PermissionTarget()
                        .setTargetId(business.getId())
                        .setTargetType(EPermissionTargetType.BUSINESS),
                EPermission.ADMINISTRATE
        ));
    }

}

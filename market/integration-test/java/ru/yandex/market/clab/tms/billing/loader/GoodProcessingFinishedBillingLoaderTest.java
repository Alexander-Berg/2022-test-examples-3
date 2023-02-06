package ru.yandex.market.clab.tms.billing.loader;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.clab.common.service.audit.AuditRepository;
import ru.yandex.market.clab.common.service.audit.wrapper.GoodWrapper;
import ru.yandex.market.clab.common.service.good.GoodRepository;
import ru.yandex.market.clab.common.service.photo.EditedPhotoRepository;
import ru.yandex.market.clab.common.service.photo.RawPhotoRepository;
import ru.yandex.market.clab.common.service.user.UserRepository;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.enums.ActionType;
import ru.yandex.market.clab.db.jooq.generated.enums.EntityType;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.PaidAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.AuditAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingAction;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.BillingTarif;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Category;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.ClabUser;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.EditedPhoto;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RawPhoto;
import ru.yandex.market.clab.tms.BaseTmsIntegrationTest;
import ru.yandex.market.clab.tms.billing.BillingTarifProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author anmalysh
 * @since 2/28/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class GoodProcessingFinishedBillingLoaderTest extends BaseTmsIntegrationTest {

    private LocalDateTime periodStart = LocalDate.now().atStartOfDay();
    private LocalDateTime periodEnd = periodStart.plusDays(1);
    private Good good;

    @Autowired
    private GoodProcessingFinishedBillingLoader goodProcessingFinishedBillingLoader;

    @Autowired
    private GoodRepository goodRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private RawPhotoRepository rawPhotoRepository;

    @Autowired
    private EditedPhotoRepository editedPhotoRepository;

    @Autowired
    private UserRepository userRepository;

    @Before
    public void setUp() {
        ClabUser clabUser = userRepository.save(new ClabUser()
            .setLogin("user4")
            .setRoles("ADMIN")
        );

        good = goodRepository.save(RandomTestUtils.randomObject(Good.class, "id")
            .setCategoryId(2L)
            .setState(GoodState.NEW)
            .setCartId(null)
            .setPhotoEditorId(clabUser.getId()));
    }

    @Test
    public void testCountAccept() {
        auditRepository.writeActions(Arrays.asList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.VERIFYING, GoodState.VERIFIED)
                .setStaffLogin("user2"),
            createAuditAction(periodStart.minusDays(2), GoodWrapper.STATE, GoodState.NEW, GoodState.ACCEPTED)
                .setStaffLogin("user100500")
        ));
        List<BillingAction> actions =
            goodProcessingFinishedBillingLoader.loadActions(periodStart, periodEnd, createTarifProviderWithTarifs());

        assertThat(actions).singleElement().satisfies(a -> {
            assertThat(a).extracting(BillingAction::getPaidAction).isEqualTo(PaidAction.GOOD_ACCEPT);
            assertThat(a).extracting(BillingAction::getStaffLogin).isEqualTo("user100500");
            assertThat(a).extracting(BillingAction::getPriceKopeck).isEqualTo(10);
            assertThat(a).extracting(BillingAction::getCategoryId).isEqualTo(2L);
            assertThat(a).extracting(BillingAction::getBillingDate).isEqualTo(periodStart.plusMinutes(1));
            assertThat(a).extracting(BillingAction::getAuditActionId).isNotNull();
        });
    }

    @Test
    public void testAddToCart() {
        auditRepository.writeActions(Arrays.asList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.VERIFYING, GoodState.VERIFIED)
                .setStaffLogin("user2"),
            createAuditAction(periodStart.minusDays(3), GoodWrapper.STATE, GoodState.ACCEPTED, GoodState.SORTED_TO_CART)
                .setStaffLogin("user3"),
            createAuditAction(periodStart.minusDays(2), GoodWrapper.STATE, GoodState.ACCEPTED, GoodState.SORTED_TO_CART)
                .setStaffLogin("user100500")
        ));
        List<BillingAction> actions =
            goodProcessingFinishedBillingLoader.loadActions(periodStart, periodEnd, createTarifProviderWithTarifs());

        assertThat(actions).singleElement().satisfies(a -> {
            assertThat(a).extracting(BillingAction::getPaidAction).isEqualTo(PaidAction.GOOD_ADD_TO_CART);
            assertThat(a).extracting(BillingAction::getStaffLogin).isEqualTo("user100500");
            assertThat(a).extracting(BillingAction::getPriceKopeck).isEqualTo(11);
            assertThat(a).extracting(BillingAction::getCategoryId).isEqualTo(2L);
            assertThat(a).extracting(BillingAction::getBillingDate).isEqualTo(periodStart.plusMinutes(1));
            assertThat(a).extracting(BillingAction::getAuditActionId).isNotNull();
        });
    }

    @Test
    public void testAssistPhoto() {
        auditRepository.writeActions(Arrays.asList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.VERIFYING, GoodState.VERIFIED)
                .setStaffLogin("user2"),
            createAuditAction(periodStart.minusDays(3), GoodWrapper.STATE, GoodState.SORTED_TO_CART, GoodState.PHOTO)
                .setStaffLogin("user3"),
            createAuditAction(periodStart.minusDays(2), GoodWrapper.STATE, GoodState.SORTED_TO_CART, GoodState.PHOTO)
                .setStaffLogin("user100500"),
            createAuditAction(periodStart.minusDays(1), GoodWrapper.STATE, GoodState.VERIFIED, GoodState.PHOTOGRAPHED)
                .setStaffLogin("user4")
        ));
        List<BillingAction> actions =
            goodProcessingFinishedBillingLoader.loadActions(periodStart, periodEnd, createTarifProviderWithTarifs());

        assertThat(actions).singleElement().satisfies(a -> {
            assertThat(a).extracting(BillingAction::getPaidAction).isEqualTo(PaidAction.GOOD_ASSIST_PHOTO);
            assertThat(a).extracting(BillingAction::getStaffLogin).isEqualTo("user100500");
            assertThat(a).extracting(BillingAction::getPriceKopeck).isEqualTo(12);
            assertThat(a).extracting(BillingAction::getCategoryId).isEqualTo(2L);
            assertThat(a).extracting(BillingAction::getBillingDate).isEqualTo(periodStart.plusMinutes(1));
            assertThat(a).extracting(BillingAction::getAuditActionId).isNotNull();
        });
    }

    @Test
    public void testRemoveFromCart() {
        auditRepository.writeActions(Arrays.asList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.VERIFYING, GoodState.VERIFIED)
                .setStaffLogin("user2"),
            createAuditAction(periodStart.minusDays(3), GoodWrapper.STATE, GoodState.VERIFIED,
                GoodState.PREPARED_TO_OUT)
                .setStaffLogin("user3"),
            createAuditAction(periodStart.minusDays(2), GoodWrapper.STATE, GoodState.VERIFIED,
                GoodState.PREPARED_TO_OUT)
                .setStaffLogin("user100500")
        ));
        List<BillingAction> actions =
            goodProcessingFinishedBillingLoader.loadActions(periodStart, periodEnd, createTarifProviderWithTarifs());

        assertThat(actions).singleElement().satisfies(a -> {
            assertThat(a).extracting(BillingAction::getPaidAction).isEqualTo(PaidAction.GOOD_REMOVE_FROM_CART);
            assertThat(a).extracting(BillingAction::getStaffLogin).isEqualTo("user100500");
            assertThat(a).extracting(BillingAction::getPriceKopeck).isEqualTo(13);
            assertThat(a).extracting(BillingAction::getCategoryId).isEqualTo(2L);
            assertThat(a).extracting(BillingAction::getBillingDate).isEqualTo(periodStart.plusMinutes(1));
            assertThat(a).extracting(BillingAction::getAuditActionId).isNotNull();
        });
    }

    @Test
    public void testMakePhoto() {
        auditRepository.writeActions(Arrays.asList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.VERIFYING, GoodState.VERIFIED)
                .setStaffLogin("user2"),
            createAuditAction(periodStart.minusDays(3), GoodWrapper.STATE, GoodState.PHOTO, GoodState.PHOTOGRAPHED)
                .setStaffLogin("user3"),
            createAuditAction(periodStart.minusDays(2), GoodWrapper.STATE, GoodState.PHOTO, GoodState.PHOTOGRAPHED)
                .setStaffLogin("user100500"),
            createAuditAction(periodStart.minusDays(1), GoodWrapper.STATE, GoodState.VERIFIED, GoodState.PHOTOGRAPHED)
                .setStaffLogin("user4")
        ));

        RawPhoto photo1 = RandomTestUtils.randomObject(RawPhoto.class, "id")
            .setGoodId(good.getId());
        RawPhoto photo2 = RandomTestUtils.randomObject(RawPhoto.class, "id")
            .setGoodId(good.getId());
        rawPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2));

        List<BillingAction> actions =
            goodProcessingFinishedBillingLoader.loadActions(periodStart, periodEnd, createTarifProviderWithTarifs())
                .stream()
                .filter(a -> a.getPaidAction() != PaidAction.GOOD_ASSIST_PHOTO)
                .collect(Collectors.toList());

        assertThat(actions)
            .hasSize(2)
            .allSatisfy(a -> {
                assertThat(a).extracting(BillingAction::getPaidAction).isEqualTo(PaidAction.GOOD_MAKE_PICTURE);
                assertThat(a).extracting(BillingAction::getStaffLogin).isEqualTo("user100500");
                assertThat(a).extracting(BillingAction::getPriceKopeck).isEqualTo(14);
                assertThat(a).extracting(BillingAction::getCategoryId).isEqualTo(2L);
                assertThat(a).extracting(BillingAction::getBillingDate).isEqualTo(periodStart.plusMinutes(1));
                assertThat(a).extracting(BillingAction::getAuditActionId).isNotNull();
            });
    }

    @Test
    public void testEditPhoto() {
        auditRepository.writeActions(Arrays.asList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.VERIFYING, GoodState.VERIFIED)
                .setStaffLogin("user2"),
            createAuditAction(periodStart.minusDays(3), GoodWrapper.STATE, GoodState.PHOTO_EDITING,
                GoodState.PHOTO_EDITED)
                .setStaffLogin("user3"),
            createAuditAction(periodStart.minusDays(2), GoodWrapper.STATE, GoodState.PHOTO_EDITING,
                GoodState.PHOTO_EDITED)
                .setStaffLogin("user100500"),
            createAuditAction(periodStart.minusDays(1), GoodWrapper.STATE, GoodState.VERIFIED, GoodState.PHOTO_EDITING)
                .setStaffLogin("user4")
        ));

        EditedPhoto photo1 = RandomTestUtils.randomObject(EditedPhoto.class, "id")
            .setGoodId(good.getId());
        EditedPhoto photo2 = RandomTestUtils.randomObject(EditedPhoto.class, "id")
            .setGoodId(good.getId());
        editedPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2));

        List<BillingAction> actions =
            goodProcessingFinishedBillingLoader.loadActions(periodStart, periodEnd, createTarifProviderWithTarifs());

        assertThat(actions)
            .hasSize(2)
            .allSatisfy(a -> {
                assertThat(a).extracting(BillingAction::getPaidAction).isEqualTo(PaidAction.GOOD_EDIT_PICTURE);
                assertThat(a).extracting(BillingAction::getStaffLogin).isEqualTo("user4");
                assertThat(a).extracting(BillingAction::getPriceKopeck).isEqualTo(15);
                assertThat(a).extracting(BillingAction::getCategoryId).isEqualTo(2L);
                assertThat(a).extracting(BillingAction::getBillingDate).isEqualTo(periodStart.plusMinutes(1));
                assertThat(a).extracting(BillingAction::getAuditActionId).isNotNull();
            });
    }

    @Test
    public void testCountProcessingFinishedNoActions() {
        auditRepository.writeActions(Collections.singletonList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.VERIFYING, GoodState.VERIFIED)
        ));
        List<BillingAction> actions =
            goodProcessingFinishedBillingLoader.loadActions(periodStart, periodEnd, createTarifProviderWithTarifs());

        assertThat(actions).isEmpty();
    }

    @Test
    public void testFailedIfGoodNotFound() {
        auditRepository.writeActions(Collections.singletonList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.VERIFYING, GoodState.VERIFIED)
                .setEntityInternalId(good.getId() + 1)
        ));
        assertThatThrownBy(() ->
            goodProcessingFinishedBillingLoader.loadActions(periodStart, periodEnd, createTarifProviderWithTarifs()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testCountNoTarif() {
        auditRepository.writeActions(Arrays.asList(
            createAuditAction(periodStart.plusMinutes(1), GoodWrapper.STATE, GoodState.VERIFYING, GoodState.VERIFIED),
            createAuditAction(periodStart.minusDays(2), GoodWrapper.STATE, GoodState.NEW, GoodState.ACCEPTED)
                .setStaffLogin("user100500")
        ));
        List<BillingAction> actions =
            goodProcessingFinishedBillingLoader.loadActions(periodStart, periodEnd, createTarifProvider(Collections.emptyList()));

        assertThat(actions).singleElement().satisfies(a -> {
            assertThat(a).extracting(BillingAction::getPaidAction).isEqualTo(PaidAction.GOOD_ACCEPT);
            assertThat(a).extracting(BillingAction::getStaffLogin).isEqualTo("user100500");
            assertThat(a).extracting(BillingAction::getPriceKopeck).isEqualTo(0);
            assertThat(a).extracting(BillingAction::getCategoryId).isEqualTo(2L);
            assertThat(a).extracting(BillingAction::getBillingDate).isEqualTo(periodStart.plusMinutes(1));
            assertThat(a).extracting(BillingAction::getAuditActionId).isNotNull();
        });
    }

    private BillingTarifProvider createTarifProviderWithTarifs() {
        List<BillingTarif> tarifs = Arrays.asList(
            new BillingTarif()
                .setCategoryId(2L)
                .setPaidAction(PaidAction.GOOD_ACCEPT)
                .setStartDate(periodStart.minusSeconds(1))
                .setPriceKopeck(10),
            new BillingTarif()
                .setCategoryId(2L)
                .setPaidAction(PaidAction.GOOD_ADD_TO_CART)
                .setStartDate(periodStart.minusSeconds(1))
                .setPriceKopeck(11),
            new BillingTarif()
                .setCategoryId(2L)
                .setPaidAction(PaidAction.GOOD_ASSIST_PHOTO)
                .setStartDate(periodStart.minusSeconds(1))
                .setPriceKopeck(12),
            new BillingTarif()
                .setCategoryId(2L)
                .setPaidAction(PaidAction.GOOD_REMOVE_FROM_CART)
                .setStartDate(periodStart.minusSeconds(1))
                .setPriceKopeck(13),
            new BillingTarif()
                .setCategoryId(2L)
                .setPaidAction(PaidAction.GOOD_MAKE_PICTURE)
                .setStartDate(periodStart.minusSeconds(1))
                .setPriceKopeck(14),
            new BillingTarif()
                .setCategoryId(2L)
                .setPaidAction(PaidAction.GOOD_EDIT_PICTURE)
                .setStartDate(periodStart.minusSeconds(1))
                .setPriceKopeck(15));
        return createTarifProvider(tarifs);
    }

    private BillingTarifProvider createTarifProvider(List<BillingTarif> tarifs) {
        List<Category> categories = Collections.singletonList(new Category().setId(2L));
        return new BillingTarifProvider(periodStart, categories, tarifs);
    }

    private AuditAction createAuditAction(LocalDateTime date,
                                          String propertyName,
                                          Object oldValue,
                                          Object newValue) {
        return RandomTestUtils.randomObject(AuditAction.class, "id")
            .setEntityInternalId(good.getId())
            .setActionType(ActionType.UPDATE)
            .setEntityType(EntityType.GOOD)
            .setActionDate(date)
            .setPropertyName(propertyName)
            .setStaffLogin("user1")
            .setOldValue(String.valueOf(oldValue))
            .setNewValue(String.valueOf(newValue));
    }
}

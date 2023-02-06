package ru.yandex.market.mboc.common.offers.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.http.MboAudit;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.ContentComment;
import ru.yandex.market.mboc.common.offers.model.ContentCommentType;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.SecurityContextAuthenticationHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.simpleSupplier;

/**
 * @author yuramalinov
 * @created 08.07.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class OfferRepositoryAuditTest extends BaseDbTestClass {
    private static final Joiner MAPPING_JOINER = Joiner.on(":");

    @Autowired
    private OfferRepositoryImpl offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private MboAuditServiceMock auditServiceMock;

    @Before
    public void setup() {
        supplierRepository.insert(simpleSupplier());
        SecurityContextAuthenticationHelper.setAuthenticationToken();
        auditServiceMock.clearActions();
    }

    @After
    public void cleanup() {
        SecurityContextAuthenticationHelper.clearAuthenticationToken();
    }

    @Test
    public void testInsertion() {
        Offer offer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);
        offerRepository.insertOffer(offer);
        MboAudit.FindActionsRequest findRequest = createFindRequest(offer);
        List<MboAudit.MboAction> actions = auditServiceMock.findActions(findRequest)
            .getActionsList();
        assertThat(actions)
            .extracting(MboAudit.MboAction::getEntityId)
            .containsOnly(offer.getId());

        Optional<MboAudit.MboAction> titleChangeAction = actions.stream()
            .filter(action -> action.getPropertyName().equals("title"))
            .findAny();

        assertThat(titleChangeAction).isPresent();

        assertThat(titleChangeAction.get().getOldValue()).isNullOrEmpty();
        assertThat(titleChangeAction.get().getNewValue()).isEqualTo("Напильники для PGaaS");
        assertThat(titleChangeAction.get().getStaffLogin()).isEqualTo("test-user");
    }

    @Test
    public void testNoChangesOnEmptyUpdate() {
        Offer offer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);
        offerRepository.insertOffer(offer);

        offer = offerRepository.getOfferById(offer.getId());

        auditServiceMock.clearActions();

        offerRepository.updateOffers(Collections.singletonList(offer));

        MboAudit.FindActionsRequest findRequest = createFindRequest(offer);
        List<MboAudit.MboAction> actions = auditServiceMock.findActions(findRequest).getActionsList();
        assertThat(actions).isEmpty();
    }

    @Test
    public void testNoChangesOnContentCommentUpdate() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setContentComments(new ContentComment(ContentCommentType.INCORRECT_INFORMATION, "something"));
        offerRepository.insertOffer(offer);

        offer = offerRepository.getOfferById(offer.getId());

        auditServiceMock.clearActions();

        offerRepository.updateOffers(Collections.singletonList(offer));

        MboAudit.FindActionsRequest findRequest = createFindRequest(offer);
        List<MboAudit.MboAction> actions = auditServiceMock.findActions(findRequest).getActionsList();
        assertThat(actions).isEmpty();
    }

    @Test
    public void testChangesOnContentCommentUpdateIfChanged() {
        Offer offer = OfferTestUtils.simpleOffer()
            .setContentComments(new ContentComment(ContentCommentType.INCORRECT_INFORMATION, "something"));
        offerRepository.insertOffer(offer);

        offer = offerRepository.getOfferById(offer.getId());
        offer.setContentComments(new ContentComment(ContentCommentType.INCORRECT_INFORMATION, "Changed!"));

        auditServiceMock.clearActions();

        offerRepository.updateOffers(Collections.singletonList(offer));

        MboAudit.FindActionsRequest findRequest = createFindRequest(offer);
        List<MboAudit.MboAction> actions = auditServiceMock.findActions(findRequest).getActionsList();
        assertThat(actions).hasSize(1);

        MboAudit.MboAction action = actions.get(0);
        assertThat(action.getPropertyName()).isEqualTo("content_comment_details");
        assertThat(action.getOldValue())
            .isEqualTo("[{\"type\": \"INCORRECT_INFORMATION\", \"items\": [\"something\"]}]");
        assertThat(action.getNewValue())
            .isEqualTo("[{\"type\": \"INCORRECT_INFORMATION\", \"items\": [\"Changed!\"]}]");
    }

    @Test
    public void testUpdate() {
        Offer offer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);
        offerRepository.insertOffer(offer);

        auditServiceMock.clearActions();

        offer = offerRepository.getOfferById(offer.getId());
        offer.setTitle("Changed!");
        offer.updateAcceptanceStatusForTests(Offer.AcceptanceStatus.TRASH);
        offer.setTransientModifiedBy("transient-offer-user");
        offer.setTransientChangeSource(MboAudit.Source.MBO);
        offer.setTransientChangeSourceId("something");

        offerRepository.updateOffers(Collections.singletonList(offer));

        MboAudit.FindActionsRequest findRequest = createFindRequest(offer);
        List<MboAudit.MboAction> actions = auditServiceMock.findActions(findRequest).getActionsList();
        assertThat(actions).isNotEmpty();

        Optional<MboAudit.MboAction> titleChangeAction = actions.stream()
            .filter(action -> action.getPropertyName().equals("title"))
            .findAny();
        Optional<MboAudit.MboAction> statusChangeAction = actions.stream()
            .filter(action -> action.getPropertyName().equals("acceptance_status"))
            .findAny();
        assertThat(titleChangeAction).isPresent();
        assertThat(statusChangeAction).isPresent();

        assertThat(titleChangeAction.get().getOldValue()).isEqualTo("Напильники для PGaaS");
        assertThat(titleChangeAction.get().getNewValue()).isEqualTo("Changed!");
        assertThat(statusChangeAction.get().getOldValue()).isEqualTo("NEW");
        assertThat(statusChangeAction.get().getNewValue()).isEqualTo("TRASH");

        assertSourceAndUser(titleChangeAction.get(), "transient-offer-user", MboAudit.Source.MBO, "something");
        assertSourceAndUser(statusChangeAction.get(), "transient-offer-user", MboAudit.Source.MBO, "something");
    }

    @Test
    public void testUpdateServiceOffer() {
        supplierRepository.insert(simpleSupplier().setId(12).setType(MbocSupplierType.THIRD_PARTY));
        supplierRepository.insert(simpleSupplier().setId(13).setType(MbocSupplierType.REAL_SUPPLIER).setRealSupplierId("000076"));

        Offer offer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);

        List<Offer.ServiceOffer> oldServiceOffers = List.of(
            new Offer.ServiceOffer(12, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.NEW),
            new Offer.ServiceOffer(13, MbocSupplierType.REAL_SUPPLIER, Offer.AcceptanceStatus.NEW));
        offer.setServiceOffers(oldServiceOffers);

        offerRepository.insertOffer(offer);
        auditServiceMock.clearActions();
        offer = offerRepository.getOfferById(offer.getId());

        List<Offer.ServiceOffer> newServiceOffers = List.of(
            new Offer.ServiceOffer(12, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK),
            new Offer.ServiceOffer(13, MbocSupplierType.REAL_SUPPLIER, Offer.AcceptanceStatus.OK));
        offer.setServiceOffers(newServiceOffers);
        offer.setTransientModifiedBy("transient-offer-user");
        offer.setTransientChangeSource(MboAudit.Source.MBO);
        offer.setTransientChangeSourceId("something");

        offerRepository.updateOffers(Collections.singletonList(offer));

        MboAudit.FindActionsRequest findRequest = createFindRequest(offer);
        List<MboAudit.MboAction> actions = auditServiceMock.findActions(findRequest).getActionsList();
        assertThat(actions).isNotEmpty();

        Optional<MboAudit.MboAction> changeAction = actions.stream()
            .filter(action -> action.getPropertyName().equals("service_offers"))
            .findAny();
        assertThat(changeAction).isPresent();

        String oldValue = oldServiceOffers.stream().map(Offer.ServiceOffer::toSqlString).collect(Collectors.joining("\n"));
        String newValue = newServiceOffers.stream().map(Offer.ServiceOffer::toSqlString).collect(Collectors.joining("\n"));
        assertThat(changeAction.get().getOldValue()).isEqualTo(oldValue);
        assertThat(changeAction.get().getNewValue()).isEqualTo(newValue);

        assertSourceAndUser(changeAction.get(), "transient-offer-user", MboAudit.Source.MBO, "something");
    }

    @Test
    public void testCorrectUpdateOfListProperty() {
        Offer offer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);
        offer.setIsOfferContentPresent(true);
        offerRepository.insertOffer(offer);

        auditServiceMock.clearActions();

        offer = offerRepository.getOfferById(offer.getId());
        offer.storeOfferContent(
            OfferContent.copyToBuilder(offer.extractOfferContent()).addUrl("http://java.sun.com").build());

        offerRepository.updateOffers(Collections.singletonList(offer));

        MboAudit.FindActionsRequest findRequest = createFindRequest(offer);
        List<MboAudit.MboAction> actions = auditServiceMock.findActions(findRequest).getActionsList();
        assertThat(actions).isNotEmpty();

        Optional<MboAudit.MboAction> urlsChangeAction = actions.stream()
            .filter(action -> action.getPropertyName().equals("urls"))
            .findAny();
        assertThat(urlsChangeAction).isPresent();
        assertThat(urlsChangeAction.get().getOldValue())
            .isEqualTo("http://yandex.ru\nhttps://yandex.com");
        assertThat(urlsChangeAction.get().getNewValue())
            .isEqualTo("http://yandex.ru\nhttps://yandex.com\nhttp://java.sun.com");
    }

    @Test
    public void testTicketsNotDuplicated() {
        Offer offer = OfferTestUtils.nextOffer()
            .addAdditionalTicket(Offer.AdditionalTicketType.RECLASSIFICATION, "OTHER-321")
            .addAdditionalTicket(Offer.AdditionalTicketType.ADD_SIZE_MEASURE, "SOME-123");
        offerRepository.insertOffer(offer);

        auditServiceMock.clearActions();

        offer = offerRepository.getOfferById(offer.getId());
        offerRepository.updateOffers(Collections.singletonList(offer));

        MboAudit.FindActionsRequest findRequest = createFindRequest(offer);
        List<MboAudit.MboAction> actions = auditServiceMock.findActions(findRequest).getActionsList();
        // Nothing should be recorded
        assertThat(actions).isEmpty();
    }

    @Test
    public void testCommentsNotDuplicated() {
        Offer offer = OfferTestUtils.nextOffer()
            .setContentComments(new ContentComment(ContentCommentType.CONFLICTING_INFORMATION, "item 1", "item 2"));
        offerRepository.insertOffer(offer);

        auditServiceMock.clearActions();

        offer = offerRepository.getOfferById(offer.getId());
        offerRepository.updateOffers(Collections.singletonList(offer));

        MboAudit.FindActionsRequest findRequest = createFindRequest(offer);
        List<MboAudit.MboAction> actions = auditServiceMock.findActions(findRequest).getActionsList();
        // Nothing should be recorded
        assertThat(actions).isEmpty();
    }

    @Test
    public void testMappingsAudit() {
        // test create mapping
        Offer offer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);
        offer.setTransientModifiedBy("transient-mapping-user");
        offerRepository.insertOffer(offer);

        long mappingId = offer.getApprovedSkuMapping().getMappingId();
        MboAudit.FindActionsRequest findRequest = createFindRequest(mappingId);
        List<MboAudit.MboAction> actions = auditServiceMock.findActions(findRequest).getActionsList();
        assertThat(actions).hasSize(1);
        Optional<MboAudit.MboAction> createMapping = actions.stream()
            .filter(action -> action.getActionType() == MboAudit.ActionType.CREATE)
            .findAny();
        assertThat(createMapping).isPresent();
        assertThat(createMapping.get().hasOldValue()).isFalse();
        String mappingChangeValue = MAPPING_JOINER.join(offer.getId(), offer.getBusinessId(), offer.getShopSku());
        assertThat(createMapping.get().getNewValue()).isEqualTo(mappingChangeValue);
        assertThat(createMapping.get().getStaffLogin()).isEqualTo("transient-mapping-user");

        assertSourceAndUser(createMapping.get(), "transient-mapping-user", MboAudit.Source.MBO, "");

        // test update mapping
        auditServiceMock.clearActions();
        int newMappingId = 500;
        offer = offerRepository.getOfferById(offer.getId());
        offer.updateApprovedSkuMapping(new Offer.Mapping(newMappingId, DateTimeUtils.dateTimeNow()),
            Offer.MappingConfidence.CONTENT);
        offer.setTransientModifiedBy("another-transient-user");
        offer.setTransientChangeSource(MboAudit.Source.YANG_TASK);
        offer.setTransientChangeSourceId("something1");
        offerRepository.updateOffers(Collections.singletonList(offer));

        findRequest = createFindRequest(newMappingId);
        actions = auditServiceMock.findActions(findRequest).getActionsList();
        assertThat(actions).hasSize(1);
        createMapping = actions.stream()
            .filter(action -> action.getActionType() == MboAudit.ActionType.CREATE)
            .findAny();
        assertThat(createMapping).isPresent();
        assertThat(createMapping.get().hasOldValue()).isFalse();
        assertThat(createMapping.get().getNewValue()).isEqualTo(mappingChangeValue);
        assertThat(createMapping.get().getStaffLogin()).isEqualTo("another-transient-user");

        assertSourceAndUser(createMapping.get(), "another-transient-user", MboAudit.Source.YANG_TASK, "something1");

        findRequest = createFindRequest(mappingId);
        actions = auditServiceMock.findActions(findRequest).getActionsList();
        assertThat(actions).hasSize(1);
        Optional<MboAudit.MboAction> deleteMapping = actions.stream()
            .filter(action -> action.getActionType() == MboAudit.ActionType.DELETE)
            .findAny();
        assertThat(deleteMapping).isPresent();
        assertThat(deleteMapping.get().hasNewValue()).isFalse();
        assertThat(deleteMapping.get().getOldValue()).isEqualTo(mappingChangeValue);
        assertThat(deleteMapping.get().getStaffLogin()).isEqualTo("another-transient-user");

        assertSourceAndUser(deleteMapping.get(), "another-transient-user", MboAudit.Source.YANG_TASK, "something1");
        // test delete mapping
        auditServiceMock.clearActions();
        int deletedMappingId = 0;
        offer = offerRepository.getOfferById(offer.getId());
        offer.updateApprovedSkuMapping(new Offer.Mapping(deletedMappingId, DateTimeUtils.dateTimeNow()),
            Offer.MappingConfidence.CONTENT);
        offer.setTransientModifiedBy("transient-mapping-user");
        offerRepository.updateOffers(Collections.singletonList(offer));

        findRequest = createFindRequest(newMappingId);
        actions = auditServiceMock.findActions(findRequest).getActionsList();
        assertThat(actions).hasSize(1);
        deleteMapping = actions.stream()
            .filter(action -> action.getActionType() == MboAudit.ActionType.DELETE)
            .findAny();
        assertThat(deleteMapping).isPresent();
        assertThat(deleteMapping.get().hasNewValue()).isFalse();
        assertThat(deleteMapping.get().getOldValue()).isEqualTo(mappingChangeValue);

        assertSourceAndUser(deleteMapping.get(), "transient-mapping-user", MboAudit.Source.MBO, "");
    }

    @Test
    public void testNoChangesOnStatusModifiedTimestamps() {
        Offer offer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);
        offerRepository.insertOffer(offer);

        offer = offerRepository.getOfferById(offer.getId())
            .setAcceptanceStatusModifiedInternal(DateTimeUtils.dateTimeNow())
            .setProcessingStatusModifiedInternal(DateTimeUtils.dateTimeNow());

        auditServiceMock.clearActions();

        offerRepository.updateOffers(Collections.singletonList(offer));

        MboAudit.FindActionsRequest findRequest = createFindRequest(offer);
        List<MboAudit.MboAction> actions = auditServiceMock.findActions(findRequest).getActionsList();
        assertThat(actions).isEmpty();
    }

    @Test
    public void testClassifierConfidenceFieldProcessedCorrectly() {
        Offer offer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);
        offerRepository.insertOffer(offer);
        MboAudit.FindActionsRequest findRequest = createFindRequest(offer);
        List<MboAudit.MboAction> actions = auditServiceMock.findActions(findRequest).getActionsList();

        // проверяем, что при добавлении classifier_confidence c 2 знаками после запятой
        assertThat(actions)
            .extracting(MboAudit.MboAction::getEntityId)
            .containsOnly(offer.getId());

        Optional<MboAudit.MboAction> insertAction = actions.stream()
            .filter(action -> action.getPropertyName().equals("classifier_confidence")).findAny();
        assertThat(insertAction).isPresent();
        assertThat(insertAction.get().getOldValue()).isNullOrEmpty();
        assertThat(insertAction.get().getNewValue()).isEqualTo("0.983");

        // проверяем, что при изменении classifier_confidence после 3-го знака, аудита нет
        offer = offerRepository.getOfferById(offer.getId())
            .setClassifierConfidenceInternal(0.9831);
        auditServiceMock.clearActions();
        offerRepository.updateOffers(Collections.singletonList(offer));
        actions = auditServiceMock.findActions(findRequest).getActionsList();
        assertThat(actions).isEmpty();

        // проверяем, что при изменении classifier_confidence до 3-го знака, аудит есть
        offer = offerRepository.getOfferById(offer.getId())
            .setClassifierConfidenceInternal(0.9731);
        auditServiceMock.clearActions();
        offerRepository.updateOffers(Collections.singletonList(offer));
        actions = auditServiceMock.findActions(findRequest).getActionsList();
        Optional<MboAudit.MboAction> updateAction = actions.stream()
            .filter(action -> action.getPropertyName().equals("classifier_confidence")).findAny();
        assertThat(updateAction).isPresent();
        assertThat(updateAction.get().getOldValue()).isEqualTo("0.983");
        assertThat(updateAction.get().getNewValue()).isEqualTo("0.973");
    }

    private void assertSourceAndUser(MboAudit.MboAction action,
                                     String staffLogin,
                                     MboAudit.Source source,
                                     String sourceId) {
        assertThat(action.getStaffLogin()).isEqualTo(staffLogin);
        assertThat(action.getSource()).isEqualTo(source);
        assertThat(action.getSourceId()).isEqualTo(sourceId);
    }

    private MboAudit.FindActionsRequest createFindRequest(Offer offer) {
        return MboAudit.FindActionsRequest.newBuilder()
            .setEntityId(offer.getId())
            .addEntityType(MboAudit.EntityType.CM_BLUE_OFFER)
            .setLength(100)
            .build();
    }

    private MboAudit.FindActionsRequest createFindRequest(long mappingId) {
        return MboAudit.FindActionsRequest.newBuilder()
            .setEntityId(mappingId)
            .setLength(100)
            .build();
    }
}

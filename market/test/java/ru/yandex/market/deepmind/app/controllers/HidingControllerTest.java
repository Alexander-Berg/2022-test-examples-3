package ru.yandex.market.deepmind.app.controllers;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.jooq.impl.TableRecordImpl;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.app.pojo.DisplayHidingTicketHistory;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingReasonType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Hiding;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingTicketHistory;
import ru.yandex.market.deepmind.common.hiding.DisplayHiding;
import ru.yandex.market.deepmind.common.hiding.HidingDescription;
import ru.yandex.market.deepmind.common.hiding.HidingReason;
import ru.yandex.market.deepmind.common.hiding.HidingReasonDescriptionRepository;
import ru.yandex.market.deepmind.common.hiding.HidingRepository;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.HidingTicketHistoryRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;

import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_MANUALLY_HIDDEN_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_OTHER_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.HIDDEN_SUPPLIER_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.HIDDEN_SUPPLIER_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.MDM_MBOC_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.MDM_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45K_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_REASON;

public class HidingControllerTest extends DeepmindBaseAppDbTestClass {
    @Resource(name = "deepmindDsl")
    protected DSLContext dslContext;
    @Resource
    protected ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    protected SupplierRepository deepmindSupplierRepository;
    @Resource
    private HidingRepository hidingRepository;
    @Resource
    private HidingTicketHistoryRepository hidingTicketHistoryRepository;

    private HidingController hidingController;

    @Resource
    protected HidingReasonDescriptionRepository hidingReasonDescriptionRepository;
    private HidingReasonDescription skk45KDescr;
    private HidingReasonDescription aboOtherDescr;
    private HidingReasonDescription aboManHiddenDescr;
    private HidingReasonDescription mdmMbocDescr;

    @Before
    public void setUp() {
        hidingController = new HidingController(hidingRepository, hidingTicketHistoryRepository);
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
    }

    @Test
    public void getHidings() {
        var offer1 = createOffer(1, "ssku1");
        var offer2 = createOffer(2, "ssku2");
        var offer3 = createOffer(3, "ssku3");
        serviceOfferReplicaRepository.save(offer1, offer2, offer3);

        var descriptions = insertHidingsReasonDescriptionsWithRes(
            createHidingDescription(SKK_45K_SUBREASON.toReasonKey(), "Стоп слово"),
            createHidingDescription(ABO_OTHER_SUBREASON.toReasonKey(), "Другое"),
            createHidingDescription(ABO_MANUALLY_HIDDEN_SUBREASON.toReasonKey(), "Скрыто"),
            createHidingDescription(MDM_MBOC_SUBREASON.toReasonKey(), "Скрыто mboc")
        );
        skk45KDescr = descriptions.get(SKK_45K_SUBREASON.toReasonKey());
        aboOtherDescr = descriptions.get(ABO_OTHER_SUBREASON.toReasonKey());
        aboManHiddenDescr = descriptions.get(ABO_MANUALLY_HIDDEN_SUBREASON.toReasonKey());
        mdmMbocDescr = descriptions.get(MDM_MBOC_SUBREASON.toReasonKey());
        hidingRepository.refreshCacheActualHidingSubReasonDescriptions();

        var hiding1 = createHiding(skk45KDescr.getId(), SKK_45K_SUBREASON, offer1).setSubreasonId("жопа");
        var hiding2 = createHiding(aboOtherDescr.getId(), ABO_OTHER_SUBREASON, offer2);
        var hiding3 = createHiding(aboManHiddenDescr.getId(), ABO_MANUALLY_HIDDEN_SUBREASON, offer2);
        var hiding4 = createHiding(mdmMbocDescr.getId(), MDM_MBOC_SUBREASON, offer3);
        insertHidings(hiding1, hiding2, hiding3, hiding4);

        var hidings = hidingController.hidings(
            ServiceOfferKey.toRawStrings(List.of(
                new ServiceOfferKey(1, "ssku1"),
                new ServiceOfferKey(2, "ssku2")),
                ":")
        );

        Assertions.assertThat(hidings)
            .containsExactlyInAnyOrder(
                createDisplayHiding(hiding1, "Стоп слово - жопа"),
                createDisplayHiding(hiding2, "Другое"),
                createDisplayHiding(hiding3, "Скрыто")
            );
    }

    @Test
    public void hidingHistory() {
        var offer1 = createOffer(1, "ssku1");
        var offer2 = createOffer(2, "ssku2");
        var offer3 = createOffer(3, "ssku3");
        serviceOfferReplicaRepository.save(offer1, offer2, offer3);

        var desc1 = createHidingDescription(SKK_45K_SUBREASON.toReasonKey(), "Стоп слово");
        var desc2 = createHidingDescription(ABO_OTHER_SUBREASON.toReasonKey(), "Другое");
        var desc3 = createHidingDescription(ABO_MANUALLY_HIDDEN_SUBREASON.toReasonKey(), "Скрыто");
        var desc4 = createHidingDescription(MDM_MBOC_SUBREASON.toReasonKey(), "Скрыто mboc");
        insertHidingsReasonDescriptions(desc1, desc2, desc3, desc4,
            createHidingDescription(SKK_REASON.toReasonKey(), "SKK_REASON"),
            createHidingDescription(ABO_REASON.toReasonKey(), "ABO_REASON"),
            createHidingDescription(MDM_REASON.toReasonKey(), "MDM_REASON")
        );
        hidingRepository.refreshCacheActualHidingSubReasonDescriptions();

        hidingTicketHistoryRepository.save(
            ticketHistory(desc1.getReasonKey(), 1, "ssku1", "TICKET-1"),
            ticketHistory(desc2.getReasonKey(), 1, "ssku1", "QUEUE-1"),
            ticketHistory(desc2.getReasonKey(), 2, "ssku2", "QUEUE-1"),
            ticketHistory(desc2.getReasonKey(), 3, "ssku3", "QUEUE-1"),
            ticketHistory("NOT_existing", 4, "ssku4", "AAA-1"),
            ticketHistory("HIDDEN_SUPPLIER_HIDDEN_SUPPLIER", 4, "ssku4", "AAA-2"),
            ticketHistory("HIDDEN_SUPPLIER_not_existing", 4, "ssku4", "AAA-3")
        );

        var ticketHistories = hidingController.hidingTicketHistory(
            ServiceOfferKey.toRawStrings(List.of(
                new ServiceOfferKey(1, "ssku1"),
                new ServiceOfferKey(2, "ssku2"),
                new ServiceOfferKey(4, "ssku4")
            ), ":")
        );

        Assertions.assertThat(ticketHistories)
            .usingElementComparatorOnFields("hidingDescription", "supplierId", "shopSku", "ticket")
            .containsExactlyInAnyOrder(
                displayHidingTicket(desc1, 1, "ssku1", "TICKET-1"),
                displayHidingTicket(desc2, 1, "ssku1", "QUEUE-1"),
                displayHidingTicket(desc2, 2, "ssku2", "QUEUE-1"),
                displayHidingTicket((HidingDescription) null, 4, "ssku4", "AAA-1"),
                displayHidingTicket(new HidingDescription(HIDDEN_SUPPLIER_REASON + "_" + HIDDEN_SUPPLIER_SUBREASON,
                        "Поставщик скрыт"),
                    4, "ssku4", "AAA-2"),
                displayHidingTicket((HidingDescription) null, 4, "ssku4", "AAA-3")
            );
    }

    protected void insertHidings(Hiding... hidings) {
        Arrays.stream(hidings)
            .map(hiding -> dslContext.newRecord(Tables.HIDING, hiding))
            .forEach(TableRecordImpl::insert);
    }

    protected void insertHidingsReasonDescriptions(HidingReasonDescription... reasonDescriptions) {
        Arrays.stream(reasonDescriptions)
            .map(description -> dslContext.newRecord(Tables.HIDING_REASON_DESCRIPTION, description))
            .forEach(TableRecordImpl::insert);
    }

    protected Map<String, HidingReasonDescription> insertHidingsReasonDescriptionsWithRes(
        HidingReasonDescription... reasonDescriptions) {
        return insertHidingsReasonDescriptionsWithRes(List.of(reasonDescriptions));
    }

    protected Map<String, HidingReasonDescription> insertHidingsReasonDescriptionsWithRes(
        Collection<HidingReasonDescription> reasonDescriptions) {
        hidingReasonDescriptionRepository.addHidingDescriptionsIfNotExists(reasonDescriptions);
        return hidingReasonDescriptionRepository.findByReasonKeysMap(
            reasonDescriptions.stream().map(HidingReasonDescription::getReasonKey).collect(Collectors.toSet()));
    }

    protected Hiding createHiding(Long reasonKeyId, String subreasonId,
                                  ServiceOfferReplica offer, MboUser user,
                                  @Nullable String hidingAt, @Nullable String comment) {
        return new Hiding()
            .setReasonKeyId(reasonKeyId)
            .setSubreasonId(subreasonId)
            .setSupplierId(offer.getBusinessId())
            .setShopSku(offer.getShopSku())
            .setUserId(user != null ? user.getUid() : null)
            .setUserName(user != null ? user.getFullName() : null)
            .setHiddenAt(hidingAt == null ? null : Instant.parse(hidingAt))
            .setComment(comment);
    }

    protected Hiding createHiding(Long reasonKeyId, HidingReason subreason, ServiceOfferReplica offer) {
        return createHiding(reasonKeyId, subreason.toString(), offer, null, null, null);
    }

    protected ServiceOfferReplica createOffer(int supplierId, String ssku) {
        var supplier = deepmindSupplierRepository.findById(supplierId).get();
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title " + ssku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(1L)
            .setSupplierType(supplier.getSupplierType())
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private HidingReasonDescription createHidingDescription(String reasonKey,
                                                            String desc) {
        return new HidingReasonDescription()
            .setType(HidingReasonType.REASON_KEY)
            .setReasonKey(reasonKey)
            .setExtendedDesc(desc);
    }

    private DisplayHiding createDisplayHiding(Hiding hiding, String shortText) {
        var reasonKey = hidingReasonDescriptionRepository.findById(hiding.getReasonKeyId()).get().getReasonKey();
        return new DisplayHiding()
            .setSupplierId(hiding.getSupplierId())
            .setShopSku(hiding.getShopSku())
            .setReasonKey(reasonKey)
            .setStopWord(reasonKey.contains(SKK_45K_SUBREASON.toString()) ? hiding.getSubreasonId() : null)
            .setUserName(hiding.getUserName())
            .setComment(hiding.getComment())
            .setShortText(shortText);
    }

    protected static HidingTicketHistory ticketHistory(String reasonKey, int supplierId, String ssku, String ticket) {
        return new HidingTicketHistory()
            .setTicket(ticket)
            .setReasonKey(reasonKey)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setCreationTs(Instant.now());
    }

    private DisplayHidingTicketHistory displayHidingTicket(HidingReasonDescription description,
                                                           int supplier, String ssku, String ticket) {
        var index = description.getReasonKey().lastIndexOf("_");
        var reason = description.getReasonKey().substring(0, index);
        var subreason = description.getReasonKey().substring(index + 1);
        var hidingDescription = new HidingDescription(reason + "_" + subreason, description.getExtendedDesc());
        return displayHidingTicket(hidingDescription, supplier, ssku, ticket);
    }

    private DisplayHidingTicketHistory displayHidingTicket(@Nullable HidingDescription hidingDescription,
                                                           int supplier, String ssku, String ticket) {
        return new DisplayHidingTicketHistory()
            .setHidingDescription(hidingDescription)
            .setSupplierId(supplier)
            .setShopSku(ssku)
            .setTicket(ticket);
    }
}

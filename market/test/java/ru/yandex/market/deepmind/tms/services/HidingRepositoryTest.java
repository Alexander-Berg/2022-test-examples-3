package ru.yandex.market.deepmind.tms.services;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingReasonType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Hiding;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.hiding.DisplayHiding;
import ru.yandex.market.deepmind.common.hiding.HidingDescription;
import ru.yandex.market.deepmind.common.hiding.HidingRepository;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_FAULTY_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.ABO_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.FEED_450_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.FEED_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.MDM_MISS_COUNTRY_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.MDM_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.PARTNER_API_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.PARTNER_API_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45J_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_45K_SUBREASON;
import static ru.yandex.market.deepmind.common.hiding.HidingReason.SKK_REASON;
import static ru.yandex.market.deepmind.common.hiding.HidingRepository.convertToDisplayHiding;

/**
 * @author kravchenko-aa
 * @date 16.03.2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class HidingRepositoryTest extends BaseHidingsServiceTest {
    public static final MboUser USER = new MboUser(12345, "Вася пупкин", "agent007@y.ru");

    @Autowired
    private HidingRepository hidingRepository;

    private ServiceOfferReplica offer1;
    private ServiceOfferReplica offer2;
    private ServiceOfferReplica offer3;
    private HidingReasonDescription aboFaultyDescr;
    private HidingReasonDescription skk45KDescr;
    private HidingReasonDescription skk45JDescr;
    private HidingReasonDescription mdmMissCountryDescr;
    private HidingReasonDescription partnerApiDescr;
    private HidingReasonDescription feed450Descr;

    @Before
    public void setUp() {
        super.setUp();
        offer1 = offer(1, "sku_1", 100L);
        offer2 = offer(2, "sku_2", 42L);
        offer3 = offer(3, "sku_3", 0L);

        var hidingsDescriptionMap = insertHidingsReasonDescriptionsWithRes(
            createReasonDescription(SKK_REASON.toString(), HidingReasonType.REASON, "SKK reason"),
            createReasonDescription(ABO_REASON.toString(), HidingReasonType.REASON, "ABO reason"),
            createReasonDescription(MDM_REASON.toString(), HidingReasonType.REASON, "MDM reason"),
            createReasonDescription(PARTNER_API_REASON.toString(), HidingReasonType.REASON, "Партнерский интерфейс"),
            createReasonDescription(FEED_REASON.toString(), HidingReasonType.REASON, "Ошибки в прайс-листе"),
            createReasonDescription(SKK_45K_SUBREASON.toReasonKey(),
                "Не важно что это тут за текст :,.;№%", "По стоп слову"),
            createReasonDescription(SKK_45J_SUBREASON.toReasonKey(),
                "Предложение невозможно разместить на Маркете"),
            createReasonDescription(ABO_FAULTY_SUBREASON.toReasonKey(), "Брак"),
            createReasonDescription(MDM_MISS_COUNTRY_SUBREASON.toReasonKey(),
                "Не указана страна производства товара"),
            createReasonDescription(PARTNER_API_SUBREASON.toReasonKey(),
                "Партнерский интерфейс"),
            createReasonDescription(FEED_450_SUBREASON.toReasonKey(), "<450>")
        );
        aboFaultyDescr = hidingsDescriptionMap.get(ABO_FAULTY_SUBREASON.toReasonKey());
        skk45KDescr = hidingsDescriptionMap.get(SKK_45K_SUBREASON.toReasonKey());
        skk45JDescr = hidingsDescriptionMap.get(SKK_45J_SUBREASON.toReasonKey());
        mdmMissCountryDescr = hidingsDescriptionMap.get(MDM_MISS_COUNTRY_SUBREASON.toReasonKey());
        partnerApiDescr = hidingsDescriptionMap.get(PARTNER_API_SUBREASON.toReasonKey());
        feed450Descr = hidingsDescriptionMap.get(FEED_450_SUBREASON.toReasonKey());
    }

    @Test
    public void testFindHidings() {
        hidingReasonDescriptionRepository.save();
        Hiding hiding1 = createHiding(aboFaultyDescr.getId(), "Faulty", offer1, USER, null, null);
        Hiding hiding2 = createHiding(skk45KDescr.getId(), "жопа", offer2, USER, null, null);
        Hiding hiding3 = createHiding(aboFaultyDescr.getId(), "Faulty", offer2, USER, null, null);
        Hiding hiding4 = createHiding(skk45JDescr.getId(), "45j", offer2, USER, null, null);

        insertHidings(hiding1, hiding2, hiding3, hiding4);

        var allHidingDescription = hidingReasonDescriptionRepository.findAllMap();

        DisplayHiding hidingInfo2 = convertToDisplayHiding(hiding2, "жопа", allHidingDescription);
        DisplayHiding hidingInfo3 = convertToDisplayHiding(hiding3, "Брак", allHidingDescription);
        DisplayHiding hidingInfo4 = convertToDisplayHiding(hiding4, "Предложение невозможно разместить на Маркете",
            allHidingDescription);

        assertThat(hidingRepository.getHidings(new ServiceOfferKey(offer2.getSupplierId(), offer2.getShopSku())))
            .containsExactlyInAnyOrder(hidingInfo2, hidingInfo3, hidingInfo4);
    }

    @Test
    @SuppressWarnings("checkstyle:LineLength")
    public void testFindSubReasons() {
        Hiding hiding1 = createHiding(aboFaultyDescr.getId(), "Faulty", offer1, USER, null, null);
        Hiding hiding2 = createHiding(skk45KDescr.getId(), "жопа", offer2, USER, null, null);
        Hiding hiding3 = createHiding(skk45KDescr.getId(), "еще жопа", offer2, USER, null, null);
        Hiding hiding4 = createHiding(skk45JDescr.getId(), "45j", offer3, USER, null, null);
        Hiding hiding5 = createHiding(partnerApiDescr.getId(), "PARTNER_API", offer3, USER, null, null);
        Hiding hiding6 = createHiding(mdmMissCountryDescr.getId(), "some-id", offer3, USER, null, null);
        Hiding hiding7 = createHiding(feed450Descr.getId(), "450", offer3, USER, null, null);

        insertHidings(hiding1, hiding2, hiding3, hiding4, hiding5, hiding6, hiding7);

        hidingRepository.refreshCacheActualHidingSubReasonDescriptions();
        assertThat(hidingRepository.getHidingDescriptions(hidingRepository.getActualHidingReasonKeys()))
            // order is important
            .usingElementComparatorOnFields("reasonKey", "shortText")
            .containsExactly(
                new HidingDescription(ABO_FAULTY_SUBREASON.toReasonKey(), "Брак"),
                new HidingDescription(FEED_450_SUBREASON.toReasonKey(), "<450>"),
                new HidingDescription(MDM_MISS_COUNTRY_SUBREASON.toReasonKey(), "Не указана страна производства товара"),
                new HidingDescription(PARTNER_API_SUBREASON.toReasonKey(), "Партнерский интерфейс"),
                new HidingDescription(SKK_45J_SUBREASON.toReasonKey(), "Предложение невозможно разместить на Маркете"),
                new HidingDescription(SKK_45K_SUBREASON.toReasonKey(), "По стоп слову")
            );
    }

    @Test
    public void testDoubleRefreshCacheActualHidingSubReasonDescriptions() {
        Hiding hiding1 = createHiding(aboFaultyDescr.getId(), "Faulty", offer1, USER, null, null);
        Hiding hiding2 = createHiding(skk45KDescr.getId(), "жопа", offer2, USER, null, null);
        Hiding hiding3 = createHiding(skk45KDescr.getId(), "еще жопа", offer2, USER, null, null);
        insertHidings(hiding1, hiding2, hiding3);

        hidingRepository.refreshCacheActualHidingSubReasonDescriptions();
        assertThat(hidingRepository.getHidingDescriptions(hidingRepository.getActualHidingReasonKeys()))
            // order is important
            .containsExactly(
                new HidingDescription(ABO_FAULTY_SUBREASON.toReasonKey(), "Брак"),
                new HidingDescription(SKK_45K_SUBREASON.toReasonKey(), "По стоп слову")
            );

        // double call
        hidingRepository.refreshCacheActualHidingSubReasonDescriptions();
        assertThat(hidingRepository.getHidingDescriptions(hidingRepository.getActualHidingReasonKeys()))
            // order is important
            .containsExactly(
                new HidingDescription(ABO_FAULTY_SUBREASON.toReasonKey(), "Брак"),
                new HidingDescription(SKK_45K_SUBREASON.toReasonKey(), "По стоп слову")
            );
    }

    @Test
    public void testFindStopWords() {
        Hiding hiding1 = createHiding(aboFaultyDescr.getId(), "Faulty", offer1, USER, null, null);
        Hiding hiding2 = createHiding(skk45KDescr.getId(), "жопа", offer2, USER, null, null);
        Hiding hiding3 = createHiding(skk45JDescr.getId(), "45j", offer3, USER, null, null);
        Hiding hiding4 = createHiding(skk45KDescr.getId(), "кокаин", offer3, USER, null, null);
        Hiding hiding5 = createHiding(skk45KDescr.getId(), "china", offer3, USER, null, null);

        insertHidings(hiding1, hiding2, hiding3, hiding4, hiding5);
        assertThat(hidingRepository.getHidingStopWords())
            // order is important
            .containsExactly("china", "жопа", "кокаин");
    }
}

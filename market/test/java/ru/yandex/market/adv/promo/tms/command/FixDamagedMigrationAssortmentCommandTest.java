package ru.yandex.market.adv.promo.tms.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferPromos;
import Market.DataCamp.DataCampUnitedOffer;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.logbroker.model.DatacampMessageLogbrokerEvent;
import ru.yandex.market.adv.promo.service.environment.EnvironmentService;
import ru.yandex.market.adv.promo.utils.model.BasicAndServiceOffersPair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerBusinessDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnersBusinessResponse;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.adv.promo.service.environment.constant.EnvironmentSettingConstants.DATACAMP_GRAPHQL_QUERY;
import static ru.yandex.market.adv.promo.tms.command.DeletePromosCommand.COMMAND_NAME;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createBasicOffer;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createPromo;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createServiceOfferWithPromos;
import static ru.yandex.market.adv.promo.utils.DataCampOfferUtils.createUnitedOffer;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_CASHBACK_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_CASHBACK_PROMOS_META;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_PROMOS;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.ALL_PARTNER_PROMOS_META;
import static ru.yandex.market.mbi.datacamp.stroller.model.graphql.OfferField.OFFER_IDENTIFIERS;

class FixDamagedMigrationAssortmentCommandTest extends FunctionalTest {
    @Autowired
    FixDamagedMigrationAssortmentCommand command;
    @Autowired
    private Terminal terminal;
    @Autowired
    private DataCampClient dataCampClient;
    @Autowired
    MbiApiClient mbiApiClient;
    @Autowired
    LogbrokerEventPublisher<DatacampMessageLogbrokerEvent> logbrokerPublisher;
    private StringWriter terminalWriter;
    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    public void setUpConfigurations() {
        terminalWriter = new StringWriter();

        when(terminal.getWriter()).thenReturn(spy(new PrintWriter(terminalWriter)));
    }

    @Test
    @DbUnitDataSet(
            before = "FixDamagedMigrationAssortmentCommandTest/testAllOk/before.csv",
            after = "FixDamagedMigrationAssortmentCommandTest/testAllOk/after.csv"
    )
    public void testAllOk() {
        allOk();
    }

    @Test
    @DbUnitDataSet(
            before = "FixDamagedMigrationAssortmentCommandTest/testAllOk/before_graphqlQuery.csv",
            after = "FixDamagedMigrationAssortmentCommandTest/testAllOk/after.csv"
    )
    public void testAllOkGraphqlQuery() {
        allOk();
    }

    private void allOk() {
        long partnerId1 = 111;
        long businessId1 = 1111;
        long partnerId2 = 222;
        long businessId2 = 2222;
        Map<String, String> options = Map.of(
                "fix-damaged-assortment", "",
                "partners", partnerId1 + "," + partnerId2
        );
        CommandInvocation commandInvocation = new CommandInvocation(COMMAND_NAME, new String[0], options);
        PartnersBusinessResponse mbiApiResponse = new PartnersBusinessResponse(
                List.of(
                        new PartnerBusinessDTO(partnerId1, businessId1),
                        new PartnerBusinessDTO(partnerId2, businessId2)
                )
        );
        doReturn(mbiApiResponse).when(mbiApiClient).getBusinessesForPartners(any());

        String offerId11 = "offer11";
        String promoId111 = "111_PRADAAF";
        String promoId112 = "111_CAG_51235";
        String promoId113 = "6234_WAHAHA_MIGR";
        DataCampOffer.Offer basicOffer11 = createBasicOffer(offerId11, Math.toIntExact(partnerId1), Math.toIntExact(businessId1));
        List<DataCampOfferPromos.Promo> partnerPromos11 = List.of(createPromo(promoId111), createPromo(promoId112), createPromo(promoId113));
        String promoId114 = "111_PSC_1245125";
        String promoId115 = "6234_PCC_616512";
        List<DataCampOfferPromos.Promo> casbackPromos11 = List.of(createPromo(promoId114), createPromo(promoId115));
        DataCampOffer.Offer serviceOffer11 = createServiceOfferWithPromos(basicOffer11.getIdentifiers(), null, null, partnerPromos11, casbackPromos11);
        DataCampUnitedOffer.UnitedOffer unitedOffer11 = createUnitedOffer(new BasicAndServiceOffersPair(basicOffer11, serviceOffer11), Math.toIntExact(partnerId1));

        String offerId12 = "offer12";
        DataCampOffer.Offer basicOffer12 = createBasicOffer(offerId12, Math.toIntExact(partnerId1), Math.toIntExact(businessId1));
        List<DataCampOfferPromos.Promo> partnerPromos12 = List.of(createPromo(promoId111), createPromo(promoId112));
        List<DataCampOfferPromos.Promo> casbackPromos12 = List.of(createPromo(promoId115));
        DataCampOffer.Offer serviceOffer12 = createServiceOfferWithPromos(basicOffer12.getIdentifiers(), null, null, partnerPromos12, casbackPromos12);
        DataCampUnitedOffer.UnitedOffer unitedOffer12 = createUnitedOffer(new BasicAndServiceOffersPair(basicOffer12, serviceOffer12), Math.toIntExact(partnerId1));

        String offerId13 = "offer13";
        DataCampOffer.Offer basicOffer13 = createBasicOffer(offerId13, Math.toIntExact(partnerId1), Math.toIntExact(businessId1));
        List<DataCampOfferPromos.Promo> partnerPromos13 = List.of(createPromo(promoId111), createPromo(promoId112));
        List<DataCampOfferPromos.Promo> casbackPromos13 = List.of(createPromo(promoId114));
        DataCampOffer.Offer serviceOffer13 = createServiceOfferWithPromos(basicOffer13.getIdentifiers(), null, null, partnerPromos13, casbackPromos13);
        DataCampUnitedOffer.UnitedOffer unitedOffer13 = createUnitedOffer(new BasicAndServiceOffersPair(basicOffer13, serviceOffer13), Math.toIntExact(partnerId1));

        SearchBusinessOffersResult firstPageResultPartner1 = SearchBusinessOffersResult.builder()
                .setOffers(List.of(unitedOffer11, unitedOffer12, unitedOffer13))
                .setNextPageToken("next1")
                .build();
        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            doReturn(firstPageResultPartner1).when(dataCampClient).searchBusinessOffers(
                    argThat(
                            request -> request.getPartnerId() == partnerId1 &&
                                    request.getPageRequest().seekKey().isEmpty() &&
                                    CollectionUtils.isEqualCollection(
                                            request.getOfferQuery().getFields(), EnumSet.of(
                                                    OFFER_IDENTIFIERS,
                                                    ALL_PARTNER_PROMOS,
                                                    ALL_PARTNER_PROMOS_META,
                                                    ALL_PARTNER_CASHBACK_PROMOS,
                                                    ALL_PARTNER_CASHBACK_PROMOS_META
                                            ))
                    ));
        } else {
            doReturn(firstPageResultPartner1).when(dataCampClient).searchBusinessOffers(
                    argThat(
                            request -> request.getPartnerId() == partnerId1 &&
                                    request.getPageRequest().seekKey().isEmpty()
                    ));
        }

        String offerId14 = "offer14";
        DataCampOffer.Offer basicOffer14 = createBasicOffer(offerId14, Math.toIntExact(partnerId1), Math.toIntExact(businessId1));
        List<DataCampOfferPromos.Promo> partnerPromos14 = List.of(createPromo(promoId111), createPromo(promoId113));
        List<DataCampOfferPromos.Promo> casbackPromos14 = List.of(createPromo(promoId114));
        DataCampOffer.Offer serviceOffer14 = createServiceOfferWithPromos(basicOffer14.getIdentifiers(), null, null, partnerPromos14, casbackPromos14);
        DataCampUnitedOffer.UnitedOffer unitedOffer14 = createUnitedOffer(new BasicAndServiceOffersPair(basicOffer14, serviceOffer14), Math.toIntExact(partnerId1));

        SearchBusinessOffersResult secondPageResultPartner1 = SearchBusinessOffersResult.builder()
                .setOffers(List.of(unitedOffer14))
                .build();
        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            doReturn(secondPageResultPartner1).when(dataCampClient).searchBusinessOffers(
                    argThat(
                            request -> request.getPartnerId() == partnerId1 &&
                                    request.getPageRequest().seekKey().isPresent() &&
                                    request.getPageRequest().seekKey().get().equals("next1") &&
                                    CollectionUtils.isEqualCollection(
                                            request.getOfferQuery().getFields(), EnumSet.of(
                                                    OFFER_IDENTIFIERS,
                                                    ALL_PARTNER_PROMOS,
                                                    ALL_PARTNER_PROMOS_META,
                                                    ALL_PARTNER_CASHBACK_PROMOS,
                                                    ALL_PARTNER_CASHBACK_PROMOS_META
                                            ))
                    ));
        } else {
            doReturn(secondPageResultPartner1).when(dataCampClient).searchBusinessOffers(
                    argThat(
                            request -> request.getPartnerId() == partnerId1 &&
                                    request.getPageRequest().seekKey().isPresent() &&
                                    request.getPageRequest().seekKey().get().equals("next1")
                    ));
        }

        String offerId21 = "offer21";
        String promoId211 = "222_PRADAAF";
        String promoId212 = "333_CAG_51235";
        DataCampOffer.Offer basicOffer21 = createBasicOffer(offerId21, Math.toIntExact(partnerId2), Math.toIntExact(businessId2));
        List<DataCampOfferPromos.Promo> partnerPromos21 = List.of(createPromo(promoId211), createPromo(promoId212));
        DataCampOffer.Offer serviceOffer21 = createServiceOfferWithPromos(basicOffer21.getIdentifiers(), null, null, partnerPromos21, null);
        DataCampUnitedOffer.UnitedOffer unitedOffer21 = createUnitedOffer(new BasicAndServiceOffersPair(basicOffer21, serviceOffer21), Math.toIntExact(partnerId2));

        SearchBusinessOffersResult firstPageResultPartner2 = SearchBusinessOffersResult.builder()
                .setOffers(List.of(unitedOffer21))
                .build();
        if (environmentService.getSettingsBooleanValue(DATACAMP_GRAPHQL_QUERY).orElse(false)) {
            doReturn(firstPageResultPartner2).when(dataCampClient).searchBusinessOffers(
                    argThat(request -> request.getPartnerId() == partnerId2 &&
                                    CollectionUtils.isEqualCollection(
                                            request.getOfferQuery().getFields(), EnumSet.of(
                                                    OFFER_IDENTIFIERS,
                                                    ALL_PARTNER_PROMOS,
                                                    ALL_PARTNER_PROMOS_META,
                                                    ALL_PARTNER_CASHBACK_PROMOS,
                                                    ALL_PARTNER_CASHBACK_PROMOS_META
                                            ))
                    ));
        } else {
            doReturn(firstPageResultPartner2).when(dataCampClient).searchBusinessOffers(
                    argThat(request -> request.getPartnerId() == partnerId2)
            );
        }
        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);

        command.executeCommand(commandInvocation, terminal);

        verify(logbrokerPublisher, times(3)).publishEvent(eventCaptor.capture());
        List<DatacampMessageLogbrokerEvent> allValues = eventCaptor.getAllValues();
        List<DataCampOffer.Offer> offerList1 = allValues.get(0).getPayload().getOffersList().get(0).getOfferList();
        assertThat(offerList1).hasSize(1);
        assertThat(offerList1)
                .singleElement()
                .satisfies(
                        offer -> {
                            assertThat(offer.getIdentifiers()).isEqualTo(basicOffer21.getIdentifiers());
                            assertThat(offer.getPromos().getPartnerPromos().getPromosList()).map(promo -> promo.getId())
                                    .containsExactlyInAnyOrder(promoId211);
                            assertThat(offer.getPromos().getPartnerPromos().getMeta().getTimestamp().getNanos()).isEqualTo(1);
                            assertThat(offer.getPromos().hasPartnerCashbackPromos()).isFalse();
                        }
                );

        List<DataCampOffer.Offer> offerList2 = allValues.get(1).getPayload().getOffersList().get(0).getOfferList();
        assertThat(offerList2).hasSize(2);
        assertThat(offerList2.get(0))
                .satisfies(
                        offer -> {
                            assertThat(offer.getIdentifiers()).isEqualTo(basicOffer11.getIdentifiers());
                            assertThat(offer.getPromos().getPartnerPromos().getPromosList()).map(promo -> promo.getId())
                                    .containsExactlyInAnyOrder(promoId111, promoId112);
                            assertThat(offer.getPromos().getPartnerPromos().getMeta().getTimestamp().getNanos()).isEqualTo(1);
                            assertThat(offer.getPromos().getPartnerCashbackPromos().getPromosList()).map(promo -> promo.getId())
                                    .containsExactlyInAnyOrder(promoId114);
                            assertThat(offer.getPromos().getPartnerCashbackPromos().getMeta().getTimestamp().getNanos()).isEqualTo(1);
                        }
                );
        assertThat(offerList2.get(1))
                .satisfies(
                        offer -> {
                            assertThat(offer.getIdentifiers()).isEqualTo(basicOffer12.getIdentifiers());
                            assertThat(offer.getPromos().hasPartnerPromos()).isFalse();
                            assertThat(offer.getPromos().getPartnerCashbackPromos().getPromosList()).isEmpty();
                            assertThat(offer.getPromos().getPartnerCashbackPromos().getMeta().getTimestamp().getNanos()).isEqualTo(1);
                        }
                );

        List<DataCampOffer.Offer> offerList3 = allValues.get(2).getPayload().getOffersList().get(0).getOfferList();
        assertThat(offerList3).hasSize(1);
        assertThat(offerList3)
                .singleElement()
                .satisfies(
                        offer -> {
                            assertThat(offer.getIdentifiers()).isEqualTo(basicOffer14.getIdentifiers());
                            assertThat(offer.getPromos().getPartnerPromos().getPromosList()).map(promo -> promo.getId())
                                    .containsExactlyInAnyOrder(promoId111);
                            assertThat(offer.getPromos().getPartnerPromos().getMeta().getTimestamp().getNanos()).isEqualTo(1);
                            assertThat(offer.getPromos().hasPartnerCashbackPromos()).isFalse();
                        }
                );
    }
}

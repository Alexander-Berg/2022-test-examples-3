package ru.yandex.market.logistics.management.service.client;

import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.id.ConfirmLegalInfoRequest;
import ru.yandex.market.id.GetByIdRequest;
import ru.yandex.market.id.GetByRegistrationNumberRequest;
import ru.yandex.market.id.GetByRegistrationNumberResponse;
import ru.yandex.market.id.GetOrCreateMarketIdRequest;
import ru.yandex.market.id.LegalInfoType;
import ru.yandex.market.id.LinkMarketIdRequest;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.id.UpdateLegalInfoRequest;
import ru.yandex.market.id.UpdateLegalInfoResponse;
import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.converter.AddressConverter;
import ru.yandex.market.logistics.management.domain.converter.AddressMapper;
import ru.yandex.market.logistics.management.domain.converter.LegalInfoConverter;
import ru.yandex.market.logistics.management.domain.entity.LegalInfo;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.repository.LegalInfoRepository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Disabled("DELIVERY-33203")
@ExtendWith(MockitoExtension.class)
@DisplayName("Тест взаимодействия с marketId")
@ParametersAreNonnullByDefault
class MarketIdServiceTest extends AbstractTest {

    private static final List<LegalInfoType> LEGAL_INFO_TYPES = ImmutableList.of(
        LegalInfoType.LEGAL_NAME,
        LegalInfoType.INN,
        LegalInfoType.TYPE,
        LegalInfoType.REGISTRATION_NUMBER
    );

    private static final long PARTNER_ID = 1L;

    private static final String PARTNER_TYPE = "YADELIVERY";

    private static final long MARKET_ID = 777;

    private static final long OGRN = 123456789;

    private static final long INN = 987654321;

    private static final String INCORPORATION = "ООО \"ООО\"";

    private static final String LEGAL_FORM = "ООО";

    @Mock
    private MarketIdServiceGrpc.MarketIdServiceBlockingStub marketIdServiceBlockingStub;

    private MarketIdService marketIdService;

    @BeforeEach
    void setUp() {
        AddressConverter addressConverter = new AddressConverter(Mappers.getMapper(AddressMapper.class));
        LegalInfoConverter legalInfoConverter = new LegalInfoConverter(
            addressConverter,
            Mockito.mock(LegalInfoRepository.class)
        );
        marketIdService = new MarketIdService(marketIdServiceBlockingStub, legalInfoConverter);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(marketIdServiceBlockingStub);
    }

    @Test
    @DisplayName("Успешное получение marketId по ОГРН")
    void testGetMarketIdSuccess() {
        softly.assertThat(testGetMarketId(true))
            .as("Asserting that marketId is valid")
            .hasValue(MARKET_ID);
    }

    @Test
    @DisplayName("Неудачное получение marketId по ОГРН")
    void testGetMarketIdFail() {
        softly.assertThat(testGetMarketId(false))
            .as("Asserting that marketId is empty")
            .isEmpty();
    }

    @Test
    @DisplayName("Создание и подтверждение юридической информации партнёра в marketId")
    void testCreateAndConfirmPartnerLegalInfo() {
        GetOrCreateMarketIdRequest getOrCreateMarketIdRequest = GetOrCreateMarketIdRequest.newBuilder()
            .setUid(0L)
            .setPartnerId(PARTNER_ID)
            .setPartnerType(PARTNER_TYPE)
            .setLegalInfo(createMarketIdLegalInfo(LEGAL_FORM))
            .build();

        MarketAccount marketAccount = createMarketAccount();

        when(marketIdServiceBlockingStub.getOrCreateMarketId(eq(getOrCreateMarketIdRequest))).thenReturn(marketAccount);

        ConfirmLegalInfoRequest confirmLegalInfoRequest = createConfirmLegalInfoRequest();

        when(marketIdServiceBlockingStub.confirmLegalInfo(eq(confirmLegalInfoRequest))).thenReturn(marketAccount);

        Partner partner = createPartner();

        softly.assertThat(marketIdService.createAndConfirmPartnerLegalInfo(partner))
            .as("Asserting that marketAccount is valid")
            .isEqualTo(marketAccount);

        verify(marketIdServiceBlockingStub).getOrCreateMarketId(eq(getOrCreateMarketIdRequest));
        verify(marketIdServiceBlockingStub).confirmLegalInfo(eq(confirmLegalInfoRequest));
    }

    @Test
    @DisplayName("Успешное обогащение и подтверждение юридической информации партнёра в marketId")
    void testEnrichAndConfirmLegalInfoEnrichedAndSuccess() {
        testEnrichAndConfirmLegalInfo(true, true);
    }

    @Test
    @DisplayName("Обогащение и подтверждение юридической информации партнёра в marketId - нечего обогащать")
    void testEnrichAndConfirmLegalInfoNothingToEnrich() {
        testEnrichAndConfirmLegalInfo(false, false);
    }

    @Test
    @DisplayName("Неудачное обогащение и подтверждение юридической информации партнёра в marketId")
    void testEnrichAndConfirmLegalInfoFail() {
        testEnrichAndConfirmLegalInfo(true, false);
    }

    @Test
    @DisplayName("Связывание партнёра с существующим marketId")
    void testLinkMarketId() {
        LinkMarketIdRequest linkMarketIdRequest = LinkMarketIdRequest.newBuilder()
            .setPartnerId(PARTNER_ID)
            .setMarketId(MARKET_ID)
            .setPartnerType(PARTNER_TYPE)
            .setUid(0L)
            .build();

        MarketAccount marketAccount = createMarketAccount();

        when(marketIdServiceBlockingStub.linkMarketIdRequest(eq(linkMarketIdRequest))).thenReturn(marketAccount);

        softly.assertThat(marketIdService.linkMarketId(PARTNER_ID, MARKET_ID))
            .as("Asserting that marketAccount is valid")
            .isEqualTo(marketAccount);

        verify(marketIdServiceBlockingStub).linkMarketIdRequest(eq(linkMarketIdRequest));
    }

    private Optional<Long> testGetMarketId(boolean success) {
        GetByRegistrationNumberRequest getByRegistrationNumberRequest = GetByRegistrationNumberRequest.newBuilder()
            .setRegistrationNumber(String.valueOf(OGRN))
            .build();

        GetByRegistrationNumberResponse getByRegistrationNumberResponse = GetByRegistrationNumberResponse.newBuilder()
            .setSuccess(success)
            .setMarketId(MARKET_ID)
            .build();

        when(marketIdServiceBlockingStub.getByRegistrationNumber(eq(getByRegistrationNumberRequest)))
            .thenReturn(getByRegistrationNumberResponse);

        Optional<Long> marketId = marketIdService.getMarketId(OGRN);

        verify(marketIdServiceBlockingStub).getByRegistrationNumber(eq(getByRegistrationNumberRequest));

        return marketId;
    }

    private void testEnrichAndConfirmLegalInfo(boolean enrichedLegalForm, boolean success) {
        GetByIdRequest getByIdRequest = GetByIdRequest.newBuilder().setMarketId(MARKET_ID).build();

        MarketAccount marketAccount = createMarketAccount("");

        when(marketIdServiceBlockingStub.getById(eq(getByIdRequest))).thenReturn(marketAccount);

        String legalForm = enrichedLegalForm ? LEGAL_FORM : "";

        Partner partner = createPartner();
        partner.getLegalInfo().setLegalForm(legalForm);
        partner.onPreUpdate();

        ru.yandex.market.id.LegalInfo enrichedMarketIdLegalInfo = createMarketIdLegalInfo(legalForm);

        UpdateLegalInfoRequest updateLegalInfoRequest = UpdateLegalInfoRequest.newBuilder()
            .setMarketId(MARKET_ID)
            .setLegalInfo(enrichedMarketIdLegalInfo)
            .setTimestamp(Timestamp.newBuilder().setSeconds(partner.getUpdated().getSecond()).build())
            .build();

        UpdateLegalInfoResponse updateLegalInfoResponse = UpdateLegalInfoResponse.newBuilder()
            .setSuccess(success)
            .build();

        if (enrichedLegalForm) {
            when(marketIdServiceBlockingStub.updateLegalInfo(eq(updateLegalInfoRequest)))
                .thenReturn(updateLegalInfoResponse);
        }

        ConfirmLegalInfoRequest confirmLegalInfoRequest = createConfirmLegalInfoRequest();

        if (success) {
            when(marketIdServiceBlockingStub.confirmLegalInfo(eq(confirmLegalInfoRequest))).thenReturn(marketAccount);
        }

        marketIdService.enrichAndConfirmLegalInfo(MARKET_ID, partner);

        verify(marketIdServiceBlockingStub).getById(eq(getByIdRequest));

        if (enrichedLegalForm) {
            verify(marketIdServiceBlockingStub).updateLegalInfo(eq(updateLegalInfoRequest));
        }

        if (success) {
            verify(marketIdServiceBlockingStub).confirmLegalInfo(eq(confirmLegalInfoRequest));
        }
    }

    private Partner createPartner() {
        LegalInfo lmsLegalInfo = new LegalInfo()
            .setId(1L)
            .setOgrn(OGRN)
            .setInn(String.valueOf(INN))
            .setIncorporation(INCORPORATION)
            .setLegalForm(LEGAL_FORM);

        return new Partner()
            .setId(PARTNER_ID)
            .setName("Fulfillment without legal info")
            .setPartnerType(PartnerType.FULFILLMENT)
            .setMarketId(1L)
            .setLegalInfo(lmsLegalInfo);
    }

    private MarketAccount createMarketAccount() {
        return createMarketAccount(LEGAL_FORM);
    }

    private MarketAccount createMarketAccount(String legalForm) {
        return MarketAccount.newBuilder()
            .setMarketId(MARKET_ID)
            .setLegalInfo(createMarketIdLegalInfo(legalForm))
            .build();
    }

    private ru.yandex.market.id.LegalInfo createMarketIdLegalInfo(String legalForm) {
        return ru.yandex.market.id.LegalInfo.newBuilder()
            .setRegistrationNumber(String.valueOf(OGRN))
            .setInn(String.valueOf(INN))
            .setLegalName(INCORPORATION)
            .setType(legalForm)
            .build();
    }

    private ConfirmLegalInfoRequest createConfirmLegalInfoRequest() {
        return ConfirmLegalInfoRequest.newBuilder()
            .setMarketId(MARKET_ID)
            .addAllLegalInfoType(LEGAL_INFO_TYPES)
            .build();
    }
}

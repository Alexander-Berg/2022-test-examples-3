package ru.yandex.market.logistics.lom.service.validation;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.converter.marketId.LegalInfoConverter;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.Credentials;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.WaybillCredentialsValidatorAndEnricher;
import ru.yandex.market.logistics.lom.service.marketid.MarketIdService;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;

import static org.mockito.Mockito.mock;

@DisplayName("Валидация и обогащение юридической информации партнеров")
class WaybillCredentialsValidatorAndEnricherTest extends AbstractTest {
    private final MarketIdService marketIdService = mock(MarketIdService.class);
    private WaybillCredentialsValidatorAndEnricher validatorAndEnricher =
        new WaybillCredentialsValidatorAndEnricher(marketIdService, new LegalInfoConverter());

    @Test
    @DisplayName("В контексте нет партнеров")
    void contextWithNoPartners() {
        Order order = new Order().setWaybill(List.of(
            new WaybillSegment().setPartnerId(1L),
            new WaybillSegment().setPartnerId(2L)
        ));

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(
            order,
            new ValidateAndEnrichContext()
        );

        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(
            results.getOrderModifier().apply(order)
                .getWaybill().stream()
                .map(WaybillSegment::getPartnerInfo)
        ).containsOnlyNulls();
    }

    @Test
    @DisplayName("У партнера не указан marketId")
    void partnerWithNoMarketId() {
        ValidateAndEnrichContext context = new ValidateAndEnrichContext().setPartners(
            List.of(PartnerResponse.newBuilder().id(1L).build())
        );

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(new Order(), context);

        softly.assertThat(results.isValidationPassed()).isFalse();
        softly.assertThat(results.getErrorMessage()).isEqualTo("Partner id 1 has no marketId in LMS");
    }

    @Test
    @DisplayName("MarketId аккаунт не найден")
    void noMarketIdAccountFound() {
        ValidateAndEnrichContext context = new ValidateAndEnrichContext().setPartners(
            List.of(PartnerResponse.newBuilder().id(1L).marketId(2L).build())
        );

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(new Order(), context);

        softly.assertThat(results.isValidationPassed()).isFalse();
        softly.assertThat(results.getErrorMessage()).isEqualTo(
            "Failed to find sender legal info by marketId 2 for partner 1"
        );
    }

    @Test
    @DisplayName("MarketId аккаунт с пустыми полями")
    void invalidCredentialsFromMarketId() {
        Mockito.when(marketIdService.findAccountById(2L)).thenReturn(Optional.of(
            MarketAccount.newBuilder().setLegalInfo(LegalInfo.newBuilder().build()).build()
        ));

        ValidateAndEnrichContext context = new ValidateAndEnrichContext().setPartners(
            List.of(PartnerResponse.newBuilder().id(1L).marketId(2L).build())
        );

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(new Order(), context);

        softly.assertThat(results.isValidationPassed()).isFalse();
        softly.assertThat(results.getErrorMessage()).isEqualTo(
            "Invalid credentials for partner 1, url=https://url.stub, incorporation=null, legalForm=null"
        );
    }

    @Test
    @DisplayName("Успех валидации")
    void validationSucceeded() {
        Mockito.when(marketIdService.findAccountById(2L)).thenReturn(
            Optional.of(MarketAccount.newBuilder().setLegalInfo(
                LegalInfo.newBuilder().setType("ИП").setLegalName("Мятная мята").build()
            ).build())
        );

        Order order = new Order().setWaybill(List.of(new WaybillSegment().setPartnerId(1L)));

        ValidateAndEnrichContext context = new ValidateAndEnrichContext().setPartners(
            List.of(PartnerResponse.newBuilder().id(1L).marketId(2L).domain("мятная-мята.рф").build())
        );

        ValidateAndEnrichResults results = validatorAndEnricher.validateAndEnrich(new Order(), context);

        softly.assertThat(results.isValidationPassed()).isTrue();

        softly.assertThat(
            results.getOrderModifier().apply(order)
                .getWaybill().stream()
                .map(WaybillSegment::getPartnerInfo)
                .map(WaybillSegment.PartnerInfo::getCredentials)
        )
            .usingRecursiveFieldByFieldElementComparator()
            .contains(
                new Credentials()
                    .setName("Мятная мята")
                    .setIncorporation("ИП Мятная мята")
                    .setUrl("мятная-мята.рф")
                    .setLegalForm("IP")
                    .setOgrn("")
                    .setInn("")
                    .setAddress("")
            );


    }
}

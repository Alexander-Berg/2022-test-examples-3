package ru.yandex.market.logistics.lom.service.validation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.MarketIdModelFactory;
import ru.yandex.market.logistics.lom.converter.marketId.LegalInfoConverter;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.embedded.Credentials;
import ru.yandex.market.logistics.lom.entity.enums.CampaignType;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.entity.items.OrderItem;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.CredentialsValidatorAndEnricher;
import ru.yandex.market.logistics.lom.service.marketid.MarketIdService;
import ru.yandex.market.logistics.lom.service.partner.PartnerInfo;
import ru.yandex.market.logistics.lom.service.partner.PartnerLegalInfoService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Валидация и обогащение юридической информации")
class CredentialsValidatorAndEnricherTest extends AbstractTest {
    private final MarketIdService marketIdService = mock(MarketIdService.class);
    private final PartnerLegalInfoService partnerLegalInfoService = mock(PartnerLegalInfoService.class);
    private final CredentialsValidatorAndEnricher credentialsValidatorAndEnricher =
        new CredentialsValidatorAndEnricher(marketIdService, partnerLegalInfoService, new LegalInfoConverter());
    private final ValidateAndEnrichContext context = new ValidateAndEnrichContext();

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(marketIdService, partnerLegalInfoService);
    }

    @Test
    @DisplayName("MarketId аккаунт не найден")
    void marketIdAccountNotFound() {
        ValidateAndEnrichResults results = credentialsValidatorAndEnricher.validateAndEnrich(
            new Order().setMarketIdFrom(1L).setPlatformClient(PlatformClient.BERU),
            context
        );
        softly.assertThat(results.isValidationPassed()).isFalse();
        softly.assertThat(results.getErrorMessage()).isEqualTo("Failed to find sender legal info by market id 1");
        verify(marketIdService).findAccountById(1L);
    }

    @Test
    @DisplayName("Успешная валидация и обогащение заказа")
    void validationPassed() {
        Order order = new Order().setMarketIdFrom(1L).setPlatformClient(PlatformClient.BERU);
        when(marketIdService.findAccountById(1L))
            .thenReturn(Optional.of(MarketIdModelFactory.marketAccount(1L)));
        ValidateAndEnrichResults results = credentialsValidatorAndEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getMarketAccountFromLegalName()).isEqualTo("Рога и копыта");

        results.getOrderModifier().apply(order);
        softly.assertThat(order.getCredentials())
            .usingRecursiveComparison()
            .isEqualTo(
                new Credentials()
                    .setAddress("Блюхера 15")
                    .setIncorporation("ООО Рога и копыта")
                    .setInn("1231231234")
                    .setLegalForm("OOO")
                    .setName("Рога и копыта")
                    .setOgrn("555777")
                    .setUrl("https://delivery.yandex.ru")
            );
        verify(marketIdService).findAccountById(1L);
    }

    @Test
    @DisplayName("Успешная валидация и обогащение заказа - в заказе указан URL")
    void validationPassedWithUrl() {
        Order order = new Order()
            .setMarketIdFrom(1L)
            .setCredentials(new Credentials().setUrl("https://site.ru"))
            .setPlatformClient(PlatformClient.BERU);
        when(marketIdService.findAccountById(1L))
            .thenReturn(Optional.of(MarketIdModelFactory.marketAccount(1L)));
        ValidateAndEnrichResults results = credentialsValidatorAndEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getMarketAccountFromLegalName()).isEqualTo("Рога и копыта");

        results.getOrderModifier().apply(order);
        softly.assertThat(order.getCredentials())
            .usingRecursiveComparison()
            .isEqualTo(
                new Credentials()
                    .setAddress("Блюхера 15")
                    .setIncorporation("ООО Рога и копыта")
                    .setInn("1231231234")
                    .setLegalForm("OOO")
                    .setName("Рога и копыта")
                    .setOgrn("555777")
                    .setUrl("https://site.ru")
            );
        verify(marketIdService).findAccountById(1L);
    }

    @Test
    @DisplayName("Успешная валидация и обогащение товаров заказа")
    void itemsEnrichSuccess() {
        Order order = new Order().setMarketIdFrom(1L).setPlatformClient(PlatformClient.BERU);
        order.setItems(List.of(new OrderItem().setVendorId(1L), new OrderItem().setVendorId(1L)));

        MarketAccount account = MarketIdModelFactory.marketAccount(1L);
        when(marketIdService.findAccountById(1L))
            .thenReturn(Optional.of(account));
        when(marketIdService.findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER))
            .thenReturn(Optional.of(account));
        mockPartnerLegalInfoService();

        ValidateAndEnrichResults results = credentialsValidatorAndEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getMarketAccountFromLegalName()).isEqualTo("Рога и копыта");

        results.getOrderModifier().apply(order);
        order.getItems().stream()
            .filter(item -> item.getVendorId() == 1L)
            .forEach(this::assertOrderItem);

        verify(marketIdService).findAccountById(1L);
        verify(marketIdService).findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER);
        verify(partnerLegalInfoService).getOptionalPartnerLegalInfoExternal(1L, Set.of(CampaignType.SUPPLIER));
    }

    @Test
    @DisplayName("Успешная валидация и обогащение товаров заказа Daas")
    void itemsEnrichSuccessDaas() {
        Order order = new Order().setMarketIdFrom(1L).setPlatformClient(PlatformClient.YANDEX_DELIVERY);
        order.setItems(List.of(new OrderItem().setVendorId(1L), new OrderItem().setVendorId(1L)));

        MarketAccount account = MarketIdModelFactory.marketAccount(1L);
        when(marketIdService.findAccountById(1L))
            .thenReturn(Optional.of(account));
        mockPartnerLegalInfoService();

        ValidateAndEnrichResults results = credentialsValidatorAndEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getMarketAccountFromLegalName()).isEqualTo("Рога и копыта");

        results.getOrderModifier().apply(order);
        order.getItems().stream()
            .filter(item -> item.getVendorId() == 1L)
            .forEach(this::assertOrderItem);

        verify(marketIdService).findAccountById(1L);
        verify(partnerLegalInfoService).getOptionalPartnerLegalInfoExternal(1L, Set.of(CampaignType.SUPPLIER));
    }

    @Test
    @DisplayName("Успешная валидация и обогащение товаров заказа - информация о поставщике уже заполнена")
    void itemsEnrichSuccessWithInnInItem() {
        Order order = new Order().setMarketIdFrom(1L).setPlatformClient(PlatformClient.BERU);
        order.setItems(
            List.of(
                new OrderItem()
                    .setVendorId(1L)
                    .setSupplierName("some-name")
                    .setSupplierPhone("some-phone")
                    .setSupplierInn("some-inn")
            )
        );

        MarketAccount account = MarketIdModelFactory.marketAccount(1L);
        when(marketIdService.findAccountById(1L))
            .thenReturn(Optional.of(account));
        when(marketIdService.findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER))
            .thenReturn(Optional.of(account));
        mockPartnerLegalInfoService();

        ValidateAndEnrichResults results = credentialsValidatorAndEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getMarketAccountFromLegalName()).isEqualTo("Рога и копыта");

        results.getOrderModifier().apply(order);
        order.getItems().stream()
            .filter(item -> item.getVendorId() == 1L)
            .forEach(item -> assertOrderItem(item, "some-inn", "some-name", "some-phone"));

        verify(marketIdService).findAccountById(1L);
        verify(marketIdService).findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER);
        verify(partnerLegalInfoService).getOptionalPartnerLegalInfoExternal(1L, Set.of(CampaignType.SUPPLIER));
    }

    @Test
    @DisplayName("Неудачное обогащение товара")
    void itemsNotEnriched() {
        Order order = new Order().setMarketIdFrom(1L).setPlatformClient(PlatformClient.BERU);
        order.setItems(List.of(new OrderItem().setVendorId(2L)));

        MarketAccount account = MarketIdModelFactory.marketAccount(1L);
        when(marketIdService.findAccountById(1L))
            .thenReturn(Optional.of(account));
        when(marketIdService.findAccountByPartnerIdAndPartnerType(2L, CampaignType.SUPPLIER))
            .thenReturn(Optional.empty());
        when(partnerLegalInfoService.getOptionalPartnerLegalInfoExternal(2L, Set.of(CampaignType.SUPPLIER)))
            .thenReturn(Optional.empty());

        ValidateAndEnrichResults results = credentialsValidatorAndEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getMarketAccountFromLegalName()).isEqualTo("Рога и копыта");

        results.getOrderModifier().apply(order);

        LegalInfo defaultLegalInfo = LegalInfo.getDefaultInstance();
        order.getItems()
            .forEach(item -> assertOrderItem(
                item,
                defaultLegalInfo.getInn(),
                defaultLegalInfo.getLegalName(),
                null
            ));

        verify(marketIdService).findAccountById(1L);
        verify(marketIdService).findAccountByPartnerIdAndPartnerType(2L, CampaignType.SUPPLIER);
        verify(partnerLegalInfoService).getOptionalPartnerLegalInfoExternal(2L, Set.of(CampaignType.SUPPLIER));
    }

    @Test
    @DisplayName("У партнера не проставлен телефон")
    void partnerWithoutPhone() {
        Order order = new Order().setMarketIdFrom(1L).setPlatformClient(PlatformClient.BERU);
        order.setItems(List.of(new OrderItem().setVendorId(1L)));

        MarketAccount account = MarketIdModelFactory.marketAccount(1L);
        when(marketIdService.findAccountById(1L))
            .thenReturn(Optional.of(account));
        when(marketIdService.findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER))
            .thenReturn(Optional.of(account));
        when(partnerLegalInfoService.getOptionalPartnerLegalInfoExternal(1L, Set.of(CampaignType.SUPPLIER)))
            .thenReturn(Optional.of(new PartnerInfo()));

        ValidateAndEnrichResults results = credentialsValidatorAndEnricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(context.getMarketAccountFromLegalName()).isEqualTo("Рога и копыта");

        results.getOrderModifier().apply(order);
        order.getItems().stream()
            .filter(item -> item.getVendorId() == 1L)
            .forEach(item -> assertOrderItem(item, "1231231234", "Рога и копыта", null));

        verify(marketIdService).findAccountById(1L);
        verify(marketIdService).findAccountByPartnerIdAndPartnerType(1L, CampaignType.SUPPLIER);
        verify(partnerLegalInfoService).getOptionalPartnerLegalInfoExternal(1L, Set.of(CampaignType.SUPPLIER));
    }

    private void mockPartnerLegalInfoService() {
        when(partnerLegalInfoService.getOptionalPartnerLegalInfoExternal(1L, Set.of(CampaignType.SUPPLIER)))
            .thenReturn(Optional.of(new PartnerInfo().setPhone("+79876543210")));
    }

    private void assertOrderItem(OrderItem item) {
        assertOrderItem(item, "1231231234", "Рога и копыта", "+79876543210");
    }

    private void assertOrderItem(OrderItem item, String inn, String name, String phone) {
        softly.assertThat(item.getSupplierInn()).isEqualTo(inn);
        softly.assertThat(item.getSupplierName()).isEqualTo(name);
        softly.assertThat(item.getSupplierPhone()).isEqualTo(phone);
    }
}

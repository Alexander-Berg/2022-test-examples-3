package ru.yandex.direct.intapi.entity.auction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.bids.interpolator.CapFactory;
import ru.yandex.direct.core.entity.bids.interpolator.InterpolatorService;
import ru.yandex.direct.core.entity.currency.model.CurrencyRate;
import ru.yandex.direct.core.entity.currency.repository.CurrencyRateRepository;
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.auction.model.PhraseAuctionItem;
import ru.yandex.direct.intapi.entity.auction.service.InterpolateAuctionItemsService;
import ru.yandex.direct.intapi.validation.IntApiDefect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.intapi.validation.ValidationUtils.getErrorText;

@IntApiTest
public class InterpolateAuctionItemsServiceValidationTest {
    private InterpolateAuctionItemsService interpolateAuctionItemsService;

    public InterpolateAuctionItemsServiceValidationTest() {
        CurrencyRateRepository currencyRateRepository = mock(CurrencyRateRepository.class);
        when(currencyRateRepository.getCurrencyRate(eq(CurrencyCode.USD), any()))
                .thenAnswer(invocation -> {
                    LocalDate date = (LocalDate) invocation.getArguments()[1];
                    return new CurrencyRate()
                            .withCurrencyCode(CurrencyCode.USD)
                            .withDate(date)
                            .withRate(BigDecimal.valueOf(60));
                });
        CapFactory capFactory = new CapFactory();
        CurrencyRateService currencyRateService = new CurrencyRateService(currencyRateRepository);
        InterpolatorService interpolatorService = new InterpolatorService(currencyRateService, capFactory);
        interpolateAuctionItemsService = new InterpolateAuctionItemsService(interpolatorService);
    }

    @Test
    public void nullPhraseAuctionItems() {
        ValidationResult<List<PhraseAuctionItem>, IntApiDefect> vr =
                interpolateAuctionItemsService.validatePhraseAuctionItems(null);
        String validationError = getErrorText(vr);
        assertThat(validationError, equalTo("Request body must be specified"));
    }

    @Test
    public void nullAuctionItems() {
        ValidationResult<List<PhraseAuctionItem>, IntApiDefect> vr =
                interpolateAuctionItemsService.validatePhraseAuctionItems(
                        Collections.singletonList(new PhraseAuctionItem().withCurrencyCode(CurrencyCode.USD)));
        String validationError = getErrorText(vr);
        assertThat(validationError, equalTo("[0].auction_items cannot be null"));
    }

    @Test
    public void nullCurrencyCode() {
        ValidationResult<List<PhraseAuctionItem>, IntApiDefect> vr =
                interpolateAuctionItemsService.validatePhraseAuctionItems(Collections.singletonList(
                        new PhraseAuctionItem().withAuctionItems(Collections.emptyList())));
        String validationError = getErrorText(vr);
        assertThat(validationError, equalTo("[0].currency_code cannot be null"));
    }
}

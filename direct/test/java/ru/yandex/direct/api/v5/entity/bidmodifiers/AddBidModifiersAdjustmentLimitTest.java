package ru.yandex.direct.api.v5.entity.bidmodifiers;

import java.util.Collections;

import com.yandex.direct.api.v5.bidmodifiers.AddRequest;
import com.yandex.direct.api.v5.bidmodifiers.BidModifierAddItem;
import com.yandex.direct.api.v5.bidmodifiers.MobileAdjustmentAdd;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.AddBidModifiersDelegate;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.api.v5.validation.Matchers;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.BidModifiersDefectTypes.addItemsLimitExceeded;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.Constants.BID_MODIFIERS_ADD_ITEMS_LIMIT;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class AddBidModifiersAdjustmentLimitTest {
    @Mock
    private BidModifierService bidModifierService;
    @Mock
    private ResultConverter resultConverter;
    @Mock
    private ApiAuthenticationSource apiAuthenticationSource;
    @InjectMocks
    private AddBidModifiersDelegate bidModifiersDelegate;

    @Before
    public void before() {
        initMocks(this);
    }

    @Test
    @Description("Количество корректировок ставок в запросе к API превышает максимальное")
    public void overlimitBidModifiersInOneRequestTest() {
        ValidationResult<AddRequest, DefectType> result = bidModifiersDelegate.validateRequestCore(
                new AddRequest().withBidModifiers(
                        Collections.nCopies(BID_MODIFIERS_ADD_ITEMS_LIMIT + 1,
                                new BidModifierAddItem().withCampaignId(123L)
                                        .withMobileAdjustment(new MobileAdjustmentAdd().withBidModifier(110))
                        )));
        assertThat(result).is(matchedBy(hasDefectWith(
                Matchers.validationError(path(field("BidModifiers")),
                        addItemsLimitExceeded(BID_MODIFIERS_ADD_ITEMS_LIMIT)))));
    }
}

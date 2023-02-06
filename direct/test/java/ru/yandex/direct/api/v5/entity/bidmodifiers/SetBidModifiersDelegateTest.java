package ru.yandex.direct.api.v5.entity.bidmodifiers;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.bidmodifiers.BidModifierSetItem;
import com.yandex.direct.api.v5.bidmodifiers.SetRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.SetBidModifiersDelegate;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.Constants.BID_MODIFIERS_SET_ITEMS_LIMIT;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.DEFAULT_PERCENT;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.getModelChangesForUpdate;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class SetBidModifiersDelegateTest {
    private static final long ZERO_ID = 0L;
    private static final long NEGATIVE_ID = -1L;

    @Autowired
    private SetBidModifiersDelegate bidModifiersDelegate;

    @Before
    public void before() {
        initMocks(this);
    }

    @Test
    @Description("Количество корректировок ставок в запросе к API превышает максимальное")
    public void overlimitBidModifiersInOneRequestTest() {
        ValidationResult<SetRequest, DefectType> vr = bidModifiersDelegate.validateRequest(
                new SetRequest().withBidModifiers(
                        Collections.nCopies(BID_MODIFIERS_SET_ITEMS_LIMIT + 1,
                                new BidModifierSetItem().withId(123L).withBidModifier(110)))
        );

        assertThat(vr).is(matchedBy(hasDefectWith(validationError(9300))));
    }

    @Test
    @Description("Количество корректировок ставок в запросе к API равно максимальному")
    public void maxBidModifiersInOneRequestTest() {
        ValidationResult<SetRequest, DefectType> vr = bidModifiersDelegate.validateRequest(
                new SetRequest().withBidModifiers(
                        Collections.nCopies(BID_MODIFIERS_SET_ITEMS_LIMIT,
                                new BidModifierSetItem().withId(123L).withBidModifier(110)))
        );

        assertFalse(vr.hasAnyErrors());
    }

    @Test
    @Description("Нулевой ID корректировки")
    public void zeroIdTest() {
        ModelChanges<BidModifierAdjustment> modelChanges = getModelChangesForUpdate(BidModifierMobileAdjustment.class,
                ZERO_ID, DEFAULT_PERCENT);

        ValidationResult<List<ModelChanges<BidModifierAdjustment>>, DefectType> vr =
                bidModifiersDelegate.validateInternalRequest(Collections.singletonList(modelChanges));

        assertThat(vr).is(matchedBy(hasDefectWith(validationError(8800))));
    }

    @Test
    @Description("Отрицательный ID корректировки")
    public void negativeIdTest() {
        ModelChanges<BidModifierAdjustment> modelChanges = getModelChangesForUpdate(BidModifierMobileAdjustment.class,
                NEGATIVE_ID, DEFAULT_PERCENT);

        ValidationResult<List<ModelChanges<BidModifierAdjustment>>, DefectType> vr =
                bidModifiersDelegate.validateInternalRequest(Collections.singletonList(modelChanges));

        assertThat(vr).is(matchedBy(hasDefectWith(validationError(5005))));
    }
}

package ru.yandex.direct.api.v5.entity.bidmodifiers;

import java.util.Collections;

import com.yandex.direct.api.v5.bidmodifiers.DeleteRequest;
import com.yandex.direct.api.v5.general.IdsCriteria;
import org.apache.commons.lang3.RandomUtils;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;

import ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.DeleteBidModifiersDelegate;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class DeleteBidModifiersLimitTest {
    @InjectMocks
    private DeleteBidModifiersDelegate bidModifiersDelegate;

    @Before
    public void before() {
        initMocks(this);
    }

    @Test
    @Description("Количество корректировок ставок в запросе к API превышает максимальное")
    public void overlimitBidModifiersInOneRequestTest() {
        ValidationResult<DeleteRequest, DefectType> vr = bidModifiersDelegate.validateRequest(
                new DeleteRequest().withSelectionCriteria(new IdsCriteria().withIds(
                        Collections.nCopies(Constants.BID_MODIFIERS_DELETE_ITEMS_LIMIT + 1,
                                RandomUtils.nextLong(1, Integer.MAX_VALUE)))));

        MatcherAssert.assertThat(vr,
                hasDefectWith(validationError(path(field("SelectionCriteria"), field("Ids")), 4001)));
    }

    @Test
    @Description("Количество корректировок ставок в запросе к API равно максимальному")
    public void maxBidModifiersInOneRequestTest() {
        ValidationResult<DeleteRequest, DefectType> vr = bidModifiersDelegate.validateRequest(
                new DeleteRequest().withSelectionCriteria(new IdsCriteria().withIds(
                        Collections.nCopies(Constants.BID_MODIFIERS_DELETE_ITEMS_LIMIT,
                                RandomUtils.nextLong(1, Integer.MAX_VALUE)))));

        assertThat(vr, hasNoDefects());
    }
}

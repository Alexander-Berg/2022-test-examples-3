package ru.yandex.chemodan.app.psbilling.web.converter;

import java.util.function.Function;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.converter.ToPromoPayloadConverter;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.promocodes.model.PromoCodeData;
import ru.yandex.chemodan.app.psbilling.core.promos.groups.AbstractGroupPromoTemplate;
import ru.yandex.chemodan.app.psbilling.core.texts.TextsManager;
import ru.yandex.chemodan.app.psbilling.web.model.PromoPojo;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class ToGroupPromoConverterTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    private ToGroupPromoConverter converter;

    @Mock
    private Function<ToPromoPayloadConverter.ConvertData, Option<String>> toPayload;
    @Mock
    private TextsManager textsManager;

    @Before
    public void setUp() throws Exception {
        this.converter = new ToGroupPromoConverter(this.toPayload, this.textsManager);
    }

    @Test
    public void applyEmpty() {
        Option<PassportUid> uidO = Option.of(PassportUid.MAX_VALUE);
        Option<Group> groupO = Option.of(Mockito.mock(Group.class));
        Option<AbstractGroupPromoTemplate> promoO = Option.empty();
        String language = "RU";
        Option<PromoCodeData> promoCode = Option.empty();
        Option<String> payloadType = Option.empty();
        Option<Integer> payloadVersion = Option.of(1);

        Option<PromoPojo> actual = converter.apply(
                ToGroupPromoConverter.ConvertData.cons(
                        uidO,
                        groupO,
                        promoO,
                        language,
                        promoCode,
                        payloadType,
                        payloadVersion
                )
        );

        Assert.none(actual);
    }

    @Test
    public void apply() {
        Option<PassportUid> uidO = Option.of(PassportUid.MAX_VALUE);
        Option<Group> groupO = Option.of(Mockito.mock(Group.class));

        AbstractGroupPromoTemplate promo = Mockito.mock(AbstractGroupPromoTemplate.class);
        Mockito.when(promo.getCode()).thenReturn("Этот код");
        Mockito.when(promo.getPromoNameTankerKey()).thenReturn(Option.empty());
        Mockito.when(promo.getToDate()).thenReturn(Option.empty());
        Instant expectedTime = Instant.now();
        Mockito.when(promo.canBeUsedUntilDate(groupO)).thenReturn(Option.of(expectedTime));

        PromoCodeData promoCodeData = Mockito.mock(PromoCodeData.class);
        Mockito.when(promoCodeData.getToDate()).thenReturn(Option.empty());

        Option<AbstractGroupPromoTemplate> promoO = Option.of(promo);
        String language = "RU";
        Option<PromoCodeData> promoCode = Option.of(promoCodeData);
        Option<String> payloadType = Option.of("Скоро получишь ответы на свои вопросы");
        Option<Integer> payloadVersion = Option.of(1);

        Mockito.when(toPayload.apply(Mockito.any())).thenReturn(Option.empty());

        PromoPojo actual = converter.apply(
                ToGroupPromoConverter.ConvertData.cons(
                        uidO,
                        groupO,
                        promoO,
                        language,
                        Option.empty(),
                        payloadType,
                        payloadVersion
                )
        ).get();

        Assert.equals(promo.getCode(), actual.getKey());
        Assert.none(actual.getTitle());
        Assert.some(expectedTime, actual.getAvailableUntil());
        Assert.none(actual.getPayload());

        PromoPojo actual2 = converter.apply(
                ToGroupPromoConverter.ConvertData.cons(
                        uidO,
                        Option.empty(),
                        promoO,
                        language,
                        promoCode,
                        payloadType,
                        payloadVersion
                )
        ).get();

        Assert.equals(promo.getCode(), actual2.getKey());
        Assert.none(actual2.getTitle());
        Assert.none(actual2.getAvailableUntil());
        Assert.none(actual2.getPayload());


        Instant expectedTime2 = Instant.now().plus(41352);
        Mockito.when(promo.getToDate()).thenReturn(Option.of(expectedTime2));

        PromoPojo actual3 = converter.apply(
                ToGroupPromoConverter.ConvertData.cons(
                        uidO,
                        Option.empty(),
                        promoO,
                        language,
                        promoCode,
                        payloadType,
                        payloadVersion
                )
        ).get();

        Assert.equals(promo.getCode(), actual3.getKey());
        Assert.none(actual3.getTitle());
        Assert.some(expectedTime2, actual3.getAvailableUntil());
        Assert.none(actual3.getPayload());

        Instant expectedTime3 = Instant.now().plus(123);
        Mockito.when(promoCodeData.getToDate()).thenReturn(Option.of(expectedTime3));

        PromoPojo actual4 = converter.apply(
                ToGroupPromoConverter.ConvertData.cons(
                        uidO,
                        Option.empty(),
                        promoO,
                        language,
                        promoCode,
                        payloadType,
                        payloadVersion
                )
        ).get();

        Assert.equals(promo.getCode(), actual4.getKey());
        Assert.none(actual4.getTitle());
        Assert.some(expectedTime3, actual4.getAvailableUntil());
        Assert.none(actual4.getPayload());
    }
}

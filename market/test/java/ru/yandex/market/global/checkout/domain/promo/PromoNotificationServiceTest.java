package ru.yandex.market.global.checkout.domain.promo;

import java.util.Map;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.promo.apply.fixed_discount.FixedDiscountArgs;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.domain.push.model.PushTankerKeyField;
import ru.yandex.market.global.checkout.domain.push.model.localizable_field.key.GlobalNotificationTextTankerKey;
import ru.yandex.market.global.checkout.domain.push.model.localizable_field.key.GlobalNotificationTitleTankerKey;
import ru.yandex.market.global.checkout.domain.queue.task.notification.SendPushPayload;
import ru.yandex.market.global.checkout.domain.queue.task.notification.SendPushProducer;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoCommunicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.mj.generated.server.model.PromoCommunicationArgsDto;

import static org.mockito.Mockito.verify;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PromoNotificationServiceTest extends BaseFunctionalTest {

    private static final EnhancedRandom RANDOM = RandomDataGenerator
            .dataRandom(PromoNotificationServiceTest.class).build();

    @InjectMocks
    private final PromoNotificationService promoNotificationService;

    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private SendPushProducer sendPushProducer;

    private final TestPromoFactory testPromoFactory;
    private final TestOrderFactory testOrderFactory;


    private SendPushPayload callAndReturnValue(Promo promo, long uid, PromoNotificationRecipient recipient) {

        final ArgumentCaptor<SendPushPayload> captor = ArgumentCaptor.forClass(SendPushPayload.class);

        promoNotificationService.pushNotification(promo, uid, recipient);
        verify(sendPushProducer).enqueueNonSleepTime(captor.capture());

        return captor.getValue();
    }

    @Test
    public void testValidByUID() {
        Long uid = RANDOM.nextObject(Long.class);
        testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(it -> it.setUid(uid).setLocale("ru")).build());

        Promo fixedDiscount = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setAccessType(EPromoAccessType.ISSUED)
                )
                .setupCommunicationTypes(it -> new EPromoCommunicationType[]{EPromoCommunicationType.PUSH})
                .setupCommunicationArgs(it -> new PromoCommunicationArgsDto()
                        .push(PromoUtil.createDefaultIssuedPushCommunication()))
                .build()
        );

        SendPushPayload payload = this.callAndReturnValue(fixedDiscount, uid, PromoUtil.createRecipientByUID(uid));
        Assertions.assertThat(payload).usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build())
                .isEqualTo(new SendPushPayload()
                        .setLocale("ru")
                        .setUserId(String.valueOf(uid))
                        .setUserIdType(SendPushPayload.SendPushUserIdentifierType.YANDEX_UID)
                        .setTitleTankerKey(new PushTankerKeyField(
                                GlobalNotificationTitleTankerKey.PROMO_ISSUED_DEFAULT_TITLE.getKeysetAlias(),
                                GlobalNotificationTitleTankerKey.PROMO_ISSUED_DEFAULT_TITLE.getKey(),
                                null
                        ))
                        .setTextTankerKey(new PushTankerKeyField(
                                GlobalNotificationTextTankerKey.PROMO_ISSUED_DEFAULT_TEXT.getKeysetAlias(),
                                GlobalNotificationTextTankerKey.PROMO_ISSUED_DEFAULT_TEXT.getKey(),
                                null

                        ))
                );
        Assertions.assertThat(payload.getTextTankerKey().getParams()).containsAllEntriesOf(Map.of(
                "promo_name", fixedDiscount.getName(),
                "discount", ((FixedDiscountArgs) fixedDiscount.getArgs()).getDiscount(),
                "minimum_cost", ((FixedDiscountArgs) fixedDiscount.getArgs()).getMinTotalItemsCost()));
    }

    @Test
    public void testValidByGoUserId() {
        Long uid = RANDOM.nextObject(Long.class);
        String goUserId = RANDOM.nextObject(String.class);
        testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(it -> it.setUid(uid)
                        .setLocale("ru")
                        .setYaTaxiUserId(goUserId)).build());

        Promo fixedDiscount = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setAccessType(EPromoAccessType.ISSUED)
                )
                .setupCommunicationTypes(it -> new EPromoCommunicationType[]{EPromoCommunicationType.PUSH})
                .setupCommunicationArgs(it -> new PromoCommunicationArgsDto()
                        .push(PromoUtil.createDefaultIssuedPushCommunication()))
                .build()
        );

        SendPushPayload payload = this.callAndReturnValue(fixedDiscount, uid,
                PromoUtil.createRecipientByGoUserId(goUserId));
        Assertions.assertThat(payload).usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build())
                .isEqualTo(new SendPushPayload()
                        .setUserId(goUserId)
                        .setUserIdType(SendPushPayload.SendPushUserIdentifierType.GO_USER_ID)
                );
    }

}

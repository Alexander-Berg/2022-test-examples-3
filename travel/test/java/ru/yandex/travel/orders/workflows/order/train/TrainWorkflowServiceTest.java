package ru.yandex.travel.orders.workflows.order.train;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.javamoney.moneta.Money;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.commons.proto.EPromoCodeNominalType;
import ru.yandex.travel.orders.entities.TrainOrder;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.entities.promo.DiscountApplicationConfig;
import ru.yandex.travel.orders.entities.promo.PromoAction;
import ru.yandex.travel.orders.entities.promo.PromoCodeGenerationType;
import ru.yandex.travel.orders.entities.promo.SimplePromoCodeGenerationConfig;
import ru.yandex.travel.orders.entities.promo.ValidTillGenerationType;
import ru.yandex.travel.orders.repository.promo.PromoActionRepository;
import ru.yandex.travel.train.model.TrainReservation;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "train-workflow.generate-personal-promo-codes=true",
                "train-workflow.promo-action-for-hotels-name=train_success_for_hotels_202102"
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
public class TrainWorkflowServiceTest {

    @Autowired
    private TrainWorkflowService trainWorkflowService;

    @Autowired
    private PromoActionRepository promoActionRepository;

    @Test
    @Transactional
    public void testGeneratePromoCodeForHotels() {
        TrainOrder order = new TrainOrder();
        order.setId(UUID.randomUUID());
        TrainOrderItem orderItem = new TrainOrderItem();
        orderItem.setId(UUID.randomUUID());
        orderItem.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        orderItem.setReservation(new TrainReservation());
        order.addOrderItem(orderItem);

        PromoAction promoAction = new PromoAction();
        promoAction.setId(UUID.randomUUID());
        promoAction.setName("train_success_for_hotels_202102");
        promoAction.setValidTill(Instant.now().plus(10, ChronoUnit.DAYS));
        promoAction.setValidFrom(Instant.now());
        promoAction.setDiscountApplicationConfig(new DiscountApplicationConfig());
        promoAction.getDiscountApplicationConfig().setMinTotalCost(Money.of(4000, ProtoCurrencyUnit.RUB));
        promoAction.getDiscountApplicationConfig().setAddsUpWithOtherActions(false);
        promoAction.setPromoCodeGenerationType(PromoCodeGenerationType.SIMPLE_GENERATION);

        var generationConfig = new SimplePromoCodeGenerationConfig();
        generationConfig.setPrefix("YA");
        generationConfig.setSuffix("COOL");
        generationConfig.setNominal(200.0);
        generationConfig.setNominalType(EPromoCodeNominalType.NT_VALUE);
        generationConfig.setMaxUsagePerUser(1);
        generationConfig.setMaxActivations(1);
        generationConfig.setValidTillGenerationType(ValidTillGenerationType.FIXED_DURATION);
        generationConfig.setFixedDaysDuration(20L);

        promoAction.setPromoCodeGenerationConfig(generationConfig);
        promoActionRepository.saveAndFlush(promoAction);

        trainWorkflowService.createPromoCodeForHotels(order);

        Assertions.assertThat(order.getGeneratedPromoCodes()).isNotNull();
        Assertions.assertThat(order.getGeneratedPromoCodes().getPromoCodes().size() > 0);
    }
}

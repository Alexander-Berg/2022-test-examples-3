package ru.yandex.market.checkout;

import java.io.IOException;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.balance.model.BalanceClientResult;
import ru.yandex.market.checkout.checkouter.balance.trust.model.CreateBasketRequest;
import ru.yandex.market.checkout.checkouter.lock.YtLock;
import ru.yandex.market.checkout.checkouter.order.JsonInstance;
import ru.yandex.market.checkout.checkouter.order.SubstatusProvider;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableBuyer;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableDelivery;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableItem;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.pay.builders.PrepayPaymentBuilder;
import ru.yandex.market.checkout.checkouter.persey.model.EstimateOrderDonationRequest;
import ru.yandex.market.checkout.checkouter.returns.domain.ReturnIdempotentKey;
import ru.yandex.market.checkout.checkouter.sberbank.model.AdditionalJsonParams;
import ru.yandex.market.checkout.checkouter.service.business.LoyaltyContext;
import ru.yandex.market.checkout.checkouter.service.personal.model.FullName;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersGps;

import static ru.yandex.market.checkout.util.ToStringChecker.checkToStringInSameModule;
import static ru.yandex.market.checkout.util.ToStringChecker.excludeByClasses;
import static ru.yandex.market.checkout.util.ToStringChecker.excludeByField;
import static ru.yandex.market.checkout.util.ToStringChecker.excludeByPackages;

public class CheckToStringTest {

    @Test
    public void checkToStringInCheckouterModule() throws IOException {
        checkToStringInSameModule(
                excludeByPackages(
                        "ru.yandex.market.checkout.checkouter.storage.jooq",
                        "ru.yandex.market.checkout.checkouter.tasks.eventinspector",
                        "ru.yandex.market.checkout.checkouter.storage.util"
                ),
                excludeByClasses(
                        FullName.class,
                        PersAddress.class,
                        PersGps.class,
                        JsonInstance.class,
                        PrepayPaymentBuilder.class,
                        LoyaltyContext.class,
                        MultiCartContext.class,
                        SubstatusProvider.class,
                        // в этом классе есть "служебная" мапа, которую не хочется распечатывать
                        EstimateOrderDonationRequest.class,
                        ImmutableDelivery.class,
                        YtLock.class,
                        ImmutableDelivery.class,
                        ImmutableOrder.class,
                        ImmutableBuyer.class,
                        ImmutableItem.class,
                        ReturnIdempotentKey.class
                ),
                excludeByField(
                        Pair.of(BalanceClientResult.class, "name"),
                        Pair.of(BalanceClientResult.class, "email"),
                        Pair.of(BalanceClientResult.class, "phone"),
                        Pair.of(CreateBasketRequest.class, "userEmail"),
                        Pair.of(AdditionalJsonParams.class, "email"),
                        Pair.of(AdditionalJsonParams.class, "phone")
                )
        );
    }
}


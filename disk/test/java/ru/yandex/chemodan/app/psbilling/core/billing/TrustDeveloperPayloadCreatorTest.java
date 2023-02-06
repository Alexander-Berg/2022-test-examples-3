package ru.yandex.chemodan.app.psbilling.core.billing;

import java.math.BigDecimal;

import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.products.TrialDefinition;
import ru.yandex.chemodan.app.psbilling.core.products.UserProduct;
import ru.yandex.chemodan.app.psbilling.core.texts.TextsManager;
import ru.yandex.misc.test.Assert;

public class TrustDeveloperPayloadCreatorTest {

    private TrustDeveloperPayloadCreator trustDeveloperPayloadCreator;

    @Mock
    private TextsManager textsManager;

    @Before
    public void setUp() throws Exception {
        this.trustDeveloperPayloadCreator = new TrustDeveloperPayloadCreator(textsManager);
    }

    @Test
    public void paymentBuildPayloadTrialNotFount() {
        UserProduct product = Mockito.mock(UserProduct.class);
        Mockito.when(product.getTrialDefinition()).thenReturn(Option.empty());

        Assert.isTrue(trustDeveloperPayloadCreator.createCreatePaymentPayload(false, product).isEmpty());

        val actual = trustDeveloperPayloadCreator.createCreatePaymentPayload(true, product);

        Assert.hasSize(1, actual);
        Assert.equals("checkout_tuning", actual.get("template"));
    }

    @Test
    public void paymentBuildPayloadWithTrialNonZero() {
        UserProduct product = Mockito.mock(UserProduct.class);
        TrialDefinition trial = Mockito.mock(TrialDefinition.class);
        Mockito.when(product.getTrialDefinition()).thenReturn(Option.of(trial));
        Mockito.when(trial.getPrice()).thenReturn(BigDecimal.ONE);

        Assert.isTrue(trustDeveloperPayloadCreator.createCreatePaymentPayload(false, product).isEmpty());

        val actual = trustDeveloperPayloadCreator.createCreatePaymentPayload(true, product);

        Assert.hasSize(1, actual);
        Assert.equals("checkout_tuning", actual.get("template"));
    }

    @Test
    public void paymentBuildPayloadWithTrialZero() {
        UserProduct product = Mockito.mock(UserProduct.class);
        TrialDefinition trial = Mockito.mock(TrialDefinition.class);
        Mockito.when(product.getTrialDefinition()).thenReturn(Option.of(trial));
        Mockito.when(trial.getPrice()).thenReturn(BigDecimal.ZERO);

        val actual = trustDeveloperPayloadCreator.createCreatePaymentPayload(false, product);

        Assert.hasSize(1, actual);
        Assert.equals(Cf.map("amount", false), actual.get("blocks_visibility"));

        val actualNew = trustDeveloperPayloadCreator.createCreatePaymentPayload(true, product);

        Assert.hasSize(2, actualNew);
        Assert.equals(Cf.map("amount", false), actualNew.get("blocks_visibility"));
        Assert.equals("checkout_tuning", actualNew.get("template"));
    }
}

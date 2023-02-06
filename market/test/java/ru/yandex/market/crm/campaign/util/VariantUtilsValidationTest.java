package ru.yandex.market.crm.campaign.util;

import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.crm.campaign.domain.actions.ActionVariant;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.GncSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.variants.AbstractVariant;
import ru.yandex.market.crm.campaign.domain.variants.HasVariants;

import static java.util.Arrays.asList;

@RunWith(Parameterized.class)
public class VariantUtilsValidationTest {
    @Parameterized.Parameter(0)
    public boolean isValid;
    @Parameterized.Parameter(1)
    public List<? super AbstractVariant<?>> variants;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static class ConcreteConf<T extends AbstractVariant<T>> implements HasVariants<T> {
        private List<T> variants;

        private ConcreteConf(List<T> variants) {
            this.variants = variants;
        }

        @Override
        public List<T> getVariants() {
            return variants;
        }

        @Override
        public void setVariants(List<T> variants) {
            this.variants = variants;
        }
    }

    @Parameterized.Parameters
    public static List<Object[]> examples() {
        return asList(new Object[][]{
                {true, null},
                {true, Collections.emptyList()},
                {true, asList(new EmailSendingVariantConf().setPercent(99))},
                {true, asList(new ActionVariant().setPercent(50), new ActionVariant().setPercent(30))},
                {true, asList(new GncSendingVariantConf().setPercent(100))},
                {true, asList(new PushSendingVariantConf().setPercent(70),
                        new PushSendingVariantConf().setPercent(30))},
                {false, asList(new EmailSendingVariantConf().setPercent(99),
                        new EmailSendingVariantConf().setPercent(2))},
                {false, asList(new ActionVariant().setPercent(50), new ActionVariant().setPercent(51))},
                {false, asList(new GncSendingVariantConf().setPercent(100),
                        new GncSendingVariantConf().setPercent(100))},
                {false, asList(new PushSendingVariantConf().setPercent(71),
                        new PushSendingVariantConf().setPercent(30))},
        });
    }

    @Test
    public void testConfigs() {
        if (!isValid) {
            expectedException.expect(IllegalArgumentException.class);
        }
        VariantUtils.validate(new ConcreteConf(variants));
    }
}

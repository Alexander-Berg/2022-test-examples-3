package ru.yandex.market.billing.config;

import javax.annotation.Nonnull;

import ru.yandex.market.core.mds.AbstractMdsS3ConfigTest;

/**
 * Unit-тесты для {@link BillingMdsS3Config}.
 *
 * @author Vladislav Bauer
 */
public class BillingMdsS3ConfigTest extends AbstractMdsS3ConfigTest<BillingMdsS3Config> {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    protected BillingMdsS3Config createConfig() {
        return new BillingMdsS3Config();
    }

}

package ru.yandex.market.admin.config;

import javax.annotation.Nonnull;

import ru.yandex.market.core.mds.AbstractMdsS3ConfigTest;

/**
 * Unit-тесты для {@link AdminMdsS3Config}.
 *
 * @author Vladislav Bauer
 */
public class AdminMdsS3ConfigTest extends AbstractMdsS3ConfigTest<AdminMdsS3Config> {

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    protected AdminMdsS3Config createConfig() {
        return new AdminMdsS3Config();
    }

}

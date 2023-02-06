package ru.yandex.market.tsup.core.data_provider.provider;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.tpl.common.data_provider.meta.ProviderMeta;
import ru.yandex.market.tpl.common.data_provider.provider.DataProvider;

@RequiredArgsConstructor
public class TestDataProvider implements DataProvider<String, TestProviderFilter> {
    public static final String RESULT = "test";
    private final LMSClient lmsClient;

    @Override
    public String provide(
        TestProviderFilter filter,
        @Nullable ProviderMeta meta
    ) {
        lmsClient.getLogisticsPoint(filter.p);
        return RESULT;
    }
}

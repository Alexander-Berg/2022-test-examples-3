package ru.yandex.market.tsup.core.data_provider.provider;

import lombok.Data;
import lombok.experimental.Accessors;

import ru.yandex.market.tpl.common.data_provider.filter.ProviderFilter;

@Data
@Accessors(chain = true)
public class TestProviderFilter implements ProviderFilter {
    long p;
}

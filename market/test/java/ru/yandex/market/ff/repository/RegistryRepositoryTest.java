package ru.yandex.market.ff.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RegistryFlowType;

public class RegistryRepositoryTest extends IntegrationTest {
    @Autowired
    private RegistryRepository registryRepository;

    @Test
    @DatabaseSetup(value = "classpath:service/registry/10/before.xml")
    void existsByRequestIdAndType() {
        Assertions.assertThat(registryRepository.existsByRequestIdAndType(1L, RegistryFlowType.FACT)).isTrue();
        Assertions.assertThat(registryRepository.existsByRequestIdAndType(10000L, RegistryFlowType.FACT)).isFalse();
        Assertions.assertThat(registryRepository.existsByRequestIdAndType(1L, RegistryFlowType.PREPARED)).isFalse();
    }
}

package ru.yandex.market.core.moderation.sandbox.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.core.moderation.sandbox.SandboxState;
import ru.yandex.market.core.moderation.sandbox.SandboxStateFactory;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingStatusDao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author zoom
 */
class DefaultSandboxRepositoryTest {
    private DefaultSandboxRepository repository;

    @Mock
    private TestingStatusDao testingStatusDao;

    @Mock
    private SandboxStateFactory sandboxStateFactory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new DefaultSandboxRepository(null, testingStatusDao, null, null, sandboxStateFactory);
    }

    @Test
    void shouldReturnNullWhenRecordNotFound() {
        SandboxState state = repository.load(10, ShopProgram.CPC);
        verify(testingStatusDao).load(10L, ShopProgram.CPC);
        verifyNoMoreInteractions(sandboxStateFactory, testingStatusDao);
        assertThat(state).isNull();
    }
}

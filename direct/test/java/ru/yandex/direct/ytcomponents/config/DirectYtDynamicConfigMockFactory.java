package ru.yandex.direct.ytcomponents.config;

import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Для использования {@link DirectYtDynamicConfig} в тестах
 */
public class DirectYtDynamicConfigMockFactory {

    /**
     * Создаёт mock для {@link DirectYtDynamicConfig}.
     * <p>
     * Для переопределения возвращаемых значений следует использовать конструкцию
     * {@code when(mockConfig.tables().direct().syncStatesTablePath()).thenReturn("...")},
     * а не {@code doReturn("...").when(mockConfig.<...>)}, так как в последнем варианте
     * Mockito падает на инициализации
     */
    public static DirectYtDynamicConfig createConfigMock() {
        DirectYtDynamicConfig.Tables tables = mock(DirectYtDynamicConfig.Tables.class);

        // Возвращаем пустые строчки вместо null (RETURNS_SMART_NULLS), чтобы в тестах не ловить NPE
        when(tables.direct()).thenReturn(mock(DirectYtDynamicConfig.DirectTables.class, RETURNS_SMART_NULLS));

        when(tables.yabsStat()).thenReturn(mock(DirectYtDynamicConfig.YabsStatTables.class, RETURNS_SMART_NULLS));

        when(tables.recommendations())
                .thenReturn(mock(DirectYtDynamicConfig.RecommendationTables.class, RETURNS_SMART_NULLS));

        DirectYtDynamicConfig config = mock(DirectYtDynamicConfig.class);
        when(config.tables()).thenReturn(tables);
        return config;
    }
}

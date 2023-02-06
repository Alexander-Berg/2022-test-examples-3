/*
 * @title Скрипт фильтрации
 */
api.db.filters.with {
    and(
            eq('information', '456 Тестовый'),
            eq('description', '789 Красавица')
    )
}
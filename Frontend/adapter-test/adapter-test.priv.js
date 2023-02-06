/**
 * Тестовый адаптер. Предназначен для тестирования новых блоков во внутренней сети Яндекса.
 *
 * @param {Context} context
 * @param {Object} snippet
 *
 * @returns {Object}
 */
blocks['adapter-test'] = function(context, snippet) {
    if (!context.isYandexNet) {
        throw new Error('Adapter "test" is not allowed outside yandex network.');
    }

    return snippet;
};

/**
 * Тестовый адаптер. Подготавливаем данные для тестов конструкторского AJAX
 *
 * @param {Context} context
 *
 * @returns {Object}
 */
blocks['adapter-test__ajax'] = function(context) {
    if (!context.isYandexNet) {
        throw new Error('Adapter "test" is not allowed outside yandex network.');
    }

    return {
        block: 'raw-text',
        text: 'Текст загруженный AJAX'
    };
};

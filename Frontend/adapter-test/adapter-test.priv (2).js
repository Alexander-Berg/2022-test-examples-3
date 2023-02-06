/**
 * Тестовый адаптер. Предназначен для тестирования новых блоков во внутренней сети Яндекса.
 *
 * @param {Context} context
 * @param {Object} snippet
 *
 * @returns {Object} snippet
 */
blocks['adapter-test'] = function(context, snippet) {
    if (!context.isYandexNet) {
        throw new Error('Adapter "test" is not allowed outside yandex network.');
    }

    return snippet;
};

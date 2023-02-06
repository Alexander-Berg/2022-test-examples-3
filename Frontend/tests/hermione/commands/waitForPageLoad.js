/**
 * Функция дает странице время на полную загрузку 
 * @param {*} timeout - таймаут на полную загрузку страницы
 * @returns {Object}
 */
 module.exports = function waitForPageLoad(timeout = 5000) {
    return this
        .waitUntil(() => {
            return this
                .execute(() => document.readyState === 'complete' )
        }, timeout, `Страница не загрузилась за ${timeout/1000}с.`, 500)
};

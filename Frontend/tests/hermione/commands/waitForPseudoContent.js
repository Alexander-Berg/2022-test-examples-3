/**
 * Функция проверяет, присутствует ли псевдоэлемент с прописанным контентом
 * Такой подход используется для проверки иконок
 * @param {*} selector - css селектор
 * @param {*} pseudo - псевдоэлемент before, after...
 * @param {*} timeout - время ожидания появления псевоэлемента с контентом
 * @returns {Object}
 */

module.exports = function(selector, pseudo, timeout = 5000) {
    return this
        .waitForVisible(selector)
        .waitUntil(() => {
            return this
                .execute((selector, pseudo) => {
                    const element = document.querySelector(selector);
                    if (!element) {
                        return undefined;
                    }
                    return window.getComputedStyle(element, `:${pseudo}`).getPropertyValue("content");
                }, selector, pseudo)
                .then((content) => {
                    return content && content != 'normal' && content != 'none'
                })
        }, timeout, `Pseudoelement "${pseudo}" has no content`, 100);
};

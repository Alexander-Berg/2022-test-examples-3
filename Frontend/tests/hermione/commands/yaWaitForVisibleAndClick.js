/**
 * Ожидание элемента и клик по нему
 * @param {string} selector - селектор
 * @param {number} time - таймаут по истечению которого, если элемент не появился на экране появится ошибка
 * @param {boolean} clickWithBrowserMethod кликнуть с помощью браузерного клика
 * @return {Promise<void>}
 */
module.exports = async function yaWaitForVisibleAndClick(selector, time = 10_000, clickWithBrowserMethod = false) {
    await this.waitForVisible(selector, time);
    if (clickWithBrowserMethod) {
        await this.execute(function(selector) {
            const item = document.querySelector(selector);
            if (item) {
                item.click();
            }
        }, selector);
    } else {
        await this.click(selector);
    }
};

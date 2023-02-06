/**
 * Ожидание элемента и клик по нему
 * @param selector - селектор
 * @param time - таймаут по истечению которого, если элемент не появился на экране появится ошибка
 * @return {Promise<void>}
 */
module.exports = async function yaWaitForVisibleAndClick(selector, time = 10_000) {
    await this.waitForVisible(selector, time);
    await this.click(selector);
};

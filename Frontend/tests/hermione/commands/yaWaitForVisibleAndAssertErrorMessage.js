/**
 * Ожидание элемента и выводит через assert требуемый текст ошибки
 * @param selector - селектор
 * @param time - таймаут по истечению которого, если элемент не появился на экране появится ошибка
 * @param errorMessage - текст ошибки, который нужно вывести
 * @param reverse - если true, то ожидает обратного поведения - что елемент не должен быть во view
 * @return {Promise<void>}
 */
module.exports = async function yaWaitForVisibleAndAssertErrorMessage(selector, time = 10_000, errorMessage = '', reverse = false) {
    if (reverse) {
        try {
            await this.waitForVisible(selector, time, true);
        } catch (error) {
            await assert(false, errorMessage ? errorMessage : `yaWaitForVisible: элемент ${selector} виден, хотя не должен быть`);
        }
    } else {
        try {
            await this.waitForVisible(selector, time);
        } catch (error) {
            await assert(false, errorMessage ? errorMessage : `yaWaitForVisible: элемент ${selector} не отобразился`);
        }
    }
};

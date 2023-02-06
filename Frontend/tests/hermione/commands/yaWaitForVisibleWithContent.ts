/**
 * Обёртка над стандартной командой waitForVisible. Позволяет указывать произвольное сообщение об ошибке.
 *
 * @param {String} selector - Селектор для элемента, появление которого нужно ждать
 * @param {String} text - Текст для элемента, появление которого нужно ждать
 * @param {Number} [timeout] - Таймаут в миллисекундах
 * @param {String} [message] - Сообщение об ошибке
 *
 * @returns {Promise}
 */
module.exports = async function yaWaitForVisibleWithContent(
    selector: string,
    text: string,
    timeout: number = 10000,
    message: string
) {
    return await this.waitUntil(async () => {
        const timestamp = Date.now();

        while (Date.now() - timestamp < timeout) {
            const contentSelector = await this.yaGetContainsSelector(selector, text);

            try {
                await this.yaWaitForVisible(contentSelector, 10);

                return true;
            } catch {
            }
        }
    }, timeout, message);
};

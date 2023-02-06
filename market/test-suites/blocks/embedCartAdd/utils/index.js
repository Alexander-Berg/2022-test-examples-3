/**
 * Хелпер проверяющий что postMessage сообщение было отправлено
 * @param {Object} message - Объект события
 * @param {String} message.event - Имя события
 * @param {Object} message.params - Параметры события
 * @param {String} failureMessage - Сообщение об ошибке, если сообщение не будет найдено
 * @return {Promise<void>}
 */
async function checkThatMessageWasDispatched(message, failureMessage) {
    const allMessages = await this.browser.yaExecClientScript('window.getRecordedPostMessages');

    await this.browser.expect(allMessages).to.deep.include(message, failureMessage);
}

/**
 * Хелпер проверяющий что сообщение было отправлено N раз
 * @param {String} messageEvent - Имя сообщения
 * @param {Number} count - Ожидаемое кол-во сообщений
 * @param {String} failureMessage - Сообщение об ошибке, если сообщение не будет найдено
 * @return {Promise<void>}
 */
async function checkMessageCount(messageEvent, count, failureMessage) {
    const allMessages = await this.browser.yaExecClientScript('window.getRecordedPostMessages');
    const targetMessages = allMessages.filter(message => message.event === messageEvent);

    await this.browser.expect(targetMessages.length).to.be.equal(count, failureMessage);
}

/**
 * Хелпер позволяющий дождаться, когда iframe корзины будет загружен
 * @return {Promise<void>}
 */
async function waitForLoad() {
    await this.browser.waitUntil(async () => {
        const messages = await this.browser.yaExecClientScript('window.getRecordedPostMessages');

        return messages.map(message => message.event).includes('ready');
    });
}

/**
 * Хелпер ожидающй когда сообщение будет отправлено
 * @param {String} event - Имя сообщения
 * @return {Promise<void>}
 */
async function waitForMessage(event) {
    const messages = await this.browser.yaExecClientScript('window.getRecordedPostMessages');

    // Считаем текущее кол-во сообщений
    const prevMessagesCount = messages.filter(message => message.event === event).length;

    // Ждём когда кол-во сообщений увеличится на единицу
    await this.browser.waitUntil(async () => {
        const actualMessages = await this.browser.yaExecClientScript('window.getRecordedPostMessages');
        const actualMessagesCount = actualMessages.filter(message => message.event === event).length;

        return actualMessagesCount === prevMessagesCount + 1;
    });
}

async function postMessage(message) {
    await this.browser.execute(messagePayload => {
        window.postMessage(messagePayload, '*');
    }, message);
}

// eslint-disable-next-line import/no-commonjs
module.exports = {
    checkThatMessageWasDispatched,
    checkMessageCount,
    waitForLoad,
    waitForMessage,
    postMessage,
};

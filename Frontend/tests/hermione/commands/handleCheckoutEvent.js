/**
 * Отвечает на событие чекаута
 *
 * @param {String} eventName – название события
 * @param {Object} data – ответ
 * @returns {Promise}
 */
module.exports = async function handleCheckoutEvent(eventName, data) {
    await this.waitUntil(async() => {
        const result = await this.execute(() => window.__getEventsQueue()[0]);
        return result.value && result.value.eventName === eventName;
    }, 5000, `Событие ${eventName} не произошло в течение 5 секунд`);

    await this.execute((eventName, data) => {
        window.__respondToEvent(eventName, data);
    }, eventName, data);
};

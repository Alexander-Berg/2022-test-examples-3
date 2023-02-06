/**
 * Отдает результат чекаута
 *
 * @returns {Promise}
 */
module.exports = async function getCheckoutResult() {
    const result = await this.execute(() => window.__getCheckoutResult());

    return result.value;
};

/**
 * Скролит InfiniteList
 *
 * @param {String} deltaY - значение, на которое нужно проскролить
 *
 * @returns {Promise}
 */

module.exports = async function yaScrollInfiniteList(deltaY: string) {
    const selector = '.ui-InfiniteList-Container';

    await this.yaWaitForVisible(selector, 'InfiniteList не показался');

    // eslint-disable-next-line no-shadow
    await this.execute((selector, deltaY) => {
        const event = new WheelEvent('wheel', { deltaY, bubbles: true });
        const list = document.querySelector(selector);

        list.dispatchEvent(event);
    }, selector, deltaY);

    await this.pause(1000);
};

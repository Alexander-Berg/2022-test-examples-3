/**
 * @copypaste https://github.yandex-team.ru/mm-interfaces/fiji/blob/dev/runner/hermione/commands/clickTo.js
 */

/**
 * Клик в определенную точку элемента
 *
 * @param {String} selector - селектор элемента
 * @param {Number} clientX - горизонтальный отступ от верхнего левого угла элемента selector
 * @param {Number} clientY - вертикальный отступ от верхнего левого угла элемента selector
 *
 * @returns {Promise}
 */

module.exports = async function (selector: string, clientX: number, clientY: number) {
    /* в FF не работает leftClick */
    if (this.desiredCapabilities?.browserName === 'firefox') {
        const { x, y } = await this.getLocation(selector);
        await this.actions([{
            type: 'pointer',
            id: 'mouse1',
            parameters: { pointerType: 'mouse' },
            actions: [
                { type: 'pointerMove', duration: 0, x: x + clientX, y: y + clientY },
                { type: 'pointerDown', button: 0 },
                { type: 'pointerUp', button: 0 },
            ],
        }]);
    } else {
        await this.leftClick(selector, clientX, clientY);
    }
};

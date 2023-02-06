const consts = require('../config').consts;

/**
 * Производит клик мышью с зажатой клавишей
 *
 * @param {string} selector
 * @param {string} key
 *
 * @returns {Promise<void>}
 */
const yaClickWithPressedKey = async function(selector, key) {
    let webElement;
    if (selector[0] === '/') { // для xpath
        webElement = (await this.execute((path) =>
            document.evaluate(path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue,
        selector));
    } else {
        webElement = (await this.execute((selector) => {
            return document.querySelector(selector);
        }, selector));
    }
    await this.yaScrollIntoView(selector);
    // В firefox использование actions вызывает неадекватное поведение при нажимании control - мышка неизбежно кликает
    // правой кнопкой, а не левой. После обновления webdriver в гермионе может перестать быть актуальным.
    if (key === consts.KEY_CTRL) {
        await this.execute((element) => {
            const mouseDown = new MouseEvent('mousedown', { bubbles: true, ctrlKey: true, cancelable: true });
            const mouseUp = new MouseEvent('mouseup', { bubbles: true, ctrlKey: true, cancelable: true });
            const click = new MouseEvent('click', { bubbles: true, ctrlKey: true, cancelable: true });
            element.dispatchEvent(mouseDown);
            element.dispatchEvent(mouseUp);
            element.dispatchEvent(click);
        }, webElement);
    } else {
        await this.actions(
            [
                {
                    type: 'key',
                    id: 'pressKey',
                    actions: [
                        { type: 'pause' },
                        { type: 'keyDown', value: key },
                        { type: 'pause' },
                        { type: 'pause' },
                        { type: 'keyUp', value: key },
                    ]
                },
                {
                    type: 'pointer',
                    id: 'leftClick',
                    parameters: { pointerType: 'mouse' },
                    actions: [
                        { type: 'pointerMove', origin: webElement, x: 1, y: 1 },
                        { type: 'pointerDown', button: 0, which: 1 },
                        { type: 'pointerUp', button: 0, which: 1 },
                    ]
                },
            ]
        );
    }
    await this.yaResetPointerPosition(); // чтобы не было проблем с hover-эффектами
};

module.exports = {
    common: {
        yaClickWithPressedKey,
    }
};

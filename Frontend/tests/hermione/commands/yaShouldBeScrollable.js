/**
 * Скролит, выравнивая нижнюю границе элемента по нижней границе экрана.
 *
 * @param {String} selector - селектор скроллящегося элемента
 * @param {Object} options - настройки проверки
 * @param {bool} [options.h=false] - должен скроллиться горизонтально
 * @param {bool} [options.v=false] - должен скроллиться вертикально
 *
 * @returns {Promise}
 */
module.exports = function yaShouldBeScrollable(selector, options) {
    if (options) {
        options.h = typeof options.h !== 'undefined' ? options.h : false;
        options.v = typeof options.v !== 'undefined' ? options.v : false;
    } else {
        options = { h: false, v: false };
    }

    const testInfo = {};
    return this
        .execute(containerSelector => {
            const container = document.querySelector(containerSelector);

            return { initX: container.scrollLeft, initY: container.scrollTop };
        }, selector)
        .then(({ value: { initX, initY } }) => {
            testInfo.initX = initX;
            testInfo.initY = initY;
        })
        .yaScrollElement(selector, 9999, 9999)
        .execute(containerSelector => {
            const container = document.querySelector(containerSelector);

            return { newX: container.scrollLeft, newY: container.scrollTop };
        }, selector)
        .then(({ value: { newX, newY } }) => {
            testInfo.newX = newX;
            testInfo.newY = newY;

            const scrolledX = testInfo.initX !== testInfo.newX;
            const scrolledY = testInfo.initY !== testInfo.newY;

            const msgh = options.h ? 'должен' : 'не должен';
            const msgv = options.v ? 'должен' : 'не должен';

            const expectedX = scrolledX === options.h;
            const expectedY = scrolledY === options.v;

            assert.isTrue(expectedX || expectedY, 'Элемент ' + msgh + ' скроллиться по оси X, ' +
                                                                msgv + ' скроллиться по оси Y');
            assert.isTrue(expectedX, 'Элемент ' + msgh + ' скроллиться по оси X');
            assert.isTrue(expectedY, 'Элемент ' + msgv + ' скроллиться по оси Y');
        });
};

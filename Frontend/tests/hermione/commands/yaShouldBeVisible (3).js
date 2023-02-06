module.exports = function(selector, visible = true) {
    return this
        .isVisible(selector)
        .then(isVisible => {
            if (Array.isArray(isVisible)) {
                throw new Error(
                    'Найдено более одного элемента. ' +
                    `Пожалуйста, используйте более конкретный селектор. Исходный селектор - ${selector}`
                );
            }

            assert.strictEqual(
                isVisible,
                visible,
                `Элемент с селектором ${selector} ${visible ? 'не' : ''} виден`
            );
        });
};

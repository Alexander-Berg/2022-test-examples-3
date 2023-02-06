module.exports = function(element, isVisible = true) {
    return this
        .execute(selector => {
            const rect = document.querySelector(selector).getBoundingClientRect();
            const clientHeight = document.documentElement.clientHeight;

            return rect.top < clientHeight;
        }, element)
        .then(({ value }) => {
            assert.strictEqual(value, isVisible, `Элемент ${isVisible ? 'не' : ''} находится в зоне вьюпорта`);
        });
};

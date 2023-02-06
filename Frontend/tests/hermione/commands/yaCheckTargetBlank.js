module.exports = function(selector) {
    return this
        .getAttribute(selector, 'target')
        .then(value => assert.strictEqual(
            value,
            '_blank',
            'Неверный аттрибут target'
        ));
};

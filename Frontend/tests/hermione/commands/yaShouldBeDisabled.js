module.exports = function(selector) {
    return this
        .getAttribute(selector, 'disabled')
        .then(isDisabled => assert.strictEqual(
            isDisabled,
            'true',
            `У селектора ${selector} неверное значение аттрибута disabled`
        ));
};

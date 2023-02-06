module.exports = function(selector, count, message) {
    return this
        .elements(selector)
        .then(({ value: elems }) => assert.equal(elems.length, count, message));
};

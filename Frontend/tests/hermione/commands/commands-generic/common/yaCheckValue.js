module.exports = function(selector, expectedValue) {
    const value = this.getValue(selector);
    assert(value, expectedValue);
};

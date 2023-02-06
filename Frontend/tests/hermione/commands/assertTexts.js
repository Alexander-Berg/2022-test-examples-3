function getString(text) {
    return Array.isArray(text) ? text.filter(Boolean).join('\n') : text;
}

/**
 * Сравнивает два текста, полученных с помощью `getText`
 * @param {String | String[]} firstText
 * @param {String | String[]} secondText
 * @param {String} message
 */
module.exports = function assertTexts(firstText, secondText, message) {
    assert.strictEqual(getString(firstText), getString(secondText), message);
};

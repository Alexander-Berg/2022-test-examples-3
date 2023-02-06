/**
 * Команда для выбора значения из саджеста
 *
 * @param {String} inputSelector
 * @param {String} value
 * @returns {Promise}
 */
module.exports = function selectFromSuggest(inputSelector, value) {
    return this
        .clearInput(inputSelector)
        .setValue(inputSelector, value)
        .waitForVisible(`.suggest-item [title^="${value}"], .search-list__item [data-name^="${value}"]`, 30000)
        .click(`.suggest-item [title^="${value}"], .search-list__item [data-name^="${value}"]`);
};

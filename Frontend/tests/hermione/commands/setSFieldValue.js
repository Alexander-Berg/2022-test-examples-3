/**
 * Выбор значения в sField
 * @param {String} block - селектор s-field
 * @param {String} value - значение. Дату передавать в виде 'MM-DD-YYYY'.
 * @returns {Object}
 */
module.exports = function setSFieldValue(block, value) {
    return this
        .execute((block, value) => {
            $(block).bem('s-field').val(value);
        }, block, value);
};

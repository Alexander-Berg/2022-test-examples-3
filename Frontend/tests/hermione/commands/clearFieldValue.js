/**
 * Чистит значение поля
 * @param {String} block - селектор s-field
 * @returns {Object}
 */
module.exports = function clearFieldValue(block) {
    return this
        .getValue(block)
        .then(value => {
            const backs = new Array(value.length).fill('\uE003').join('');
            return this.setValue(block, backs);
        });
};

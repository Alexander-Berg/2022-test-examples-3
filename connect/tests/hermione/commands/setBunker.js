/**
 * Ставит куку по которой определяется надо ли ходить в бункер.
 * @param {Boolean} val
 * @returns {Promise}
 */
module.exports = function(val = false) {
    if (val) {
        return this.setCookie({
            name: '__bunker__',
            value: String(val),
        });
    }
};

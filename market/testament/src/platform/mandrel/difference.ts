// @ts-nocheck
/* eslint-disable */

/**
 *
 * Возвращает diff между двумя объектами или false, если отличий нет
 * @param {Object} o1
 * @param {Object} o2
 * @param {Boolean} ignoreUndefined - не учитывать при сравнении поля равные undefined
 * @returns {Object|false}
 */
module.exports = function difference(o1, o2, ignoreUndefined = false) {
    var k;
    var kDiff;
    var diff = {};

    if (typeof o1 != 'object' || typeof o2 != 'object') {
        return o1;
    }

    for (k in o1) {
        if (Object.prototype.hasOwnProperty.call(o1, k) && (o1.k !== undefined || !ignoreUndefined)) {
            if (typeof o1[k] != 'object' || typeof o2[k] != 'object') {
                if (!(k in o2) || o1[k] !== o2[k]) {
                    diff[k] = o2[k];
                }
            } else if (kDiff = difference(o1[k], o2[k])) {
                diff[k] = kDiff;
            }
        }
    }

    for (k in o2) {
        if (Object.prototype.hasOwnProperty.call(o2, k) && (o2.k !== undefined || !ignoreUndefined)) {
            if (!(k in o1)) {
                diff[k] = o2[k];
            }
        }
    }

    for (k in diff) {
        if (Object.prototype.hasOwnProperty.call(diff, k)) {
            return diff;
        }
    }
    return false;
};

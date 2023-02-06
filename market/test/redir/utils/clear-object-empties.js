/**
 * Clear object from empty or null fields.
 */

function removeEmptyOrNull(obj) {
    Object.keys(obj).forEach(
        (k) =>
            (obj[k] && typeof obj[k] === 'object' && removeEmptyOrNull(obj[k])) ||
            (!obj[k] && obj[k] !== undefined && delete obj[k]),
    );
    return obj;
}

module.exports = { removeEmptyOrNull };

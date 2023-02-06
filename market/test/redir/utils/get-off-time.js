'use strict';

const OFF_TIME = {
    'half-hour': 1800000,
    'day': 86400000,
    '3 days': 259200000,
    '7 days': 604800000,
    '30 days': 2592000000
};

/**
 * Get off time.
 *
 * @param {String} timeKey
 * @returns {?Number}
 */
function getOffTime(timeKey) {
    return OFF_TIME[timeKey];
}

/**
 * @type {getOffTime}
 */
module.exports = getOffTime;


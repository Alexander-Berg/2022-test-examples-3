/**
 * Milliseconds in one hour
 *
 * @type {number}
 */
const MS_IN_HOUR = 36e5;

/**
 * Milliseconds in one day
 *
 * @type {number}
 */
const MS_IN_DAY = 864e5;

/**
 * Enum for filter keys
 *
 * @readonly
 * @enum {string}
 */
const prefixes = {
    COPY: 'copy-target_',
    MOVE: 'move-target_',
    TMP: 'tmp-',
    ANY: () => true
};

module.exports = { MS_IN_HOUR, MS_IN_DAY, prefixes };

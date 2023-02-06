'use strict';

/**
 * @param {*} value
 * @return {Boolean}
 *
 * @example
 *  isInteger('123'); // true
 *  isInteger('123abc'); // false
 *  isInteger(123); // true
 *
 */
module.exports = (value) => !isNaN(parseInt(value)) && isFinite(value);

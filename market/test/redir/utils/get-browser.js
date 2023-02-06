'use strict';

const UAParser = require('ua-parser-js');
const uaParser = new UAParser();

/**
 *
 * @param userAgent
 * @return {{name: String, version: String, major: String}}
 */
function getBrowser(userAgent) {
    return uaParser.setUA(userAgent).getBrowser();
}

/**
 * @type {getBrowser}
 */
module.exports = getBrowser;
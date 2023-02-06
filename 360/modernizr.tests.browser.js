/**
 * @fileOverview Тесты для браузеров через uatraits
 */

/**
 * Версия IE.
 * @name Modernizr.msie
 * @type {Boolean|Number}
 */
Modernizr.addTest('msie', function() {
    if (Daria.UA.BrowserName === 'MSIE') {
        return document['documentMode'];
    }

    return false;
});

/**
 * Версия webkit (Chrome, Chromium, Safari, Ya)
 * @name Modernizr.webkit
 * @type {Boolean|Number}
 */
Modernizr.addTest('webkit', function() {
    if (Daria.UA.BrowserEngine === 'WebKit') {
        return Daria.UA.BrowserEngineVersion;
    }

    return false;
});

/**
 * Версия Opera
 * @name Modernizr.opera
 * @type {Boolean|Number}
 */
Modernizr.addTest('opera', function() {
    if (Daria.UA.BrowserName === 'Opera') {
        return Daria.UA.BrowserVersion;
    }

    return false;
});

/**
 * Версия FF
 * @name Modernizr.mozilla
 * @type {Boolean|Number}
 */
Modernizr.addTest('mozilla', function() {
    if (Daria.UA.BrowserEngine === 'Gecko') {
        return Daria.UA.BrowserEngineVersion;
    }

    return false;
});

/**
 * Версия Edge
 * @name Modernizr.edge
 * @type {Boolean|Number}
 */
Modernizr.addTest('edge', function() {
    if (Daria.UA.BrowserEngine === 'Edge') {
        return Daria.UA.BrowserEngineVersion;
    }

    return false;
});

/**
 * Проверка на Safari
 * @name Modernizr.safari
 * @type {Boolean|Number}
 */
Modernizr.addTest('safari', function() {
    return Daria.UA.BrowserName === 'Safari';
});

/**
 * Проверка на IE9
 * @name Modernizr.ie9
 * @type {Boolean}
 */
Modernizr.addTest('ie9', function() {
    // если это IE и documentMode === 9
    return Daria.UA.BrowserName === 'MSIE' && document['documentMode'] === 9;
});

/**
 * Проверка на IE10
 * @name Modernizr.ie10
 * @type {Boolean}
 */
Modernizr.addTest('ie10', function() {
    return Daria.UA.BrowserName === 'MSIE' && document['documentMode'] === 10;
});

/**
 * Проверка на IE11
 * @name Modernizr.ie11
 * @type {Boolean}
 */
Modernizr.addTest('ie11', function() {
    return Daria.UA.BrowserName === 'MSIE' && document['documentMode'] === 11;
});

/**
 * Проверка на IE < 11
 * @name Modernizr.ielt11
 * @type {Boolean}
 */
Modernizr.addTest('ielt11', function() {
    // если это IE и documentMode < 11
    return Daria.UA.BrowserName === 'MSIE' && document['documentMode'] < 11;
});

/**
 * Проверка на iOS
 * @name Modernizr.ios
 * @type {Boolean}
 */
Modernizr.addTest('ios', function() {
    return Daria.UA.OSFamily === 'iOS';
});

/**
 * Проверка на OSX
 * @name Modernizr.mac
 * @type {Boolean}
 */
Modernizr.addTest('mac', function() {
    return Daria.UA.OSFamily === 'MacOS';
});

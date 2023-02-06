'use strict';

const mockRequire = require('mock-require');

mockRequire('@yandex-int/express-uatraits', () => (req, res, next) => {
    req.uatraits = {
        BrowserBase: 'Chromium',
        BrowserBaseVersion: '56.0.2924.87',
        BrowserEngine: 'WebKit',
        BrowserEngineVersion: '537.36',
        BrowserName: 'YandexBrowser',
        BrowserVersion: '17.3.1.873',
        OSFamily: 'Linux',
        YaGUI: '2.5',
        isBrowser: true,
        isMobile: false,
        x64: true
    };
    next();
});

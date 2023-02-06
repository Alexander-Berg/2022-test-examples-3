'use strict';
const defaultConfig = {
    availableLanguages: {
        ru: ['ru'],
        com: ['en'],
    },
};

function getApp(headers, langdetectConfig) {
    langdetectConfig = langdetectConfig || defaultConfig;
    const app = require('express')();
    const cookieParser = require('cookie-parser');
    app.enable('trust proxy');
    app.use((req, res, next) => {
        for (const header in headers) {
            if (Object.prototype.hasOwnProperty.call(headers, header)) {
                req.headers[header] = headers[header];
            }
        }
        next();
    });
    app.use(cookieParser());
    app.use(require('express-blackbox')({
        api: 'blackbox-mimino.yandex.net',
    }));
    app.use(require('express-http-geobase')());
    app.use(require('..')(langdetectConfig));

    app.get('/', (req, res) => {
        res.send(req.langdetect);
    });
    return app;
}

module.exports = getApp;

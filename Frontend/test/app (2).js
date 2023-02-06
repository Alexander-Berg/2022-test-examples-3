const createServer = require('express');
const cookieParser = require('cookie-parser');
const createExpressBlackBox = require('@yandex-int/express-blackbox');

const createExpressLangDetect = require('../dist').default;
const defaultConfig = {
    availableLanguages: {
        ru: ['ru'],
        com: ['en'],
    },
    langDetectData: '/usr/share/yandex/lang_detect_data.txt',
};

function getApp(headers, langdetectConfig) {
    langdetectConfig = langdetectConfig || defaultConfig;
    const app = createServer();
    app.enable('trust proxy');
    app.use(cookieParser());
    app.use(function(req, res, next) {
        for (const header in headers) {
            req.headers[header] = headers[header];
        }
        next();
    });
    app.use(createExpressBlackBox({ api: 'blackbox-mimino.yandex.net' }));
    app.use(createExpressLangDetect(langdetectConfig));

    app.get('/', function(req, res) {
        res.send(req.langdetect);
    });

    return app;
}

module.exports = getApp;

let express = require('express');

function mw(cb) {
    return function(req, res) {
        cb && typeof cb === 'function' && cb.apply(this, arguments);
        res.sendStatus(200);
    };
}

function getApp() {
    let app = express();

    let config = {
        app: {
            domains: ['ru', 'ua', 'by', 'kz', 'com', 'com.tr', 'net'],
        },
    };

    function configure(app) {
        app.set('env', 'tests');
        app.disable('x-powered-by');
        app.disable('strict routing');
        app.enable('trust proxy');
    }

    function useMiddleware(app) {
        app.use(require('cookie-parser')());
        app.use(require('express-http-geobase')());
        // Осуществляем национальный редирект
        app.use(require('..')(config));
    }

    configure(app);
    // eslint-disable-next-line react-hooks/rules-of-hooks
    useMiddleware(app);

    return app;
}

// Настроенное Express приложение
exports.getApp = getApp;

// Волшебная мидделвара исполняющая коллбек и отправляющая 200 OK
exports.mw = mw;

// GeoID Минска
exports.BELORUSSIAN_YANDEX_GID = 157;

// Екатеринбургский IP-адрес
exports.YEKATERINBURGS_IP_ADDRESS = '87.250.248.243';

// Харьковский IP-адрес
exports.KHARKOVS_IP_ADDRESS = '178.165.60.4';

'use strict';

const supertest = require('supertest');
const yandexLogger = require('@yandex-int/yandex-logger');

const supertestWrapper = (app, timeout = 2000) => callback => {
    const server = app.listen(0, 'localhost', () => {
        const closeServer = server.close.bind(server);

        const timer = setTimeout(closeServer, timeout);

        callback(
            supertest(server),
            () => {
                clearTimeout(timer);
                closeServer();
            }
        );
    });
};

function mockAddLoggerToRequest(req, _res, next) {
    req.logger = yandexLogger({
        streams: [],
        middleware: [],
    });

    next();
}

module.exports = {
    supertestWrapper,
    mockAddLoggerToRequest,
};

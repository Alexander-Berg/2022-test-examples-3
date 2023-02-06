'use strict';

const { express, middleware: { cookieParser } } = require('@yandex-int/duffman');

const app = express();
app.use(cookieParser);
const routes = require('../../../routers/web-api.js').routes;
routes.forEach((route) => {
    app.use(route.name, require(route.path));
});

module.exports = app;

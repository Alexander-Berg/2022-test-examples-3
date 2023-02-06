'use strict';

let runMiddleware = require('./lib/run-middleware');

let req = {
    cookies: {},
    query: {},
    body: {},
    headers: {},
};
let res = {
    clearCookie() {},
    on() {},
    cookie() {},
    jsonp() {},
};

async function init() {
    try {
        await runMiddleware(require('../../routes/products'), req, res);
    } catch (ex) {
        console.log(ex);
    }

    console.log(req);
}

module.exports = init;

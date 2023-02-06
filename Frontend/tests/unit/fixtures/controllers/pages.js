'use strict';

const Controller = require('../../../../src/server/controller');

class Pages extends Controller {
    mod() {
        return this.res.send('').end();
    }
}

module.exports = Pages;

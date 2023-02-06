'use strict';
const Controller = require('../../../../src/server/controller');

class DebugPages extends Controller {
    experiment() {
        return this.res.json({}).end();
    }

    config() {
        return this.res.json({}).end();
    }

    queries() {
        return this.res.send('').end();
    }

    token() {
        return this.res.send('').end();
    }
}

module.exports = DebugPages;

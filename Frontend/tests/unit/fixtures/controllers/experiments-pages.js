'use strict';

const Controller = require('../../../../src/server/controller');

class ExperimentsPages extends Controller {
    listRouter() {
        return this.res.json().end();
    }

    form() {
        return this.res.send('').end();
    }

    results() {
        return this.res.send('').end();
    }

    queriesAnalysisBlock() {
        return this.res.json({}).end();
    }
}

module.exports = ExperimentsPages;

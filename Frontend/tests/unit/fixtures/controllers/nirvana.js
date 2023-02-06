'use strict';

const Controller = require('../../../../src/server/controller');

class Nirvana extends Controller {
    setPools() {
        return this.renderJsonSuccess();
    }

    setStatus() {
        return this.renderJsonSuccess();
    }

    getExpValidityStatus() {
        return this.renderJsonSuccess();
    }

    setCommentsAndVotesFiles() {
        return this.renderJsonSuccess();
    }

    uploadPlanStats() {
        return this.renderJsonSuccess();
    }

    setExpStats() {
        return this.renderJsonSuccess();
    }
}
module.exports = Nirvana;

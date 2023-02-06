'use strict';

const Controller = require('../../../../src/server/controller');

class UserApi extends Controller {
    getNotification() {
        return this.res.json({ id: Date.now(), text: '' }).end();
    }

    disableNotification() {
        return this.renderJsonSuccess();
    }
}

module.exports = UserApi;

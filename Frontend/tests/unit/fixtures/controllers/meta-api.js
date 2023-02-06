'use strict';

const Controller = require('../../../../src/server/controller');

class MetaApi extends Controller {
    setTemplateWorkflowId() {
        return this.renderJsonSuccess();
    }

    setTemplateWorkflowDevId() {
        return this.renderJsonSuccess();
    }

    setDefaultBeta() {
        return this.renderJsonSuccess();
    }

    setPoolsList() {
        return this.renderJsonSuccess();
    }

    setNotification() {
        return this.renderJsonSuccess();
    }

    toggleWorkflows() {
        return this.renderJsonSuccess();
    }
}

module.exports = MetaApi;

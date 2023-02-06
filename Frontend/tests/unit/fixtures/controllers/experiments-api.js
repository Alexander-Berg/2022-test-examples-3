'use strict';

const Controller = require('../../../../src/server/controller');
const experiment = require('../models/experiment');

class ExperimentsApi extends Controller {
    create() {
        return this.res.json({ id: 1 }).end();
    }

    createAndStartExperiment() {
        return this.res.json({ id: 1 }).end();
    }

    update() {
        return this.res.json({ id: 1 }).end();
    }

    clone() {
        return this.res.json({ id: 1 }).end();
    }

    createWorkflow() {
        return this.res.json({ id: '03da5423-4d16-11e7-89a6-0025909427cc' }).end();
    }

    startWorkflow() {
        return this.renderJsonSuccess();
    }

    setStatus() {
        return this.renderJsonSuccess();
    }

    setPools() {
        return this.renderJsonSuccess();
    }

    abExport() {
        this.res.setHeader('Content-Type', 'application/json; charset=utf-8');

        experiment
            .abExportPromise()
            .then((docs) => this.res.json(docs).end());

        return this.res;
    }

    currentWorkflowId() {
        return experiment.getCurrentWorkflowId(this.req.params.id)
            .then((results) => this.res.json(results).end());
    }

    callAnalyst() {
        return this.renderJsonSuccess();
    }

    setExpValidityStatus() {
        return this.renderJsonSuccess();
    }

    getExpValidityStatus() {
        return this.res.json({
            id: '',
            workflowId: '',
            expValidityStatus: 'unknown',
            ownerApprove: false,
        });
    }

    sendReactionToArgentum() {
        return this.renderJsonSuccess();
    }

    uploadLayouts() {
        return this.res.json({});
    }

    uploadLayoutsV2() {
        return this.res.json({});
    }
}

module.exports = ExperimentsApi;

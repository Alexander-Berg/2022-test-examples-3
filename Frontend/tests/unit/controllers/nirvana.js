'use strict';

const proxyquire = require('proxyquire');

const errors = require('../../../src/server/errors');

const config = require('config');
const experimentHelper = require('../../../src/server/helpers/experiments');
const meta = require('../fixtures/models/meta');
const experiment = require('../fixtures/models/experiment');
const ticket = require('../fixtures/models/ticket');
const express = require('../fixtures/express');
const flowActions = {
    stopWorkflow: () => Promise.resolve(),
    stopPreviousWorkflows: () => Promise.resolve(),
};
const resultsConverter = {
    convert() {
        return {};
    },
};
const NirvanaController = proxyquire.load('../../../src/server/controllers/nirvana', {
    '../models/experiment': experiment,
    '../models/meta': meta,
    '../models/ticket/ticket': ticket,
    '../models/ticket/ticket_serp': ticket,
    '../models/ticket/ticket_layout': ticket,
    '../helpers/experiments': experimentHelper,
    '../nirvana-input/serp/index': {
        main() {
            return {};
        },
    },
    '../results-converter': resultsConverter,
    '../helpers/workflow-actions': flowActions,
});

describe('controllers/nirvana', () => {
    let controller, req, res, sandbox;

    beforeEach(() => {
        req = {
            user: {
                login: 'brazhenko',
                token: '**************************************',
            },
            body: {},
            params: { id: 1 },
            query: {},
        };

        res = express.getRes();

        sandbox = sinon.createSandbox();
        controller = new NirvanaController(req, res);
    });

    afterEach(() => sandbox.restore());

    describe('setStatus', () => {
        it('если передан неизвестный этап, то контроллер должен вернуть ошибку', () => {
            req.body.stage = 'UNKNOWN STAGE';

            sandbox.spy(errors, 'getInvalidParamError');

            controller.setStatus().then(() =>
                assert.isTrue(errors.getInvalidParamError.calledOnce),
            );
        });

        it('если передан неизвестный статус на этапе, то контроллер должен вернуть ошибку', () => {
            req.body.status = 'unknown';

            sandbox.spy(errors, 'getInvalidParamError');

            controller.setStatus().then(() =>
                assert.isTrue(errors.getInvalidParamError.calledOnce),
            );
        });

        it('по окончанию загрузки результатов должен обновиться тикет', () => {
            req.body = {
                'workflow-id': 'new-workflow-1',
                stage: 'results',
                status: 'succeeded',
                progress: null,
                details: null,
            };

            sandbox.spy(controller, '_updateTicket');

            return controller.setStatus().then(() =>
                assert.isTrue(controller._updateTicket.called),
            );
        });

        it('должен правильно сохранять этап и статус', () => {
            req.params.id = 1;
            req.body = {
                'workflow-id': 'bccf77dc-3568-11e7-89a6-0025909427cc',
                'workflow-type': 'main',
                stage: 'pool-start',
                status: 'succeeded',
                progress: null,
                details: null,
                'error-type': null,
                'is-hidden': null,
                message: null,
            };

            sandbox.stub(experimentHelper, 'isStatusExistInList').callsFake(() => false);
            sandbox.spy(experiment, 'setStatus');

            return controller.setStatus().then(() =>
                assert.calledWith(experiment.setStatus,
                    req.params.id,
                    {
                        stage: req.body.stage,
                        status: req.body.status,
                        progress: req.body.progress,
                        details: req.body.details,
                        message: req.body.message,
                        workflowId: req.body['workflow-id'],
                        workflowType: req.body['workflow-type'],
                        errorType: req.body['error-type'],
                        isHidden: req.body['is-hidden'],
                    },
                ),
            );
        });

        it('если этап завершился успешно, то контроллер должен запостить комментарий в тикет', () => {
            req.params.id = 1;
            req.body = {
                'workflow-id': 'bccf77dc-3568-11e7-89a6-0025909427cc',
                'workflow-type': 'main',
                stage: 'pool-start',
                status: 'succeeded',
                progress: null,
                details: null,
            };

            const expUrl = `https://${config.hostname}/experiment/${req.params.id}`;

            sandbox.stub(experimentHelper, 'isStatusExistInList').callsFake(() => false);
            sandbox.spy(controller, '_postStatusCommentToTicket');

            return controller.setStatus().then(() =>
                assert.calledWith(controller._postStatusCommentToTicket,
                    req.params.id,
                    req.body.stage,
                    'serp',
                    {
                        pools: {
                            production: { 'pool-id': 456, 'project-id': 123 },
                            sandbox: { 'pool-id': 89, 'project-id': 576 },
                        },
                        workflowId: req.body['workflow-id'],
                        assessmentGroup: 'tolokers',
                        expUrl,
                        planReportUrl: 'https://sbs.s3.yandex.net/0681d29e3610579d7e3b823f198a4a6e6421cc58eda45500d63c3310aeae4ab3/sbs-101174-plan-report.html',
                    },
                ),
            );
        });

        it('если пришли этап и статус от неактуального графа, то контроллер должен вернуть ошибку', () => {
            req.params.id = 1;
            req.body = {
                'workflow-id': '36027914-4baa-11e7-89a6-0025909427cc',
                'workflow-type': 'main',
                stage: 'pool-start',
                status: 'succeeded',
                progress: null,
                details: null,
            };

            sandbox.spy(errors, 'getDeprecatedWorkflowError');

            return controller.setStatus().then(() =>
                assert.calledOnce(errors.getDeprecatedWorkflowError),
            );
        });

        it('если пришли этап и статус от неизвестного графа, то контроллер должен приклеить его к заявке и обновить тикет', () => {
            req.params.id = 1;
            req.body = {
                'workflow-id': '32cd3153-e092-478c-a399-e7ddeaa0a5d3',
                stage: 'pool-start',
                status: 'succeeded',
                progress: null,
                details: null,
            };

            sandbox.spy(experiment, 'setWorkflowId');
            sandbox.spy(controller, '_updateTicket');

            return controller.setStatus().then(() => {
                assert.calledWith(experiment.setWorkflowId, req.params.id, req.body['workflow-id']);
                assert.calledWith(controller._updateTicket, req.params.id);
            });
        });

        it('если пришли пулы от неизвестного графа, то контроллер должен приклеить его к заявке и обновить тикет', () => {
            req.params.id = 1;
            req.body = {
                'workflow-id': '32cd3153-e092-478c-a399-e7ddeaa0a5d3',
                production: {
                    'project-id': 123,
                    'pool-id': 456,
                },
                sandbox: {
                    'project-id': 123,
                    'pool-id': 456,
                },
            };

            sandbox.spy(experiment, 'setWorkflowId');
            sandbox.spy(controller, '_updateTicket');

            return controller.setPools().then(() => {
                assert.calledWith(experiment.setWorkflowId, req.params.id, req.body['workflow-id']);
                assert.calledWith(controller._updateTicket, req.params.id);
            });
        });

        it('после того, как началась разметка пула в Толоке, заявка должна быть залочена', () => {
            req.body = {
                'workflow-id': 'new-workflow-1',
                stage: 'pool-start',
                status: 'succeeded',
                progress: null,
                details: null,
            };

            sandbox.stub(experimentHelper, 'isStatusExistInList').callsFake(() => false);
            sandbox.spy(experiment, 'lock');

            return controller.setStatus().then(() =>
                assert.calledOnce(experiment.lock),
            );
        });

        it('если от графа пришел статус "failed", то заявка должна быть разлочена', () => {
            req.body = {
                'workflow-id': 'new-workflow-1',
                stage: 'pool-start',
                status: 'failed',
                progress: null,
                details: null,
            };

            sandbox.stub(experimentHelper, 'isStatusExistInList').callsFake(() => false);
            sandbox.spy(experiment, 'unlock');

            return controller.setStatus().then(() =>
                assert.calledWith(experiment.unlock, req.params.id),
            );
        });

        it('если от графа пришел статус "canceled", то заявка должна быть разлочена', () => {
            req.body = {
                'workflow-id': 'new-workflow-1',
                stage: 'pool-start',
                status: 'canceled',
                progress: null,
                details: null,
            };

            sandbox.stub(experimentHelper, 'isStatusExistInList').callsFake(() => false);
            sandbox.spy(experiment, 'unlock');

            return controller.setStatus().then(() =>
                assert.calledWith(experiment.unlock, req.params.id),
            );
        });
    });
});

'use strict';

require('should-http');

const proxyquire = require('proxyquire');

const httpStatuses = require('http-status');

const meta = require('../fixtures/models/meta');
const experiment = require('../fixtures/models/experiment');
const Argentum = require('../fixtures/argentum');
const ticket = require('../fixtures/models/ticket');
const express = require('../fixtures/express');

const flowHelpers = require('../../../src/server/helpers/workflows');
const mergeExperimentsUtils = require('../../../src/server/utils/merge-exp');
const argConverter = require('../../../src/server/data-adapters/argentum/experiment/index');

const resultsConverter = {
    convert() {
        return {};
    },
};
const experimentHelper = {
    isExperimentCreationLimitExceeded: () => false,
};

const ExperimentsApi = proxyquire.load('../../../src/server/controllers/experiments-api', {
    '../models/experiment': experiment,
    '../adapters/argentum/experiment': Argentum,
    '../adapters/argentum/results': Argentum,
    '../adapters/argentum/reactions': Argentum,
    '../models/meta': meta,
    '../models/ticket/ticket': ticket,
    '../models/ticket/ticket_serp': ticket,
    '../models/ticket/ticket_layout': ticket,
    '../nirvana-input/serp/index': {
        main() {
            return {};
        },
    },
    '../results-converter': resultsConverter,
    '../helpers/experiments': experimentHelper,
    '../helpers/workflows': flowHelpers,
    '../experiment-validators': () => ({ isValid: true }),
    '../helpers/move-queries-to-mds': { shouldQueriesBeMovedToMds: () => false },

});

describe('experiments-api', () => {
    let api, req, res, sandbox;

    beforeEach(() => {
        req = {
            user: {
                login: 'brazhenko',
                token: '**************************************',
                permissions: { 'search': {} },
            },
            body: {},
            params: { id: 1 },
            query: {},
            id: '12345',
        };

        res = express.getRes();

        sandbox = sinon.createSandbox();
        api = new ExperimentsApi(req, res);
    });

    afterEach(() => sandbox.restore());

    describe('???????????????????? ?????????????????? ???? ????????:', () => {
        const status = httpStatuses.SERVICE_UNAVAILABLE;
        const message = '???????????????????? ?? ???????????? ???????????? ???????????????? ??????????????????????????';

        function getMetaStub() {
            return Promise.resolve({
                workflowsLocked: true,
            });
        }

        it('?????????????????? ?? ?????????????????? ??????????', () => {
            sandbox.stub(meta, 'getMeta').callsFake(getMetaStub);

            return api.start().then(() => {
                assert.equal(res.getStatus(), status);
                assert.equal(res.getJsonBody().error.message, message);
            });
        });
    });

    describe('?????????????????? ????????????:', () => {
        function getMetaStub() {
            return Promise.resolve({
                workflowsLocked: true,
            });
        }

        it('?????? ?????????????????????????? ???????????? ???????????????????????? ???????????????? reqId', () => {
            sandbox.stub(meta, 'getMeta').callsFake(getMetaStub);

            return api.start().then(() => {
                assert.equal(res.getJsonBody().error.reqId, '12345');
            });
        });
    });

    describe('???????????? ?? ????????????:', () => {
        it('?????????? ???????????????? ??????????, ???? ???????????????????? ?? ????????????', () => {
            sandbox.spy(experiment, 'setWorkflowId');

            return api.start().then(() =>
                assert.isTrue(experiment.setWorkflowId.calledOnce),
            );
        });

        it('?????????? ???????????????? ?????????? ?????????????????? ?????????? ?? ??????????????', () => {
            sandbox.spy(api, '_updateTicket');

            return api.start().then(() =>
                assert.isTrue(api._updateTicket.calledOnce),
            );
        });

        it('?????????????? ?????????????? ???????? ???? ???????????? ???????????????????????????? ???????????? ?????????????? ????????????', () => {
            req.params.id = 42;

            const status = httpStatuses.NOT_FOUND;
            const message = `???????????? ?? ID ${req.params.id} ???? ????????????????????`;

            return api.start().then(() => {
                assert.equal(res.getStatus(), status);
                assert.equal(res.getJsonBody().error.message, message);
            });
        });

        it('?????????????? ?????????????????? ???????? ???? ???????????? ???????????????????????????? ???????????? ?????????????? ????????????', () => {
            req.params.id = 42;

            const status = httpStatuses.NOT_FOUND;
            const message = `???????????? ?? ID ${req.params.id} ???? ????????????????????`;

            return api.start().then(() => {
                assert.equal(res.getStatus(), status);
                assert.equal(res.getJsonBody().error.message, message);
            });
        });

        it('???????? ?? ???????????? ???????????? ???? ?????????????????????? ???? ???????????? ??????????, ???? ?????????? ?????? ?????????????????? ?????????????????????? ?????????? ???????????? ?????????????? ?????????????????????????????? ????????????', () => {
            req.params.id = 2;

            const notFoundStatus = httpStatuses.NOT_FOUND;
            const message = `?? ???????????? ?? ID ${req.params.id} ???? ???????????????? ???? ???????? ????????`;

            sandbox.stub(experiment, 'getById').callsFake(() => Promise.resolve({ workflows: [] }));

            return api.currentWorkflowId().then(() => {
                assert.equal(res.getStatus(), notFoundStatus);
                assert.equal(res.getJsonBody().error.message, message);
            });
        });

        it('???????? ???????????? ???? ????????????????????, ???? ?????????? ?????? ?????????????????? ?????????????????????? ?????????? ???????????? ?????????????? ?????????????????????????????? ????????????', () => {
            req.params.id = 2;

            const notFoundStatus = httpStatuses.NOT_FOUND;
            const message = `???????????? ?? ID ${req.params.id} ???? ????????????????????`;

            sandbox.stub(experiment, 'getById').callsFake(() => Promise.resolve(null));

            return api.currentWorkflowId().then(() => {
                assert.equal(res.getStatus(), notFoundStatus);
                assert.equal(res.getJsonBody().error.message, message);
            });
        });
    });

    describe('???????????? ?? ??????????????:', () => {
        it('?????? ???????????????????????? ???????????? ?????????? ?????????????? ?????????? ??????????????????, ????????????????, ?????????????? ????????????????????????, ?????????????????? ????????????????????, approveMode', () => {
            req.params.id = 1;
            req.body = {
                title: 'custom title',
                description: 'custom description',
                regular: true,
                overlap: { mode: 'edit', value: 40 },
                approveMode: 'manual',
            };

            const newTicketId = 42;

            function getMetaStub() {
                return Promise.resolve({
                    poolsList: [
                        {
                            poolId: 260707,
                            sandboxId: 23252,
                            title: 'sbs_ranking',
                            platform: 'desktop',
                            region: 'ru',
                            default: true,
                        },
                        {
                            poolId: 262882,
                            sandboxId: 23243,
                            title: 'design_with_regions',
                            platform: 'desktop',
                            region: 'all',
                            default: false,
                        },
                    ],
                    whitelists: {
                        autostart: ['brazhenko'],
                        approval: ['brazhenko'],
                    },
                });
            }

            sandbox.spy(api, '_createTicket');
            sandbox.spy(experiment, 'createExp');
            sandbox.stub(meta, 'getMeta').callsFake(getMetaStub);
            sandbox.stub(mergeExperimentsUtils, 'merge').callsFake((exp) => exp);
            sandbox.stub(argConverter.serp, 'fromArg').callsFake(() => {});

            return api.clone().then(() => {
                assert.calledWithExactly(api._createTicket, 'serp', req.body.title, 38940, req.user.login, []);
                assert.calledWithExactly(experiment.createExp, {
                    id: newTicketId,
                    login: req.user.login,
                    data: {
                        title: req.body.title,
                        overlap: { mode: 'edit', value: 40 },
                        description: req.body.description,
                        notificationMode: {
                            preset: 'workflowOnly',
                            workflowNotificationChannels: ['email'],
                        },
                        workflowType: 'stable',
                        runInYang: 'no',
                        poolTitle: 'sbs_ranking',
                        assessmentGroup: 'tolokers',
                        useAutoHoneypots: 'yes',
                        device: 'touch',
                        screenProfile: 'default',
                        assessmentDeviceType: 'touch',
                        tasksuiteComposition: {
                            mode: 'default',
                            val: {
                                goodTasksCount: null,
                                badTasksCount: null,
                                assignmentsAcceptedCount: null,
                            },
                        },
                        owners: [],
                        resultsPerPage: { mode: 'default' },
                        approveMode: 'manual',
                        honeypotsMode: 'auto',
                        checkEqualScreenshots: 'default',
                        equalScreenshotsMetricThreshold: 'medium',
                    },
                    regular: req.body.regular,
                    parentId: req.params.id,
                    uiVersion: void 0,
                    type: 'serp',
                    parentTemplateId: undefined,
                }, api.logger);
            });
        });

        // TODO: !! ???? ????????????
        it.skip('?????????????? ???????????????????????? ???????????? ?? ???????????????????????? ?????????????????? ?????????? ???????????? ?????????????????? ?? ????????????', () => {
            req.params.id = 1;
            req.body = {};

            const invalidPoolName = 'invalidPoolName';
            const status = httpStatuses.BAD_REQUEST;
            const message = `?????????????????? ?????????????????? ?????? ???? ???????????? (${invalidPoolName}). ???????????????? ?????????????????????? ???????????? ?????????????????????? ?? ?????????????????????? ????????????. ???????????????? ?????????????????????? ?????????? ??????????????????, ????????????????????, ?????? ???????????? ???????????? ?????????????????? ??????.`;

            sandbox.stub(api, '_getMetaAndExp').callsFake(() => Promise.resolve({
                meta: {
                    poolsList: [
                        {
                            poolId: 260707,
                            sandboxId: 23252,
                            title: 'sbs_ranking',
                            platform: 'desktop',
                            region: 'ru',
                            default: true,
                        },
                        {
                            poolId: 262882,
                            sandboxId: 23243,
                            title: 'design_with_regions',
                            platform: 'desktop',
                            region: 'all',
                            default: false,
                        },
                    ],
                },
                exp: {
                    type: 'serp',
                    params: {
                        poolTitle: invalidPoolName,
                    },
                },
            }));

            return api.clone().then(() => {
                assert.equal(res.getStatus(), status);
                assert.equal(res.getJsonBody().error.message, message);
            });
        });

        it('?????? ???????????????? ???????????? ?????????????????? ?????????????????? ?????????????????????? ???????????????? goodTasks/badTasks', () => {
            req.body = {
                experiment: {
                    title: 'custom title',
                    description: 'custom description',
                    regular: true,
                    poolTitle: 'touch_360_default',
                    layouts: {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb1.png' },
                                ],
                                honeypots: [
                                    { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb4.png' },
                                ],
                            },
                            {
                                screens: [
                                    { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb6.png' },
                                ],
                                honeypots: [
                                    { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb10.png' },
                                ],
                            },
                        ],
                        screens: [],
                    },
                },
                type: 'layout',
            };

            sandbox.spy(experiment, 'createExp');

            return api.create().then(() => {
                assert.equal(experiment.createExp.getCall(0).args[0].data.goodTasks, 5);
                assert.equal(experiment.createExp.getCall(0).args[0].data.badTasks, 1);
            });
        });

        it('?????? ???????????????? ???????????? ?????????????????? ?????????????????? ?????????????????????? ???????????????? goodTasks/badTasks ???????? ???????? ???????????? ???????? ???? ??????', () => {
            req.body = {
                experiment: {
                    title: 'custom title',
                    description: 'custom description',
                    regular: true,
                    goodTasks: 25,
                    poolTitle: 'touch_360_default',
                    layouts: {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb1.png' },
                                ],
                                honeypots: [
                                    { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb4.png' },
                                ],
                            },
                            {
                                screens: [
                                    { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb6.png' },
                                ],
                                honeypots: [
                                    { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb10.png' },
                                ],
                            },
                        ],
                        screens: [],
                    },
                },
                type: 'layout',
            };

            sandbox.spy(experiment, 'createExp');

            return api.create().then(() => {
                assert.equal(experiment.createExp.getCall(0).args[0].data.goodTasks, 25);
                assert.equal(experiment.createExp.getCall(0).args[0].data.badTasks, 1);
            });
        });

        it('?????? ???????????????? ???????????? ???? ???????????????????? ???????????????? goodTasks/badTasks ???????? ?????? ???????? ??????????????', () => {
            req.body = {
                experiment: {
                    title: 'custom title',
                    description: 'custom description',
                    regular: true,
                    badTasks: 250,
                    goodTasks: 25,
                    poolTitle: 'touch_360_default',
                    layouts: {
                        layouts: [
                            {
                                screens: [
                                    { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb1.png' },
                                ],
                                honeypots: [
                                    { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb4.png' },
                                ],
                            },
                            {
                                screens: [
                                    { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb6.png' },
                                ],
                                honeypots: [
                                    { origUrl: 'https://storage.mds.yandex.net/get-mturk/859398/a92fbe84-e7a4-44bb-be73-655689368cb10.png' },
                                ],
                            },
                        ],
                        screens: [],
                    },
                },
                type: 'layout',
            };

            sandbox.spy(experiment, 'createExp');

            return api.create().then(() => {
                assert.equal(experiment.createExp.getCall(0).args[0].data.goodTasks, 25);
                assert.equal(experiment.createExp.getCall(0).args[0].data.badTasks, 250);
            });
        });

        it('?????????? ?????????????? ???????????????????? ?????????? ???????????? ???????????? ???????? ????????????????', () => {
            sandbox.stub(api, '_getMetaAndExp').callsFake(() => Promise.resolve({
                meta: {
                    workflowsLocked: false,
                },
                exp: {
                    id: 123,
                    type: 'serp',
                    locked: false,
                    params: { title: 'exp-title', workflowType: 'stable' },
                    ticket: 'SIDEBYSIDE-123',
                },
            }));
            sandbox.stub(api, '_updateTicket').callsFake(() => Promise.resolve());
            sandbox.stub(flowHelpers, 'isWorkflowCreationAvailablePromise').callsFake(() => Promise.resolve());
            sandbox.stub(flowHelpers, 'isWorkflowStartAvailablePromise').callsFake(() => Promise.resolve());
            sandbox.stub(Argentum.prototype, 'startExperiment').callsFake(() => Promise.resolve({ 'workflow-id': 'wf-01' }));

            sandbox.spy(experiment, 'lock');

            return api.start().catch().then(() =>
                assert.calledWith(experiment.lock, req.params.id),
            );
        });

        it('???????? ?????????????????? ???????????? ?????? ???????????????? ??????????, ???? ???????????? ???????????? ???????? ??????????????????', () => {
            sandbox.stub(api, '_getMetaAndExp').callsFake(() => Promise.resolve({
                meta: {
                    workflowsLocked: false,
                },
                exp: {
                    id: 123,
                    type: 'serp',
                    locked: false,
                    params: { title: 'exp-title', workflowType: 'stable' },
                    ticket: 'SIDEBYSIDE-123',
                },
            }));
            sandbox.stub(flowHelpers, 'isWorkflowCreationAvailablePromise').callsFake(() => Promise.resolve());
            sandbox.stub(flowHelpers, 'isWorkflowStartAvailablePromise').callsFake(() => Promise.resolve());
            sandbox.stub(Argentum.prototype, 'startExperiment').callsFake(() => Promise.reject({ error: '' }));

            sandbox.spy(experiment, 'unlock');

            return api.start().catch().then(() =>
                assert.calledWith(experiment.unlock, req.params.id),
            );
        });

        it('???????????? ???????????????? ???????????????????? ????????????', () => {
            req.params.id = 1;
            req.body.params = {};

            function getByIdStub() {
                return Promise.resolve({
                    id: req.params.id,
                    locked: true,
                });
            }

            sandbox.stub(experiment, 'getById').callsFake(getByIdStub);

            const status = httpStatuses.LOCKED;
            const message = `???????????? ?? ID ${req.params.id} ??????????????????????????`;

            return api.update().then(() => {
                assert.equal(res.getStatus(), status);
                assert.equal(res.getJsonBody().error.message, message);
            });
        });

        it('???????????? ?????????????? ???????? ???? ???????????? ???????????????????? ????????????', () => {
            req.params.id = 1;

            function getByIdStub() {
                return Promise.resolve({
                    id: req.params.id,
                    locked: true,
                });
            }

            sandbox.stub(experiment, 'getById').callsFake(getByIdStub);

            const status = httpStatuses.LOCKED;
            const message = `???????????? ?? ID ${req.params.id} ??????????????????????????`;

            return api.start().then(() => {
                assert.equal(res.getStatus(), status);
                assert.equal(res.getJsonBody().error.message, message);
            });
        });

        it('?????????? ?????????????? ?????????????????? ?????????? ???????????? ???????????? ???????? ??????????????????', () => {
            sandbox.stub(api, '_getMetaAndExp').callsFake(() => Promise.resolve({ meta: 'meta', experiment: 'experiment' }));
            sandbox.stub(Argentum.prototype, 'stopExperiment').callsFake(() => Promise.resolve());

            sandbox.spy(experiment, 'unlock');

            return api.stop().then(() =>
                assert.calledWith(experiment.unlock, req.params.id),
            );
        });

        it('???????? ?????????????????? ???????????? ?????? ?????????????????? ??????????, ???? ???????????? ???????????? ???????? ????????????????', () => {
            sandbox.stub(api, '_getMetaAndExp').callsFake(() => Promise.resolve({ meta: 'meta', experiment: 'experiment' }));

            sandbox.stub(Argentum.prototype, 'stopExperiment').callsFake(() => Promise.reject({ err: 'error' }));
            sandbox.spy(experiment, 'unlock');

            return api.stop().finally(() =>
                assert.calledWith(experiment.unlock, req.params.id),
            );
        });
    });

    describe('?????????????? ???????????? ?? ??????', () => {
        it('???????????? ???????????????????? ????????????????????????, ??????????????????/???????????????????? ?? ?????????????????? ?????????????????? ?????? from-to', () => {
            req.query.from = '1455138000';
            req.query.to = '1486501200';

            const from = new Date('2016-02-10T21:00Z');
            const to = new Date('2017-02-07T21:00Z');

            sandbox.spy(experiment, 'abExport');

            api.abExport();
            return assert.calledWith(experiment.abExport, from, to);
        });
    });

    describe('sendReactionToArgentum', () => {
        it('???????? ???????????? ???????????????????? ???????????? ???? ?????????????????????????? ?????????? (?????????????????????? worker-id), ???? ???????????? ?????????????? ?????????????????????????????? ????????????', () => {
            req.params.id = 1;
            req.body = {
                'task-id': 'task-id',
                typeOfReaction: 'like',
                wasActive: true,
            };

            const message = '???????????????? ???????? ???????????????????? ?????? ???????????????? ?????????????? ???????????? ???????? ????????????????????';

            return api.sendReactionToArgentum().then(() => {
                assert.equal(res.getStatus(), httpStatuses.INTERNAL_SERVER_ERROR);
                assert.equal(res.getJsonBody().error.message, message);
            });
        });

        it('???????? ???????????? ???????????????????? ???????????? ???? ?????????????????????????? ?????????? (?????????????????????? worker-id), ???? ???? ?????????? ?????????????? ?????????????? ?????????? ???????????????? ?????? ???????????????? ?????? ???????????????????? ??????????????', () => {
            req.params.id = 1;
            req.body = {
                'task-id': 'task-id',
                typeOfReaction: 'like',
                wasActive: true,
            };

            sandbox.spy(Argentum.prototype, 'deleteReaction');
            sandbox.spy(Argentum.prototype, 'addReaction');

            return api.sendReactionToArgentum().then(() => {
                assert.isTrue(Argentum.prototype.deleteReaction.notCalled);
                assert.isTrue(Argentum.prototype.addReaction.notCalled);
            });
        });

        it('???????? ???????????? ???????????????????? ???????????? ???? ?????????????????????????? ?????????? (?????????????????????? task-id), ???? ???? ?????????? ?????????????? ?????????????? ?????????? ???????????????? ?????? ???????????????? ?????? ???????????????????? ??????????????', () => {
            req.params.id = 1;
            req.body = {
                'worker-id': 'worker-id',
                typeOfReaction: 'like',
                wasActive: false,
            };
            sandbox.spy(Argentum.prototype, 'deleteReaction');
            sandbox.spy(Argentum.prototype, 'addReaction');

            return api.sendReactionToArgentum().then(() => {
                assert.isTrue(Argentum.prototype.addReaction.notCalled);
                assert.isTrue(Argentum.prototype.deleteReaction.notCalled);
            });
        });

        it('???????? ???????????? ???????????????????? ???????????? ?????????????????????????? ?????????? ?? wasActive: true, ???? ?????????? ?????????????? ?????????? ???????????????? ?????? ???????????????? ??????????????', () => {
            req.params.id = 1;
            req.user.login = 'login';
            req.body = {
                'worker-id': 'worker-id',
                'task-id': 'task-id',
                typeOfReaction: 'like',
                wasActive: true,
            };
            sandbox.spy(Argentum.prototype, 'deleteReaction');

            return api.sendReactionToArgentum().then(() => {
                assert.calledWith(Argentum.prototype.deleteReaction,
                    req.params.id,
                    req.body['worker-id'],
                    req.body['task-id'],
                    req.user.login,
                    req.body.typeOfReaction,
                );
            });
        });

        it('???????? ???????????? ???????????????????? ???????????? ?????????????????????????? ?????????? ?? wasActive: false, ???? ?????????? ?????????????? ?????????? ???????????????? ?????? ???????????????????? ??????????????', () => {
            req.params.id = 1;
            req.user.login = 'login';
            req.body = {
                'worker-id': 'worker-id',
                'task-id': 'task-id',
                typeOfReaction: 'like',
                wasActive: false,
            };
            sandbox.spy(Argentum.prototype, 'addReaction');

            return api.sendReactionToArgentum().then(() => {
                assert.calledWith(Argentum.prototype.addReaction,
                    req.params.id,
                    req.body['worker-id'],
                    req.body['task-id'],
                    req.user.login,
                    req.body.typeOfReaction,
                );
            });
        });
    });
});

'use strict';

require('should-http');

const proxyquire = require('proxyquire');

const httpStatuses = require('http-status');

const config = require('config');
const experiment = require('../fixtures/models/experiment');
const Argentum = require('../fixtures/argentum');
const express = require('../fixtures/express');

const Results = proxyquire.load('../../../src/server/controllers/results', {
    '../models/experiment': experiment,
    '../adapters/argentum/results': Argentum,
});

describe('controllers/results', () => {
    let controller, req, res, sandbox;

    beforeEach(() => {
        req = {
            user: {
                login: 'brazhenko',
            },
            body: {},
            params: { id: 1 },
            query: {},
            id: '12345',
        };

        res = express.getRes();

        sandbox = sinon.createSandbox();
        controller = new Results(req, res);
    });

    afterEach(() => sandbox.restore());

    it('ручка получения настроек эксперимента должна отдавать корректный статус, если эксперимент не существует', function() {
        req.params.id = 12345678;

        const status = httpStatuses.NOT_FOUND;
        const message = `Заявка с ID ${req.params.id} не существует`;

        return controller.getExp().then(() => {
            assert.equal(res.getStatus(), status);
            assert.equal(res.getJsonBody().error.message, message);
        });
    });

    describe('getStatusOrResults', () => {
        it('должен возвращать этап/статус графа, если результаты не готовы', () => {
            req.params.id = 1;

            const stage = 'pool-ready';
            const status = 'succeeded';
            const expected = {
                stage: config.expStages[stage].title,
                status: config.expStageStatuses[status],
            };

            function getByIdStub() {
                return Promise.resolve({
                    id: req.params.id,
                    hasResults: false,
                    lastStatus: { stage, status },
                });
            }

            sandbox.stub(experiment, 'getById').callsFake(getByIdStub);

            return controller.getStatusOrResults().then(() =>
                assert.deepEqual(res.getJsonBody(), expected),
            );
        });

        // TODO SBSDEV-5756: Расскипать тест на метод getStatusOrResults
        it.skip('должен возврашать результаты, если готовы', () => {
            req.params.id = 1;

            const results = {
                systems: [],
                summary: [],
                pairs: [],
            };

            function getByIdStub() {
                return Promise.resolve({
                    id: req.params.id,
                    hasResults: true,
                });
            }

            sandbox.stub(experiment, 'getById').callsFake(getByIdStub);

            return controller.getStatusOrResults().then(() =>
                assert.deepEqual(res.getJsonBody(), results),
            );
        });
    });

    describe('getWinAgainstControlSystem', function() {
        it('если в параметрах присутствуют фильтр по тега, обращается к Argentum, передавая его в запросе', function() {
            sandbox.spy(Argentum.prototype, 'getWinsAgainstControlSystem');

            req.query.leftSystemId = '0';
            req.query.rightSystemId = '1';
            req.query.controlSystemId = '2';
            req.query.tagsFiltering = 'ewogICJleHQtb3AiOiAib3IiLAogICJmaWx0ZXJzIjogWwogICAgewogICAgICAic3lzdGVtIjog==';

            return controller.getWinAgainstControlSystem()
                .then(() => {
                    assert.isTrue(Argentum.prototype.getWinsAgainstControlSystem.calledOnce);
                    assert.calledWith(Argentum.prototype.getWinsAgainstControlSystem, 1, {
                        metrics: ['win-rate', 'bt'],
                        tagsFiltering: 'ewogICJleHQtb3AiOiAib3IiLAogICJmaWx0ZXJzIjogWwogICAgewogICAgICAic3lzdGVtIjog==',
                        leftSystemId: '0',
                        rightSystemId: '1',
                        controlSystemId: '2',
                    });
                });
        });

        it('преобразовывает ответ Аргентума в формат для верстки', function() {
            sandbox.stub(Argentum.prototype, 'getWinsAgainstControlSystem')
                .returns(Promise.resolve({
                    valid: true,
                    'control-system-id': '2',
                    'left-system-id': '0',
                    'right-system-id': '1',
                    results: {
                        bt: {
                            'left-system-score': 0.7584739089416946,
                            'p-value': 1.76854185732607e-10,
                            'query-count': 100,
                            'right-system-score': 0.8856497612369912,
                        },
                    },
                }));

            req.query.leftSystemId = '0';
            req.query.rightSystemId = '1';
            req.query.controlSystemId = '2';

            sandbox.spy(res, 'json');
            return controller.getWinAgainstControlSystem()
                .then(() => {
                    assert.isTrue(res.json.calledOnce);
                    assert.calledWith(res.json, {
                        valid: true,
                        systems: [],
                        controlSystemId: '2',
                        leftSystem: { id: '0', score: 0.7584739089416946 },
                        rightSystem: { id: '1', score: 0.8856497612369912 },
                        pValue: 1.76854185732607e-10,
                        queryCount: 100,
                    });
                });
        });

        it('если в ответе от Аргентума есть обе метрики, возвращает значения метрики Bradley-Terry', function() {
            sandbox.stub(Argentum.prototype, 'getWinsAgainstControlSystem')
                .returns(Promise.resolve({
                    valid: true,
                    'control-system-id': '2',
                    'left-system-id': '0',
                    'right-system-id': '1',
                    results: {
                        bt: {
                            'left-system-score': 0.7584739089416946,
                            'p-value': 1.76854185732607e-10,
                            'query-count': 100,
                            'right-system-score': 0.8856497612369912,
                        },
                        'win-rate': {
                            'left-system-score': 0.765,
                            'p-value': 2.054534504909998e-12,
                            'query-count': 100,
                            'right-system-score': 0.8985000000000005,
                        },
                    },
                }));

            req.query.leftSystemId = '0';
            req.query.rightSystemId = '1';
            req.query.controlSystemId = '2';

            sandbox.spy(res, 'json');
            return controller.getWinAgainstControlSystem()
                .then(() => {
                    assert.isTrue(res.json.calledOnce);
                    assert.calledWith(res.json, {
                        valid: true,
                        systems: [],
                        controlSystemId: '2',
                        leftSystem: { id: '0', score: 0.7584739089416946 },
                        rightSystem: { id: '1', score: 0.8856497612369912 },
                        pValue: 1.76854185732607e-10,
                        queryCount: 100,
                    });
                });
        });

        it('если в ответе от Аргентума нет метрик, отвечаем заглушкой без их значений', function() {
            sandbox.stub(Argentum.prototype, 'getWinsAgainstControlSystem')
                .returns(Promise.resolve({
                    valid: true,
                    'control-system-id': '2',
                    'left-system-id': '0',
                    'right-system-id': '1',
                    results: {},
                }));

            req.query.leftSystemId = '0';
            req.query.rightSystemId = '1';
            req.query.controlSystemId = '2';

            sandbox.spy(res, 'json');
            return controller.getWinAgainstControlSystem()
                .then(() => {
                    assert.isTrue(res.json.calledOnce);
                    assert.calledWith(res.json, {
                        valid: true,
                        systems: [],
                        controlSystemId: '2',
                        leftSystem: { id: '0', score: null },
                        rightSystem: { id: '1', score: null },
                        pValue: null,
                    });
                });
        });

        it('если Аргентум сообщил, что параметры невалидны, отвечаем заглушкой без значений метрик', function() {
            sandbox.stub(Argentum.prototype, 'getWinsAgainstControlSystem')
                .returns(Promise.resolve({ valid: false }));

            req.query.leftSystemId = '0';
            req.query.rightSystemId = '1';
            req.query.controlSystemId = '2';

            sandbox.spy(res, 'json');
            return controller.getWinAgainstControlSystem()
                .then(() => {
                    assert.isTrue(res.json.calledOnce);
                    assert.calledWith(res.json, {
                        valid: true,
                        systems: [],
                        controlSystemId: '2',
                        leftSystem: { id: '0', score: null },
                        rightSystem: { id: '1', score: null },
                        pValue: null,
                    });
                });
        });
    });
});

const proxyquire = require('proxyquire');
const Ajv = require('../fixtures/ajv');
const utils = require('../../../src/server/experiment-validators/utils');

const validateExperiment = proxyquire.load('../../../src/server/experiment-validators', {
    'ajv': Ajv,
});

describe('validateExperiment', function() {
    const sandbox = sinon.createSandbox();

    afterEach(function() {
        sandbox.restore();
        Ajv.resetBehavior();
    });

    it('должен вызывать utils.validateExperiment с верным набором параметров для макетного эксперимента', function() {
        const experiment = {
            type: 'layout',
            title: 'Эксперимент',
            description: 'Описание',
        };

        sandbox.stub(utils, 'validateExperiment');

        validateExperiment('layout', experiment);
        assert.calledOnce(utils.validateExperiment);
        assert.calledWith(utils.validateExperiment, experiment, Ajv.stubs.compileDefaultResult, sinon.match.array);
    });

    it('должен вызывать utils.validateExperiment с верным набором параметров для опроса', function() {
        const experiment = {
            type: 'poll',
            title: 'Эксперимент',
            description: 'Описание',
        };

        sandbox.stub(utils, 'validateExperiment');

        validateExperiment('poll', experiment);
        assert.calledOnce(utils.validateExperiment);
        assert.calledWith(utils.validateExperiment, experiment, Ajv.stubs.compileDefaultResult, sinon.match.array);
    });

    it('должен вызывать utils.validateExperiment с верным набором параметров для сценарного эксперимента', function() {
        const experiment = {
            type: 'scenario',
            title: 'Эксперимент',
            description: 'Описание',
        };

        sandbox.stub(utils, 'validateExperiment');

        validateExperiment('scenario', experiment);
        assert.calledOnce(utils.validateExperiment);
        assert.calledWith(utils.validateExperiment, experiment, Ajv.stubs.compileDefaultResult, sinon.match.array);
    });

    it('должен вызывать utils.validateExperiment с верным набором параметров для поискового эксперимента', function() {
        const experiment = {
            type: 'serp',
            title: 'Эксперимент',
            description: 'Описание',
        };

        sandbox.stub(utils, 'validateExperiment');

        validateExperiment('serp', experiment);
        assert.calledOnce(utils.validateExperiment);
        assert.calledWith(utils.validateExperiment, experiment, Ajv.stubs.compileDefaultResult, sinon.match.array);
    });

    it('должен вернуть ошибку валидации если не передан тип эксперимента', function() {
        const { isValid } = validateExperiment(undefined);
        assert.isFalse(isValid);
    });
});

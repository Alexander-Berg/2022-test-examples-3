const proxyquire = require('proxyquire');
const Ajv = require('../fixtures/ajv');

const utils = proxyquire.load('../../../src/server/experiment-validators/utils.js', {
    'ajv': Ajv,
});

describe('utils.validateExperiment', function() {
    const sandbox = sinon.createSandbox();

    afterEach(function() {
        sandbox.restore();
        Ajv.resetBehavior();
    });

    it('должен вызывать все переданные функции', function() {
        const experiment = {
            type: 'layout',
            title: 'Эксперимент',
            description: 'Описание',
        };

        const ajvValidateFunctionStub = sandbox.spy(() => ({ isValid: true }));
        const validateFieldsFunctions = [1, 2, 3].map(() => sandbox.spy(() => ({ isValid: true })));

        utils.validateExperiment(experiment, ajvValidateFunctionStub, validateFieldsFunctions);
        assert.calledOnce(ajvValidateFunctionStub);
        assert.calledWith(ajvValidateFunctionStub, experiment);

        validateFieldsFunctions.forEach((fn) => {
            assert.calledOnce(fn);
            assert.calledWith(fn, experiment);
        });
    });
});

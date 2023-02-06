const _ = require('lodash');

const converter = require('../../../../../src/server/data-adapters/nirvana/poll');
const { CONFIG_INPUT, CONFIG_OUTPUT } = require('./fixtures');

describe('nirvana/poll', () => {
    let converterInput;

    beforeEach(function() {
        converterInput = _.cloneDeep(CONFIG_INPUT);
    });

    it('должен правильно конвертировать настройки эксепримента в конфиг для Нирваны', () => {
        assert.deepEqual(converter(converterInput), CONFIG_OUTPUT);
    });

    it('должен выставлять ID продакшн-графа Опросов если задан', () => {
        converterInput.pollProdWorkflowId = '907d74aa-9512-4a58-8270-dc085b8fc76e';
        assert.equal(converter(converterInput)['poll-params']['prod-workflow-id'], converterInput.pollProdWorkflowId);
    });
});

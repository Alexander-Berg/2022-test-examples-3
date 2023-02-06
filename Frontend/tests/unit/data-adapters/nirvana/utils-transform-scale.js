const utils = require('../../../../src/server/data-adapters/nirvana/utils');

const fixture = {
    type: 'scale',
    data: {
        required: true,
        question: 'Scale',
        leftText: 'Left',
        rightText: 'Right',
        startsWith: -2,
        endsWith: 2,
    },
    key: 'system-scale-key-4',
};

describe('nirvana/utils/transformScaleQuestion', function() {
    let question;

    beforeEach(() => {
        question = { ...fixture };
    });

    it('корректно конвертирует startsWith в scaleStart', function() {
        const result = utils.transformScaleQuestion(question);

        assert.strictEqual(result.data.scaleStart, fixture.data.startsWith);
    });

    it('корректно конвертирует endsWith в scaleEnd', function() {
        const result = utils.transformScaleQuestion(question);

        assert.strictEqual(result.data.scaleEnd, fixture.data.endsWith);
    });

    it('сохраняет корректный набор полей', function() {
        const result = utils.transformScaleQuestion(question);

        assert.hasAllKeys(result, ['type', 'data', 'key']);
        assert.hasAllKeys(result.data, ['required', 'question', 'leftText', 'rightText', 'scaleStart', 'scaleEnd']);
    });
});

const _ = require('lodash');

const calc = require('../../../../../src/client/components/utils/exp-calc');
const fixtures = require('./fixtures');

describe('exp-calc', () => {
    fixtures.forEach((f) => {
        describe(f.description, () => {
            it('должно правильно считаться количество сравнений', () => {
                const result = calc.getCompCount(f.fixture, f.queriesCount);

                assert.strictEqual(result, f.comparisions);
            });

            it('должно правильно считаться количество наборов заданий, если значение overlap отсутствует', () => {
                const result = calc.getTaskSuitesCount(f.fixture, f.queriesCount);

                assert.strictEqual(result, f.tasks);
            });

            it('должно правильно считаться количество наборов заданий, если значение overlap задано вручную', () => {
                const i = _.cloneDeep(f.fixture);
                i['toloka'] = {
                    'overlap': 20,
                };
                const result = calc.getTaskSuitesCount(i, f.queriesCount);

                assert.strictEqual(result, f.tasks);
            });

            it('должна правильно считаться стоимость', () => {
                const result = calc.getPrice(f.fixture, f.queriesCount);

                assert.strictEqual(result, f.price);
            });
        });
    });

    describe('getTaskSuitesCount', () => {
        it('должен возвращать null, если не передано корректное кол-во хороших заданий', () => {
            const queriesCount = 2000;

            assert.strictEqual(calc.getTaskSuitesCount({ exp: { 'normal-tasks': null } }, queriesCount), null);
            assert.strictEqual(calc.getTaskSuitesCount({ exp: { 'normal-tasks': 0 } }, queriesCount), null);
            assert.strictEqual(calc.getTaskSuitesCount({ exp: { 'normal-tasks': undefined } }, queriesCount), null);
        });
    });
});

const logs = require('./__stubs__/logs');
const getSignals = require('../unistat/lib/signals');

describe('response-formatter', () => {
    test('Соответствует snapshot', () => {
        expect(getSignals(logs)).toMatchSnapshot();
    });
});

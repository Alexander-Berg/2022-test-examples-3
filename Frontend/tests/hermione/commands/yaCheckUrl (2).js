const { parse } = require('url');

const getErrorMessage = part => `Wrong ${part}`;

module.exports = function(actualUrl, expectedUrl, params = {}, query = []) {
    const actual = parse(actualUrl, true, true);
    const expected = parse(expectedUrl, true, true);

    const makeAssertViewSuite = part => {
        assert.strictEqual(actual[part], expected[part], getErrorMessage(part));
    };

    if (!params.skipProtocol) makeAssertViewSuite('protocol');
    if (!params.skipHostname) makeAssertViewSuite('hostname');
    if (!params.skipPathname) makeAssertViewSuite('pathname');
    if (!params.skipHash) makeAssertViewSuite('hash');

    if (!params.skipQuery && !query.length) {
        assert.deepEqual(actual.query, expected.query, getErrorMessage('query'));
    }

    if (query.length) {
        query.forEach(current => {
            assert.strictEqual(
                actual.query[current],
                expected.query[current],
                getErrorMessage('query')
            );
        });
    }
};

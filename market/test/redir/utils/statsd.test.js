let { increment } = require('./statsd');

increment = jest.fn().mockName('statsd increment');

beforeEach(() => {
    jest.clearAllMocks();
});

describe('statsd', () => {
    test('increment method works as intended', () => {
        increment({ host: 'fake_host', prefix: 1 });

        expect(increment).toHaveBeenCalled();
        expect(increment).toHaveBeenCalledTimes(1);
        expect(increment.mock.calls[0]).toMatchInlineSnapshot(`
            Array [
              Object {
                "host": "fake_host",
                "prefix": 1,
              },
            ]
        `);
    });
});
